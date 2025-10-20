// Coordonnées GPS du centre de la carte (ici Lyon) à l'initialisation
let lat = 45.764;
let lon = 4.8357;
let edgesVisible = false;
let toggleEdgesBtn = null;
let planLoaded = false;

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

var warehouseIcon = L.icon({
  iconUrl: "warehouse-icon.png",
  iconSize: [20, 20],
  iconAnchor: [10, 10],
  iconUrl: "warehouse-icon.png",
  iconSize: [20, 20],
  iconAnchor: [10, 10],
});

var nodeMarkers = [];
var edgeLines = [];
var pairColors = {};
var nodeMap = new Map(); // Graphe déjà chargé
var edges_list = []; // Liste des edges déjà chargés
var requestMap = new Map(); // Requests de la tournée déjà chargés
var edgeTourLines = [];
var tourPOIMap = new Map(); // POI de la tournée déjà chargés

var requestList = []; // Liste des demandes de livraison
var delivererList = []; // Liste des livreurs

var numberOfDeliverers = 1; // Nombre de livreurs (par défaut 1)

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
            // On ajoute les edges mais sans les afficher pour l’instant
            let line = L.polyline(latlngs, { color: "#50d76b" });
            edgeLines.push(line);
          }
        });

        // Ajuster le zoom pour englober tous les nodes
        if (topLeftNode && bottomRightNode) {
          let bounds = L.latLngBounds(
            [bottomRightNode.latitude, topLeftNode.longitude],
            [topLeftNode.latitude, bottomRightNode.longitude]
          );
          map.flyToBounds(bounds, { duration: 2.0 });
          // Activer le bouton "Afficher le plan"
          toggleEdgesBtn = document.getElementById("toggleEdgesBtn");
          toggleEdgesBtn.style.display = "inline-block";
          toggleEdgesBtn.textContent = "Afficher le plan";
          edgesVisible = false;
          planLoaded = true;

          // Changer la couleur du bouton "Charger un plan"
          const planButton = document.querySelector(
            ".buttons button:nth-child(1)"
          );
          planButton.style.backgroundColor = "var(--primary-green)";
          planButton.style.color = "white";
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

        if (!data.poiMap) {
          console.error("No poiMap in response:", data);
          return;
        }

        // Reset des Requests de la tournée
        requestMap.clear();

        Object.entries(data.poiMap).forEach(([id, poi]) => {
          console.log("POI:", poi.type);
          if (poi.type === "PICKUP") {
            console.log("Adding pickup POI:", poi.type);
            requestMap.set(Number(id), poi);
          }
        });

        // Reset des POIs de la tournée

        Object.entries(data.poiMap).forEach(([id, poi]) => {
          tourPOIMap.set(Number(id), poi);
        });

        console.log("Updated requestMap:", requestMap);
        console.log("Updated tourPOIMap:", tourPOIMap);

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
            console.log(
              `élement du type est ${element.type} et id est ${element.deliveryId}`
            );
            console.log(
              `élement du type est ${element.type} et id est ${element.deliveryId}`
            );
            const direction = element.type === "pickup" ? "up" : "down";

            const icon = createArrowIcon(color, direction);

            nodeMarkers.push(
              L.marker([element.latitude, element.longitude], { icon }).addTo(
                map
              )
            );
          }
        });

        // Générer la liste des livraisons dans le panneau de droite
        generateDeliveriesList(requestMap.values(), getNumberOfDeliverers(), pairColors);
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

function toggleEdges() {
  if (!planLoaded) {
    alert("Veuillez d'abord charger un plan.");
    return;
  }

  if (edgesVisible) {
    // Masquer les edges
    edgeLines.forEach((l) => map.removeLayer(l));
    toggleEdgesBtn.textContent = "Afficher le plan";
    toggleEdgesBtn.classList.remove("active");
  } else {
    // Afficher les edges
    edgeLines.forEach((l) => l.addTo(map));
    toggleEdgesBtn.textContent = "Masquer le plan";
    toggleEdgesBtn.classList.add("active");
  }

  edgesVisible = !edgesVisible;
}

function compute_tour() {
  if (!nodeMap || nodeMap.size === 0) {
    alert("Veuillez d'abord importer un plan avant de calculer une tournée.");
    return;
  }

  // Prepare data to send to backend to compute the tour
  let body = {
    allNodes: Object.fromEntries(nodeMap),
    allEdges: Array.from(edges_list),
    tour: Object.fromEntries(tourPOIMap),
  };

  console.log("Computing tour...");

  fetch("http://localhost:8090/runTSP", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
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
          currentId = nextId;
          nextId = subtour[currentId];
        }
      }
    })
    .catch((err) => {
      console.error("Error fetching /computeTour:", err);
    });
}

