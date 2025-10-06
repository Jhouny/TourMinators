package backend; 
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;
import backend.models.Node;
import backend.models.Edge;


public class XMLParser {

    public static Map<Long, Node> parseNodes(String filename) throws Exception {
        Map<Long, Node> nodes = new HashMap<>();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(filename));
        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("noeud");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element e = (Element) nodeList.item(i);
            long id = Long.parseLong(e.getAttribute("id"));
            double lat = Double.parseDouble(e.getAttribute("latitude"));
            double lon = Double.parseDouble(e.getAttribute("longitude"));
            nodes.put(id, new Node(id, lat, lon));
        }
        return nodes;
    }

    public static List<Edge> parseEdges(String filename) throws Exception {
        List<Edge> edges = new ArrayList<>();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(filename));
        doc.getDocumentElement().normalize();

        NodeList edgeList = doc.getElementsByTagName("troncon");
        for (int i = 0; i < edgeList.getLength(); i++) {
            Element e = (Element) edgeList.item(i);
            long origin = Long.parseLong(e.getAttribute("origine"));
            long dest = Long.parseLong(e.getAttribute("destination"));
            String name = e.getAttribute("nomRue");
            edges.add(new Edge(origin, dest, name));
        }
        return edges;
    }
}
