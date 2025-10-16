package frontend;

import frontend.models.Triple;
import frontend.models.Node;
import frontend.models.Edge;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Component;

// Singleton pour stocker le graphe en mémoire
@Component
class GraphStorage {
    private Map<Long, Node> graphNodes;

    public void setGraphNodes(Map<Long, Node> nodes) {
        this.graphNodes = nodes;
    }

    public Map<Long, Node> getGraphNodes() {
        return this.graphNodes;
    }
}

@RestController
public class XMLUploadController {

    private final GraphStorage graphStorage;

    // Injection du GraphStorage
    public XMLUploadController(GraphStorage graphStorage) {
        this.graphStorage = graphStorage;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadXML(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "File is empty"));
        }
        
        try {
            File tempFile = File.createTempFile("upload-", ".xml");
            file.transferTo(tempFile);
            
            Map<Long, Node> nodes = XMLParser.parseNodes(tempFile.getAbsolutePath());
            List<Edge> edges = XMLParser.parseEdges(tempFile.getAbsolutePath());

            // Stockage du graphe pour le futur /uploadDeliveries
            graphStorage.setGraphNodes(nodes);

            tempFile.delete();

            Map<String, Object> responseBody = Map.of(
                "nodes", nodes.values(),
                "edges", edges
            );
            return ResponseEntity.ok().body(responseBody);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Error processing file"));
        }
    }

    @PostMapping("/uploadDeliveries")
    public ResponseEntity<Map<String, Object>> uploadDeliveriesXML(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "File missing"));
        }

        try {
            File tempDeliveryFile = File.createTempFile("delivery-", ".xml");
            file.transferTo(tempDeliveryFile);

            // Récupère le graphe déjà chargé
            Map<Long, Node> graphNodes = graphStorage.getGraphNodes();
            if (graphNodes == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No graph loaded. Please upload a map first."));
            }

            Map<Long, Triple<Node, Long, Integer>> deliveryNodes =
                    DeliveryRequestParser.parseDeliveries(tempDeliveryFile.getAbsolutePath(), graphNodes);

            // On garde une trace des pickup déjà vus
            Map<Long, Boolean> seenPickups = new HashMap<>();

            List<Map<String, Object>> nodesList = deliveryNodes.values().stream()
                .map(triple -> {
                    Map<String, Object> map = new HashMap<>();
                    Node node = triple.first;
                    Long deliveryId = triple.second;
                    int duration = triple.third;

                    map.put("id", node.getId());
                    map.put("latitude", node.getLatitude());
                    map.put("longitude", node.getLongitude());
                    map.put("deliveryId", deliveryId);
                    map.put("duration", duration);

                    // Déterminer le type : "pickup" ou "delivery"
                    if (deliveryId == -1) {
                        map.put("type", "warehouse");
                    } else if (!seenPickups.containsKey(deliveryId)) {
                        seenPickups.put(deliveryId, true);
                        map.put("type", "pickup");
                    } else {
                        map.put("type", "delivery");
                    }

                    return map;
                })
                .collect(Collectors.toList());

            tempDeliveryFile.delete();

            return ResponseEntity.ok(Map.of("nodes", nodesList));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Error processing file"));
        }
    }
}