function generateDeliveriesList(deliveries, numberOfDeliverers = 1, pairColors = {}) {
  const deliveriesListContainer = document.getElementById("deliveries-list");
  deliveriesListContainer.innerHTML = "";

  // Comme deliveries est un Map.values(), on le transforme en tableau
  const deliveriesArray = Array.from(deliveries);

  deliveriesArray.forEach((delivery, index) => {
    const deliveryItem = document.createElement("div");
    deliveryItem.className = "delivery-item";

    // 🔹 Récupérer la couleur associée au deliveryId
    const deliveryId = delivery.deliveryId ?? delivery.node?.id ?? index;
    const color = pairColors[delivery.associatedPoI] || pairColors[deliveryId] || "#999";

    console.log(`Delivery ID: ${deliveryId}, Color: ${pairColors[delivery.associatedPoI] || pairColors[deliveryId]}`);

    // 🔹 Créer la pastille colorée
    const colorDot = document.createElement("span");
    colorDot.className = "color-dot";
    colorDot.style.backgroundColor = color;

    // 🔹 Label de la demande
    const label = document.createElement("span");
    label.className = "delivery-label";
    label.textContent = `Demande no. ${index + 1}`;

    // 🔹 Sélecteur de livreur
    const select = document.createElement("select");
    select.className = "delivery-select";
    select.setAttribute("data-delivery-id", deliveryId);

    for (let i = 1; i <= numberOfDeliverers; i++) {
      const option = document.createElement("option");
      option.value = i;
      option.textContent = `Livreur ${i}`;
      select.appendChild(option);
    }

    // Assembler les éléments
    deliveryItem.appendChild(colorDot);
    deliveryItem.appendChild(label);
    deliveryItem.appendChild(select);

    deliveriesListContainer.appendChild(deliveryItem);
  });
}

function getNumberOfDeliverers() {
  const input = document.getElementById("numberOfDeliverers");
  return input ? parseInt(input.value) || 1 : 1; // Valeur par défaut: 1
}

function updateDeliverersList() {
  const numberOfDeliverers = getNumberOfDeliverers();
  generateDeliveriesList(requestMap.values(), numberOfDeliverers);
}

const input = document.getElementById("numberOfDeliverers");
input.addEventListener("input", updateDeliverersList);

function generateDeliverersAssignment() {
  const numberOfDeliverers = getNumberOfDeliverers();

  const assignment = {};
  for (let i = 1; i <= numberOfDeliverers; i++) {
    assignment[`livreur ${i}`] = {};
  }

  const selects = document.querySelectorAll(".delivery-select");

  selects.forEach((select) => {
    const deliveryId = parseInt(select.getAttribute("data-delivery-id"));
    const selectedDeliverer = parseInt(select.value);

    // Récupérer le POI pickup correspondant dans tourPOIMap
    const pickupPOI = tourPOIMap.get(deliveryId);

    if (pickupPOI) {
      // Trouver le POI delivery associé (même deliveryId mais type DELIVERY)
      let deliveryPOI = null;
      for (let [id, poi] of tourPOIMap.entries()) {
        if (
          poi.deliveryId === pickupPOI.deliveryId &&
          poi.type === "DELIVERY"
        ) {
          deliveryPOI = poi;
          break;
        }
      }

      // Ajouter les POIs au livreur sélectionné
      const delivererKey = `livreur ${selectedDeliverer}`;
      assignment[delivererKey][deliveryId] = pickupPOI;

      if (deliveryPOI) {
        // Trouver l'ID du POI delivery dans tourPOIMap
        for (let [id, poi] of tourPOIMap.entries()) {
          if (poi === deliveryPOI) {
            assignment[delivererKey][id] = deliveryPOI;
            break;
          }
        }
      }
    }
  });

  return assignment;
}

// Fonction pour envoyer l'assignation au backend
function sendDeliverersAssignment() {
  if (!nodeMap || nodeMap.size === 0) {
    alert("Veuillez d'abord importer un plan.");
    return;
  }

  if (tourPOIMap.size === 0) {
    alert("Veuillez d'abord charger une demande de livraison.");
    return;
  }

  const assignment = generateDeliverersAssignment();

  console.log("Assignment à envoyer:", assignment);

  fetch("http://localhost:8090/assignDeliverers", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(assignment),
  })
    .then((response) => {
      if (!response.ok) throw new Error("HTTP error " + response.status);
      return response.json();
    })
    .then((data) => {
      console.log("Response from backend:", data);
      alert("Assignation des livreurs effectuée avec succès !");
    })
    .catch((err) => {
      console.error("Error sending assignment:", err);
      alert("Erreur lors de l'envoi de l'assignation.");
    });
}
