package backend;

import backend.models.Node;
import backend.models.Triple;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class DeliveryRequestParser {

    /**
     * Parse un fichier de livraisons XML et retourne :
     * Map<nodeId, Triple<Node, deliveryId, duration>>
     * deliveryId = -1 pour entrepôt, identifiant unique pour chaque livraison.
     */

    public static Map<Long, Triple<Node, Long, Integer>> parseDeliveries(String filename, Map<Long, Node> graphNodes) throws Exception {
        Map<Long, Triple<Node, Long, Integer>> sommets = new HashMap<>();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(filename));
        doc.getDocumentElement().normalize();

        Element entrepotElement = (Element) doc.getElementsByTagName("entrepot").item(0);
        long entrepotId = Long.parseLong(entrepotElement.getAttribute("adresse"));
        Node entrepotNode = graphNodes.get(entrepotId);
        if (entrepotNode != null) {
            sommets.put(entrepotId, new Triple<>(entrepotNode, -1L, 0));
        }

        NodeList livraisonList = doc.getElementsByTagName("livraison");
        long deliveryCounter = 1;

        for (int i = 0; i < livraisonList.getLength(); i++) {
            Element e = (Element) livraisonList.item(i);

            long idPickup = Long.parseLong(e.getAttribute("adresseEnlevement"));
            long idDelivery = Long.parseLong(e.getAttribute("adresseLivraison"));
            int dureePickup = Integer.parseInt(e.getAttribute("dureeEnlevement"));
            int dureeDelivery = Integer.parseInt(e.getAttribute("dureeLivraison"));

            Node pickupNode = graphNodes.get(idPickup);
            Node deliveryNode = graphNodes.get(idDelivery);

            if (pickupNode != null) {
                sommets.put(idPickup, new Triple<>(pickupNode, deliveryCounter, dureePickup));
            }
            if (deliveryNode != null) {
                sommets.put(idDelivery, new Triple<>(deliveryNode, deliveryCounter, dureeDelivery));
            }

            deliveryCounter++;
        }

        return sommets;
    }
}
