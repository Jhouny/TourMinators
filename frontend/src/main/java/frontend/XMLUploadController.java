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
    public ResponseEntity<String> uploadXML(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty");
        }
        
        // Handle the XML file upload
        try {
            // Save the file to a temporary location
            File tempFile = File.createTempFile("upload-", ".xml");
            file.transferTo(tempFile);
            
            // Parse nodes and edges from the XML file
            Map<Long, Node> nodes = XMLParser.parseNodes(tempFile.getAbsolutePath());
            List<Edge> edges = XMLParser.parseEdges(tempFile.getAbsolutePath());
            
            // For demonstration, print the parsed data
            System.out.println("Parsed Nodes: " + nodes);
            System.out.println("Parsed Edges: " + edges);
            
            // Delete the temporary file
            tempFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file");
        }

        return ResponseEntity.ok("File uploaded successfully");
    }
}
