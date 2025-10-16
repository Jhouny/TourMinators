// Coordonnées GPS du centre de la carte (ici Lyon) à l'initialisation
let lat = 45.764;
let lon = 4.8357;

// On initialise la carte (en lui passant 'map' qui est l'ID de la DIV contenant la carte)
let map = L.map("map", {
  zoom: 13,
  center: [lat, lon],
});

// On ajoute le calque permettant d'afficher les images de la carte
L.tileLayer("https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png", {
  minZoom: 1,
  maxZoom: 20,
  attribution:
    'données © <a href="//osm.org/copyright">OpenStreetMap</a>/ODbL - rendu <a href="//openstreetmap.fr">OSM France</a>',
}).addTo(map);

var newIcon = L.icon({
  iconUrl: "circle-icon.png",
  iconSize: [15, 15], // size of the icon
  iconAnchor: [7, 7], // point of the icon which will correspond to marker's location
});

var warehouseIcon = L.icon({
  iconUrl: "warehouse-icon.png",
  iconSize: [20, 20],
  iconAnchor: [10, 10],
});

var nodeMarkers = [];
var edgeLines = [];
var nodeMap = new Map(); // Graphe déjà chargé
var edges_list = []; // Liste des edges déjà chargés
var tourPOIMap = new Map(); // POI de la tournée déjà chargés
var edgeTourLines = [];

// Génère une couleur hexadécimale aléatoire
function getRandomColor() {
  const letters = "0123456789ABCDEF";
  let color = "#";
  for (let i = 0; i < 6; i++) {
    color += letters[Math.floor(Math.random() * 16)];
  }
  return color;
}

// Génère une icône de flèche colorée (orientée selon le type)
function createArrowIcon(color, direction) {
  const rotation =
    direction === "down" ? "rotate(180 12 12)" : "rotate(0 12 12)";
  return L.divIcon({
    className: "",
    html: `
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" 
                 viewBox="0 0 24 24">
                <g transform="${rotation}">
                    <path fill="${color}" d="M12 2L5 9h4v9h6V9h4z"/>
                </g>
            </svg>
        `,
    iconSize: [20, 20],
    iconAnchor: [10, 10],
  });
}

