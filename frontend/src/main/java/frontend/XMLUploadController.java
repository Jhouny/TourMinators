package frontend;

import frontend.models.PointOfInterest;
import frontend.models.Node;
import frontend.models.Edge;

import java.io.File;
import java.util.List;
import java.util.Map;

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

    public XMLUploadController(GraphStorage graphStorage) {
        this.graphStorage = graphStorage;
    }

    // --- 1️⃣ Upload de la carte ---
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

            graphStorage.setGraphNodes(nodes);

            tempFile.delete();

            Map<String, Object> responseBody = Map.of(
                    "nodes", nodes.values(),
                    "edges", edges);
            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error processing file"));
        }
    }

    // --- 2️⃣ Upload des livraisons ---
    @PostMapping("/uploadDeliveries")
    public ResponseEntity<Map<String, Object>> uploadDeliveriesXML(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "File missing"));
        }

        try {
            File tempDeliveryFile = File.createTempFile("delivery-", ".xml");
            file.transferTo(tempDeliveryFile);

            Map<Long, Node> graphNodes = graphStorage.getGraphNodes();
            if (graphNodes == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No graph loaded. Please upload a map first."));
            }

            List<PointOfInterest> poiList =
                    DeliveryRequestParser.parseDeliveries(tempDeliveryFile.getAbsolutePath(), graphNodes);


            tempDeliveryFile.delete();

            return ResponseEntity.ok(Map.of("tours", poiList));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error processing file"));
        }
    }
}
