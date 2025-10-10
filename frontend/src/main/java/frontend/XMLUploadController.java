package frontend;

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

@RestController
public class XMLUploadController {

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadXML(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "File is empty"));
        }
        
        // Handle the XML file upload
        try {
            // Save the file to a temporary location
            File tempFile = File.createTempFile("upload-", ".xml");
            file.transferTo(tempFile);
            
            // Parse nodes and edges from the XML file
            Map<Long, Node> nodes = XMLParser.parseNodes(tempFile.getAbsolutePath());
            List<Edge> edges = XMLParser.parseEdges(tempFile.getAbsolutePath());
            
            // Delete the temporary file
            tempFile.delete();

            System.out.println("Parsed " + nodes.size() + " nodes and " + edges.size() + " edges.");

            // Return a JSON object with nodes and edges. Convert nodes map to its values
            // (list of Node objects)
            Map<String, Object> responseBody = Map.of(
                "nodes", nodes.values(),
                "edges", edges
            );
            return ResponseEntity.ok().body(responseBody);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error processing file"));
        }
    }
}
