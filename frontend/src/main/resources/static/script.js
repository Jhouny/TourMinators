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

var nodeMarkers = [];
var edgeLines = [];

// Charger la map en fonction du fichier XML choisi
function load_xml_map() {
    // Créer un input de type file pour choisir le fichier XML
    console.log("Loading XML map...");
    let input = document.createElement('input');
    input.type = 'file';
    input.accept = '.xml';

    // Quand un fichier est choisi, on le lit et on l'envoie au backend
    input.onchange = e => {
        let file = e.target.files[0];
        let formData = new FormData();
        formData.append('file', file);

        fetch('/upload', {
            method: 'POST',
            body: formData
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.text();
        })
        .then(data => {
            var nodes = JSON.parse(data).nodes;
            var edges = JSON.parse(data).edges;

            console.log('Nodes:', nodes);
            console.log('Edges:', edges);

            var topLeftNode, bottomRightNode;

            nodeMarkers.forEach(marker => {
                map.removeLayer(marker);
            });
            nodeMarkers = [];
            edgeLines.forEach(line => {
                map.removeLayer(line);
            });
            edgeLines = [];

            var nodeMap = new Map();
            nodes.forEach(element => {
                nodeMap.set(element.id, element);
                nodeMarkers.push(L.marker([element.latitude, element.longitude], { icon: newIcon }).addTo(map));

                if (!topLeftNode || (element.latitude > topLeftNode.latitude && element.longitude < topLeftNode.longitude)) {
                    topLeftNode = element;
                }
                if (!bottomRightNode || (element.latitude < bottomRightNode.latitude && element.longitude > bottomRightNode.longitude)) {
                    bottomRightNode = element;
                }
            });
            
            edges.forEach(element => {
                let startNode = nodeMap.get(element.originId);
                let endNode = nodeMap.get(element.destinationId);
                if (startNode && endNode) {
                    let latlngs = [
                        [startNode.latitude, startNode.longitude],
                        [endNode.latitude, endNode.longitude]
                    ];
                    edgeLines.push(L.polyline(latlngs, {color: 'blue'}).addTo(map));
                }
            });
            if (topLeftNode && bottomRightNode) {
                let bounds = L.latLngBounds(
                    [bottomRightNode.latitude, topLeftNode.longitude],
                    [topLeftNode.latitude, bottomRightNode.longitude]
                );
                map.flyToBounds(bounds, { duration: 2.0 });
            }
        })
        .catch((error) => {
            console.error('Error:', error);
        });
    }
    input.click();
};