// Charger la map en fonction du fichier XML choisi
function load_xml_map() {
  console.log("Loading XML map...");

  let input = document.createElement("input");
  input.type = "file";
  input.accept = ".xml";

  input.onchange = (e) => {
    let file = e.target.files[0];
    let formData = new FormData();
    formData.append("file", file);

    fetch("/upload", { method: "POST", body: formData })
      .then((response) => {
        if (!response.ok) throw new Error("HTTP error " + response.status);
        return response.json();
      })
      .then((data) => {
        var nodes = data.nodes;
        var edges = data.edges;

        console.log("Nodes:", nodes);
        console.log("Edges:", edges);

        // Supprimer anciens markers et edges
        nodeMarkers.forEach((m) => map.removeLayer(m));
        nodeMarkers = [];
        edgeLines.forEach((l) => map.removeLayer(l));
        edgeLines = [];

        // Réinitialiser le graphe global
        nodeMap.clear();
        nodes.forEach((node) => nodeMap.set(node.id, node));

        // Réinitialiser la liste des edges
        edges_list = edges;

        // Variables pour calculer les bounds
        let topLeftNode = null;
        let bottomRightNode = null;

        // Ajouter les markers
        nodes.forEach((node) => {
          if (
            !topLeftNode ||
            (node.latitude > topLeftNode.latitude &&
              node.longitude < topLeftNode.longitude)
          ) {
            topLeftNode = node;
          }
          if (
            !bottomRightNode ||
            (node.latitude < bottomRightNode.latitude &&
              node.longitude > bottomRightNode.longitude)
          ) {
            bottomRightNode = node;
          }
        });

        // Ajouter les edges
        edges.forEach((edge) => {
          let startNode = nodeMap.get(edge.originId);
          let endNode = nodeMap.get(edge.destinationId);
          if (startNode && endNode) {
            let latlngs = [
              [startNode.latitude, startNode.longitude],
              [endNode.latitude, endNode.longitude],
            ];
            edgeLines.push(
              L.polyline(latlngs, { color: "#50d76b" }).addTo(map)
            );
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
      .catch((error) => console.error("Error loading XML map:", error));
  };

  input.click();
}

function load_xml_delivery() {
  if (!nodeMap || nodeMap.size === 0) {
    alert(
      "Veuillez d'abord importer un plan avant de charger une demande de livraison."
    );
    return;
  }

  let input = document.createElement("input");
  input.type = "file";
  input.accept = ".xml";

  input.onchange = (e) => {
    let deliveryFile = e.target.files[0];
    let formData = new FormData();
    formData.append("file", deliveryFile);

    fetch("/uploadDeliveries", {
      method: "POST",
      body: formData,
    })
      .then((response) => {
        if (!response.ok) throw new Error("HTTP error " + response.status);
        return response.json();
      })
      .then((data) => {
        console.log("Pickup/Delivery response:", data);

        if (!data.nodes) {
          console.error("No nodes in response:", data);
          return;
        }

        // Reset des POI de la tournée
        tourPOIMap.clear();
        data.nodes.forEach((node) => tourPOIMap.set(node.id, node));

        // Supprime les anciens marqueurs (on veut rafraîchir)
        nodeMarkers.forEach((m) => map.removeLayer(m));
        nodeMarkers = [];

        // map deliveryId -> couleur (persistant pour cette réponse)
        const colorMap = new Map();
        let colorIndex = 0;

        data.nodes.forEach((element) => {
          // entree de sécurité si les champs manquent
          if (
            typeof element.latitude !== "number" ||
            typeof element.longitude !== "number"
          ) {
            console.warn("Node missing coords:", element);
            return;
          }

          // entrepot
          if (element.deliveryId === -1) {
            nodeMarkers.push(
              L.marker([element.latitude, element.longitude], {
                icon: warehouseIcon,
              }).addTo(map)
            );
            return;
          }

          // On suppose que chaque deliveryId correspond à une paire (pickup/delivery)
          if (element.deliveryId === -1) {
            // entrepôt
            nodeMarkers.push(
              L.marker([element.latitude, element.longitude], {
                icon: warehouseIcon,
              }).addTo(map)
            );
          } else {
            // Couleur associée à la paire pickup/delivery
            if (!window.pairColors) window.pairColors = {};
            if (!pairColors[element.deliveryId]) {
              pairColors[element.deliveryId] = getRandomColor();
            }

            const color = pairColors[element.deliveryId];
            console.log(`élement du type est ${element.type} et id est ${element.deliveryId}`);
            const direction = element.type === "pickup" ? "up" : "down";

            const icon = createArrowIcon(color, direction);

            nodeMarkers.push(
              L.marker([element.latitude, element.longitude], { icon }).addTo(
                map
              )
            );
          }
        });
      })

      .catch((err) => {
        console.error("Error fetching /uploadDeliveries:", err);
        alert(
          "Erreur lors du chargement de la demande de livraison (voir console)."
        );
      });
  };

  input.click();
}

function compute_tour() {
  if (!nodeMap || nodeMap.size === 0) {
    alert("Veuillez d'abord importer un plan avant de calculer une tournée.");
    return;
  }

  // Prepare data to send to backend to compute the tour
  let formData = new FormData();
  // Add actual parameters as needed
  formData.append("all_nodes", JSON.stringify(Object.fromEntries(nodeMap))); // all_nodes Map<Long,Node>
  // formData.append("all_edges", JSON.stringify(Array.from(edges_list.values()))); // all_edges Edge[]
  // formData.append("tourPOI", JSON.stringify(Object.fromEntries(tourPOIMap))); // tour Map<Long, POI>

  console.log("Computing tour...");

  // fetch("/runTSP", { method: "POST", body: formData }) 

  fetch("/test_tour.json", {
    method: "GET",
  })
    .then((response) => {
      if (!response.ok) throw new Error("HTTP error " + response.status);
      return response.json();
    })
    .then((data) => {
      console.log("Tour response:", data);
      if (!data.tour || data.tour.length === 0) {
        console.error("No tour in response:", data);
        return;
      }
      var bestSolution = data.bestSolution; // Pair<Long,LocalTime> []
      var POIbestSolution = bestSolution.map((bs) => bs.id); //List<Long>
      console.log("POIbestSolution:", POIbestSolution);
      var LocalTimebestSolution = bestSolution.map((bs) => bs.time); //List<LocalTime>
      var tour = data.tour; //Map<Pair<Long,Long>, Map<Long,Long>>
      console.log("Tour map:", tour);

      // Diplay the edges tour lines above the existing edges lines
      // Remove previous tour lines
      edgeTourLines.forEach((l) => map.removeLayer(l));
      edgeTourLines = [];

      // Draw new tour lines

      for (let i = 0; i < POIbestSolution.length - 1; i++) {
        let fromId = POIbestSolution[i];
        let toId = POIbestSolution[i + 1];
        console.log(`Drawing tour segment from ${fromId} to ${toId}`);
        let subtour = tour[`(${fromId},${toId})`];
        let currentId = toId;
        let nextId = subtour[currentId];
        console.log(`Drawing subtour from ${fromId} to ${toId}:`, subtour);
        while (currentId && currentId !== fromId) {
            let startNode = nodeMap.get(parseInt(currentId));
            let endNode = nodeMap.get(parseInt(nextId));
            console.log(`Predecessor: ${currentId}, Arrival: ${nextId}`);
            console.log("Start Node:", startNode);
            if (startNode && endNode) {
            console.log(`Drawing edge from ${currentId} to ${nextId}`);
                let latlngs = [
                    [startNode.latitude, startNode.longitude],
                    [endNode.latitude, endNode.longitude],
                ];
                edgeTourLines.push(
                    L.polyline(latlngs, { color: "#0b3213" }).addTo(map)
                );
            }

            currentId = nextId;
            nextId = subtour[currentId];
        }
      }
    })
    .catch((err) => {
      console.error("Error fetching /computeTour:", err);
    });
}
