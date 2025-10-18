package frontend;

import frontend.models.Node;
import frontend.models.PointOfInterest;
import frontend.models.PointOfInterest.PoIEnum;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

public class DeliveryRequestParser {

    public static List<PointOfInterest> parseDeliveries(String filename, Map<Long, Node> graphNodes) throws Exception {
        List<PointOfInterest> pois = new LinkedList<>();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(filename));
        doc.getDocumentElement().normalize();

        // --- Entrepôt ---
        Element entrepotElement = (Element) doc.getElementsByTagName("entrepot").item(0);
        long entrepotId = Long.parseLong(entrepotElement.getAttribute("adresse"));
        Node entrepotNode = graphNodes.get(entrepotId);
        if (entrepotNode != null) {
            // on marque le type dans un champ annexe
            entrepotNode.setType("warehouse");
            pois.add(new PointOfInterest(entrepotNode, PoIEnum.WAREHOUSE, null, 0));
        } else {
            throw new Exception("Entrepot node not found in graph nodes");
        }

        // --- Livraisons ---
        NodeList livraisonList = doc.getElementsByTagName("livraison");

        for (int i = 0; i < livraisonList.getLength(); i++) {
            Element e = (Element) livraisonList.item(i);

            long idPickup = Long.parseLong(e.getAttribute("adresseEnlevement"));
            long idDelivery = Long.parseLong(e.getAttribute("adresseLivraison"));
            int dureePickup = Integer.parseInt(e.getAttribute("dureeEnlevement"));
            int dureeDelivery = Integer.parseInt(e.getAttribute("dureeLivraison"));

            Node pickupNode = graphNodes.get(idPickup);
            Node deliveryNode = graphNodes.get(idDelivery);

            PointOfInterest pickupPOI = null;
            PointOfInterest deliveryPOI = null;

            // Marquer les types pour chaque noeud
            if (pickupNode != null) {
                pickupNode.setType("pickup");
                pickupPOI = new PointOfInterest(pickupNode, PoIEnum.PICKUP, idDelivery, dureePickup);
                pois.add(pickupPOI);
            } else {
                throw new Exception("Pickup node not found in graph nodes: " + idPickup);
            }
            
            if (deliveryNode != null) {
                deliveryNode.setType("delivery");
                deliveryPOI = new PointOfInterest(deliveryNode, PoIEnum.DELIVERY, idPickup, dureeDelivery);
                pois.add(deliveryPOI);
            } else {
                throw new Exception("Delivery node not found in graph nodes: " + idDelivery);
            }

        }

        return pois;
    }
}
