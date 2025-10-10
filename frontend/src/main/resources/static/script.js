// Coordonnées GPS du centre de la carte (ici Lyon) à l'initialisation
let lat = 45.764;
let lon = 4.8357;

// On initialise la carte (en lui passant 'map' qui est l'ID de la DIV contenant la carte)
let map = L.map("map", {
    zoom: 13,
    center: [lat, lon]
});

// On ajoute le calque permettant d'afficher les images de la carte
L.tileLayer("https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png", {
    minZoom: 1,
    maxZoom: 20,
    attribution: 'données © <a href="//osm.org/copyright">OpenStreetMap</a>/ODbL - rendu <a href="//openstreetmap.fr">OSM France</a>'
}).addTo(map);

var newIcon = L.icon({
    iconUrl: 'circle-icon.png',
    iconSize: [15, 15], // size of the icon
    iconAnchor: [7, 7], // point of the icon which will correspond to marker's location
});

var warehouseIcon = L.icon({
    iconUrl: 'warehouse-icon.png', 
    iconSize: [20, 20],
    iconAnchor: [10, 10],
});

var nodeMarkers = [];
var edgeLines = [];
var nodeMap = new Map(); // Graphe déjà chargé


// Charger la map en fonction du fichier XML choisi
function load_xml_map() {
    console.log("Loading XML map...");
    
    let input = document.createElement('input');
    input.type = 'file';
    input.accept = '.xml';

    input.onchange = e => {
        let file = e.target.files[0];
        let formData = new FormData();
        formData.append('file', file);

        fetch('/upload', { method: 'POST', body: formData })
        .then(response => {
            if (!response.ok) throw new Error("HTTP error " + response.status);
            return response.json();
        })
        .then(data => {
            var nodes = data.nodes;
            var edges = data.edges;

            console.log('Nodes:', nodes);
            console.log('Edges:', edges);

            // Supprimer anciens markers et edges
            nodeMarkers.forEach(m => map.removeLayer(m));
            nodeMarkers = [];
            edgeLines.forEach(l => map.removeLayer(l));
            edgeLines = [];

            // Réinitialiser le graphe global
            nodeMap.clear();
            nodes.forEach(node => nodeMap.set(node.id, node));

            // Variables pour calculer les bounds
            let topLeftNode = null;
            let bottomRightNode = null;

            // Ajouter les markers
            nodes.forEach(node => {

                if (!topLeftNode || (node.latitude > topLeftNode.latitude && node.longitude < topLeftNode.longitude)) {
                    topLeftNode = node;
                }
                if (!bottomRightNode || (node.latitude < bottomRightNode.latitude && node.longitude > bottomRightNode.longitude)) {
                    bottomRightNode = node;
                }
            });

            // Ajouter les edges
            edges.forEach(edge => {
                let startNode = nodeMap.get(edge.originId);
                let endNode = nodeMap.get(edge.destinationId);
                if (startNode && endNode) {
                    let latlngs = [
                        [startNode.latitude, startNode.longitude],
                        [endNode.latitude, endNode.longitude]
                    ];
                    edgeLines.push(L.polyline(latlngs, { color: '#0b3213' }).addTo(map));
                }
            });

            // Ajuster le zoom pour englober tous les nodes
            if (topLeftNode && bottomRightNode) {
                let bounds = L.latLngBounds(
                    [bottomRightNode.latitude, topLeftNode.longitude],
                    [topLeftNode.latitude, bottomRightNode.longitude]
                );
                map.flyToBounds(bounds, { duration: 2.0 });
            }
        })
        .catch(error => console.error("Error loading XML map:", error));
    };

    input.click();
}

function load_xml_delivery() {
    if (!nodeMap || nodeMap.size === 0) {
        alert("Veuillez d'abord importer un plan avant de charger une demande de livraison.");
        return;
    }

    let input = document.createElement('input');
    input.type = 'file';
    input.accept = '.xml';

    input.onchange = e => {
        let deliveryFile = e.target.files[0];
        let formData = new FormData();
        formData.append('file', deliveryFile);

        // On envoie aussi l'info du graphe déjà chargé côté serveur si besoin
        // Ici, on pourrait l'envoyer sous forme de JSON ou juste récupérer côté serveur
        // Si ton backend prend en param graphFile, il faut l'adapter pour accepter le graphe déjà chargé côté serveur

        fetch('/uploadDeliveries', {
            method: 'POST',
            body: formData
        })
        .then(response => {
            if (!response.ok) throw new Error("HTTP error " + response.status);
            return response.json();
        })
        .then(data => {
            console.log("Pickup/Delivery Nodes:", data.nodes);
            data.nodes.forEach(element => {
            let iconToUse = element.deliveryId === -1 ? warehouseIcon : newIcon;

            nodeMarkers.push(
                L.marker([element.latitude, element.longitude], { icon: iconToUse }).addTo(map)
            );
        })
        })
        .catch(err => console.error(err));
    }

    input.click();
}
