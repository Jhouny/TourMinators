package frontend;

import frontend.models.Node;
import frontend.models.PointOfInterest;
import frontend.models.PointOfInterest.PoIEnum;
import frontend.models.Triple;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

public class DeliveryRequestParser {

    /**
     * Parse un fichier de livraisons XML et retourne :
     * Map<nodeId, Triple<Node, deliveryId, duration>>
     * deliveryId = -1 pour entrepôt, identifiant unique pour chaque livraison.
     * 
     * Chaque Node se voit aussi attribuer un attribut "type" dans ses métadonnées :
     * - "warehouse" pour l'entrepôt
     * - "pickup" pour l’adresse d’enlèvement
     * - "delivery" pour l’adresse de livraison
     */
    public static Map<Long, PointOfInterest> mapDeliveries(Map<Long, Triple<Node, Long, Integer>> deliveries)
            throws Exception {

        Map<Long, PointOfInterest> poiMap = new HashMap<Long, PointOfInterest>();

        // On garde une trace des pickup déjà vus
        Map<Long, Long> seenPickups = new HashMap<Long, Long>(); // deliveryCounter -> pickupId

        for (Map.Entry<Long, Triple<Node, Long, Integer>> entry : deliveries.entrySet()) {
            Long nodeId = entry.getKey();
            Triple<Node, Long, Integer> triple = entry.getValue();
            Node node = triple.first;
            Long deliveryCounter = triple.second;
            Integer duration = triple.third;
            PoIEnum type;
            Long associatedPickupId = null;

            if (deliveryCounter == -1) {
                type = PoIEnum.WAREHOUSE;
            } else if (!seenPickups.containsKey(deliveryCounter)) {
                type = PoIEnum.PICKUP;
                seenPickups.put(deliveryCounter, nodeId);

            } else {
                type = PoIEnum.DELIVERY;
                associatedPickupId = seenPickups.get(deliveryCounter);

                poiMap.get(associatedPickupId).setAssociatedPickupId(nodeId);
            }

            poiMap.put(nodeId, new PointOfInterest(node, type, associatedPickupId, duration));

        }
        return poiMap;
    }

    public static Map<Long, Triple<Node, Long, Integer>> parseDeliveries(String filename, Map<Long, Node> graphNodes)
            throws Exception {
        Map<Long, Triple<Node, Long, Integer>> sommets = new LinkedHashMap<>();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(filename));
        doc.getDocumentElement().normalize();

        // --- Entrepôt ---
        Element entrepotElement = (Element) doc.getElementsByTagName("entrepot").item(0);
        long entrepotId = Long.parseLong(entrepotElement.getAttribute("adresse"));
        Node entrepotNode = graphNodes.get(entrepotId);
        if (entrepotNode != null) {
            // on marque le type dans un champ annexe
            entrepotNode.setType("warehouse");
            sommets.put(entrepotId, new Triple<>(entrepotNode, -1L, 0));
        }

        // --- Livraisons ---
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

            // Marquer les types pour chaque noeud
            if (pickupNode != null) {
                pickupNode.setType("pickup");
                sommets.put(idPickup, new Triple<>(pickupNode, deliveryCounter, dureePickup));
            }
            if (deliveryNode != null) {
                deliveryNode.setType("delivery");
                sommets.put(idDelivery, new Triple<>(deliveryNode, deliveryCounter, dureeDelivery));
            }

            deliveryCounter++;
        }

        return sommets;
    }
}
