document.addEventListener("DOMContentLoaded", function () {
  // Code to run when the DOM is fully loaded
  
  // Reset global state on page load
  resetGlobalState();

  // Reset number of deliverers input
  const input = document.getElementById("numberOfDeliverers");
  input.value = 1;
});


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

var nodeMarkers = [];
var edgeLines = [];
var pairColors = {};
var nodeMap = new Map(); // Graphe déjà chargé
var edges_list = []; // Liste des edges déjà chargés
var requestMap = new Map(); // Requests de la tournée déjà chargés
var edgeTourLines = [];
var tourPOIMap = new Map(); // POI de la tournée déjà chargés
var deliveryIdToMarkers = {}; // Map deliveryId -> [markers]

var activeRequestCounter = 0; // Counter for 'still-calculating' requests
var requestList = []; // Liste des demandes de livraison
var delivererList = []; // Liste des livreurs

var delivererETA = {}; // Map delivererId -> ETA string
var numberOfDeliverers = 1; // Nombre de livreurs (par défaut 1)
var numberOfRequests = 1; // Nombre de demandes de livraison (initialement 1)

var delivererLayerGroups = new Map(); // Map delivererId -> L.layerGroup
var layerControl = null; // Contrôle des couches Leaflet
var delivererColors = new Map(); // Map delivererId -> couleur

var allDeliverersTours = {}; // Map delivererId -> tour data
var assignment = {}; // Global assignment variable

// Génère une couleur hexadécimale aléatoire (évite les verts)
function getRandomColor() {
  let color;
  let attempts = 0;
  const maxAttempts = 100;

  do {
    const letters = "0123456789ABCDEF";
    color = "#";
    for (let i = 0; i < 6; i++) {
      color += letters[Math.floor(Math.random() * 16)];
    }
    attempts++;
  } while (isGreenish(color) && attempts < maxAttempts);

  return color;
}

// Reset global variables
function resetGlobalState() {
  // Reset the other global variables
  requestMap.clear();
  tourPOIMap.clear();
  deliveryIdToMarkers = {};
  pairColors = {};
  edgeTourLines = [];
  numberOfDeliverers = 1;
  numberOfRequests = 0;
  delivererETA = {};
  allDeliverersTours = {};
  delivererList = [];
  for (const layerGroup of delivererLayerGroups.values()) {
    layerGroup.clearLayers();
  }
  delivererLayerGroups.clear();
  if (layerControl) {
    map.removeControl(layerControl);
    layerControl = null;
  }
  delivererColors.clear();
}

// Checks if a color is "greenish" (to avoid green colors on the map)
function isGreenish(hexColor) {
  // Convert HEX to RGB
  const r = parseInt(hexColor.substr(1, 2), 16);
  const g = parseInt(hexColor.substr(3, 2), 16);
  const b = parseInt(hexColor.substr(5, 2), 16);

  // Avoid colors where green is dominant (G > R and G > B)
  // and where green is too strong (G > 100)
  return g > r && g > b && g > 100;
}

// Makes an arrow icon pointing up or down with the given color
function createArrowIcon(color, direction, size = 32) {
  const rotation =
    direction === "down" ? "rotate(180 12 12)" : "rotate(0 12 12)";

  return L.divIcon({
    className: "",
    html: `
            <svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" 
                 viewBox="0 0 24 24">
                <g transform="${rotation}">
                    <path fill="${color}" d="M12 2L5 9h4v9h6V9h4z"/>
                </g>
            </svg>
        `,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
  });
}

// Créer des LayerGroups pour chaque livreur basé sur l'assignation
function createDelivererLayerGroups() {
  // Créer un layer group pour chaque livreur
  if (delivererLayerGroups.size === 0) {
    delivererColors.forEach((color, delivererId) => {
      if (delivererId > getNumberOfDeliverers()) return; // Ne pas créer pour les livreurs non utilisés
      delivererLayerGroups.set(delivererId, L.layerGroup());
    });
  } else if (delivererLayerGroups.size < getNumberOfDeliverers()) {
    // Ajouter les layer groups manquants
    for ( let i = delivererLayerGroups.size + 1; i <= getNumberOfDeliverers(); i++ ) {
      delivererLayerGroups.set(i, L.layerGroup());
    }
  }

  // Parcourir toutes les sélections pour assigner les markers aux bons livreurs
  const selects = document.querySelectorAll(".delivery-select");

  selects.forEach((select) => {
    const deliveryId = parseInt(select.getAttribute("data-delivery-id"));
    const selectedDeliverer = parseInt(select.value);

    // Récupérer les markers pour ce deliveryId
    const markers = deliveryIdToMarkers[deliveryId];

    if (markers && delivererLayerGroups.has(selectedDeliverer)) {
      const layerGroup = delivererLayerGroups.get(selectedDeliverer);
      markers.forEach((marker) => {
        // Retirer le marker de la carte principale
        map.removeLayer(marker);
        // L'ajouter au layer group du livreur
        layerGroup.addLayer(marker);
      });
    }
  });

  // Ajouter tous les layer groups à la carte par défaut
  delivererLayerGroups.forEach((layerGroup) => {
    // Only add those that are actively used
    const delivererId = Array.from(delivererLayerGroups.entries()).find(
      ([id, lg]) => lg === layerGroup
    )[0];
    if (delivererId > getNumberOfDeliverers()) {
      // Empty other non-used layer groups
      layerGroup.clearLayers();
      return;
    } // Ne pas ajouter les livreurs non utilisés
    if (!map.hasLayer(layerGroup)) {
      layerGroup.addTo(map);
    }
  });
}

// Mettre à jour le contrôle des couches Leaflet
function updateLayerControl() {
  // Supprimer l'ancien contrôle s'il existe
  if (layerControl) {
    map.removeControl(layerControl);
  }

  // Créer l'objet overlays pour le contrôle
  const overlayMaps = {};

  delivererColors.forEach((color, delivererId) => {
    const layerGroup = delivererLayerGroups.get(delivererId);
    if (layerGroup) {
      // Utiliser du HTML pour afficher la couleur dans le nom
      overlayMaps[`
        <span style="display: inline-flex; align-items: center;">
          <span style="width: 12px; height: 12px; background-color: ${color}; border-radius: 50%; display: inline-block; margin-right: 8px; border: 1px solid #ccc;"></span>
          ${delivererId in delivererETA ? `Livreur ${delivererId} - ETA ${delivererETA[delivererId]}` : `Livreur ${delivererId} - ETA --:--`}
        </span>`
      ] = layerGroup;
    }
  });

  // Créer et ajouter le nouveau contrôle
  layerControl = L.control
    .layers(null, overlayMaps, {
      collapsed: false, // Toujours ouvert
      position: "topright",
    })
    .addTo(map);
}

// Fonction principale pour mettre à jour l'affichage des livreurs
function updateDelivererDisplay() {
  createDelivererLayerGroups();
  updateLayerControl();
}

// Generate the colors for each deliverer
// Colors are fixed for each deliverer during the session
function generateDelivererColors(numberOfDeliverers) {
  if (numberOfDeliverers !== delivererColors.size) {
    delivererColors.clear();
  }
  for (let i = 1; i <= numberOfDeliverers; i++) {
    if (!delivererColors.has(i)) {
      const hue = Math.floor((360 * i) / numberOfDeliverers);
      delivererColors.set(i, `hsl(${hue}, 80%, 50%)`);
    }
  }
}

// Charger la map en fonction du fichier XML choisi
function load_xml_map() {
  let input = document.createElement("input");
  input.type = "file";
  input.accept = ".xml";

  input.onchange = (e) => {
    let file = e.target.files[0];

    if (!file) {
      alert("Veuillez ajouter un fichier XML");
      return;
    }

    blockButtons();

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

        // Reset the global state related to deliveries
        resetGlobalState();

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
            // On ajoute les edges mais sans les afficher pour l'instant
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

        unblockButtons();
      })
      .catch((error) => {
        console.error("Error loading XML map:", error);
        unblockButtons();
        alert("Erreur lors du chargement du plan (voir console).");
      });
  };

  input.oncancel = () => {
    alert("Veuillez ajouter un fichier XML");
    unblockButtons();
  };

  input.onabort = () => {
    alert("Veuillez ajouter un fichier XML");
    unblockButtons();
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

    if (!deliveryFile) {
      alert("Veuillez ajouter un fichier XML");
      return;
    }

    let formData = new FormData();
    formData.append("file", deliveryFile);

    blockButtons();

    fetch("/uploadDeliveries", {
      method: "POST",
      body: formData,
    })
      .then((response) => {
        if (!response.ok) throw new Error("HTTP error " + response.status);
        unblockButtons();
        return response.json();
      })
      .then((data) => {
        if (!data.nodes) {
          console.error("No nodes in response:", data);
          return;
        }

        if (!data.poiMap) {
          console.error("No poiMap in response:", data);
          return;
        }

        // Créer une map pour retrouver le deliveryId à partir de l'id du node
        const nodeIdToDeliveryId = new Map();
        data.nodes.forEach((node) => {
          nodeIdToDeliveryId.set(node.id, node.deliveryId);
        });

        // Reset global state related to deliveries
        resetGlobalState();

        Object.entries(data.poiMap).forEach(([id, poi]) => {
          // Ajouter le deliveryId au node dans le POI
          const nodeId = Number(id);
          const deliveryId = nodeIdToDeliveryId.get(nodeId);

          if (poi.node) {
            poi.node.deliveryId = deliveryId;
          }

          if (poi.type === "PICKUP" && deliveryId !== -1) {
            requestMap.set(nodeId, poi);
          }
        });

        // Reset des POIs de la tournée
        Object.entries(data.poiMap).forEach(([id, poi]) => {
          tourPOIMap.set(Number(id), poi);
        });

        updateNumberOfRequests();

        // Supprime les anciens marqueurs (on veut rafraîchir)
        nodeMarkers.forEach((m) => map.removeLayer(m));
        nodeMarkers = [];
        deliveryIdToMarkers = {}; // Reset la map des markers par deliveryId

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

          // Couleur associée à la paire pickup/delivery
          if (!window.pairColors) window.pairColors = {};
          if (!pairColors[element.deliveryId]) {
            pairColors[element.deliveryId] = getRandomColor();
          }

          const color = pairColors[element.deliveryId];
          const direction = element.type === "pickup" ? "up" : "down";

          const icon = createArrowIcon(color, direction);
          const popUp = L.popup({
            closeButton: false,
            autoClose: false,
            className: "custom-popup",
          }).setContent(`Type: ${element.type}`);
          const marker = L.marker([element.latitude, element.longitude], {
            icon,
          }).addTo(map).bindPopup(popUp);

          // Stocker les informations du marker pour le hover
          marker.deliveryId = element.deliveryId;
          marker.color = color;
          marker.direction = direction;
          marker.type = element.type;
          marker.nodeId = element.id;

          nodeMarkers.push(marker);

          // Grouper les markers par deliveryId
          if (!deliveryIdToMarkers[element.deliveryId]) {
            deliveryIdToMarkers[element.deliveryId] = [];
          }
          deliveryIdToMarkers[element.deliveryId].push(marker);
        });

        // Générer la liste des livraisons dans le panneau de droite
        generateDeliveriesList(
          requestMap.values(),
          getNumberOfDeliverers(),
          pairColors
        );
        generateDelivererColors(getNumberOfDeliverers());
        updateDelivererDisplay();
      })
      .catch((err) => {
        console.error("Error fetching /uploadDeliveries:", err);
        alert(
          "Erreur lors du chargement de la demande de livraison (voir console)."
        );
        unblockButtons();
      });
  };

  input.oncancel = () => {
    alert("Veuillez ajouter un fichier XML");
    unblockButtons();
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

// Function to block buttons
function blockButtons() {
  const btns = document.getElementsByClassName("user-action-button");
  for (let btn of btns) {
    btn.disabled = true;
  }
}

// Function to unblock buttons
function unblockButtons() {
  const btns = document.getElementsByClassName("user-action-button");
  for (let btn of btns) {
    btn.disabled = false;
  }
}

function compute_tour() {
  if (!nodeMap || nodeMap.size === 0) {
    alert("Veuillez d'abord importer un plan avant de calculer une tournée.");
    return;
  }

  blockButtons();
  activeRequestCounter = 0;

  assignment = generateDeliverersAssignment();

  // Diplay the edges tour lines above the existing edges lines
  // Remove previous tour lines
  edgeTourLines.forEach((l) => map.removeLayer(l));
  edgeTourLines = [];

  // Clear control layers content
  for (const layerGroup of delivererLayerGroups.values()) {
    layerGroup.clearLayers();
  }

  // Make separate requests for each deliverer
  for (const [deliverer, poiMap] of Object.entries(assignment)) {
    computeSingleTour(deliverer, poiMap);
  }
}

// Compute the tour for a single deliverer and draw it on the map
function computeSingleTour(deliverer, poiMap) {
  console.log(`Computing tour for ${deliverer} with POIs:`, poiMap);
  // Prepare data to send to backend to compute the tour
  let body = {
    allNodes: Object.fromEntries(nodeMap),
    allEdges: Array.from(edges_list),
    tour: poiMap, // Map<Long, POI>
  };

  
  activeRequestCounter++;
  fetch("http://localhost:8080/runTSP", {
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
      if (!data.solutionOrder) {
        console.error("No tour in response:", data);
        return;
      }

      var bestSolution = data.solutionOrder;
      var arrivalTimes = data.solutionOrderWithArrivalTime;
      var tour = data.solutionPaths; // Map<String, Map<Long, Long>>

      const delivererId = parseInt(deliverer);
      allDeliverersTours[delivererId] = {
        delivererId: delivererId,
        tourOrder: bestSolution,
        tourDetails: [],
        color: delivererColors.get(delivererId),
      };

      const delivererColor =
        delivererColors.get(parseInt(deliverer)) || "#000000";

      const layerGroup = delivererLayerGroups.get(parseInt(deliverer));
      if (layerGroup) {
        layerGroup.addTo(map);
      }

      // Draw new tour lines
      for (let i = 0; i < bestSolution.length - 1; i++) {
        let fromId = bestSolution[i];
        let toId = bestSolution[i + 1];

        const fromNode = nodeMap.get(parseInt(fromId));
        const toNode = nodeMap.get(parseInt(toId));
        const fromPOI = tourPOIMap.get(fromId);
        const toPOI = tourPOIMap.get(toId);

        allDeliverersTours[delivererId].tourDetails.push({
          from: {
            id: fromId,
            type: fromPOI?.type || "UNKNOWN",
            latitude: fromNode?.latitude,
            longitude: fromNode?.longitude,
          },
          to: {
            id: toId,
            type: toPOI?.type || "UNKNOWN",
            latitude: toNode?.latitude,
            longitude: toNode?.longitude,
          },
        });

        let subtour = null;
        let key = `(${fromId}, ${toId})`;
        for (el in tour) {
          if (key in tour[el]) {
            subtour = tour[el][key];
          }
        }

        for (let j = 0; j < Object.keys(subtour).length - 1; j++) {
          let currentId = subtour[j];
          let nextId = subtour[j + 1];
          let startNode = nodeMap.get(parseInt(currentId));
          let endNode = nodeMap.get(parseInt(nextId));
          if (startNode && endNode) {
            let latlngs = [
              [startNode.latitude, startNode.longitude],
              [endNode.latitude, endNode.longitude],
            ];
            edgeTourLines.push(
              L.polyline(latlngs, { color: delivererColor }).addTo(layerGroup)
            );
          }

          currentId = nextId;
          nextId = subtour[currentId];
          currentId = nextId;
          nextId = subtour[currentId];
        }
      }

      // Add arrival times to markers' popups
      for (const [id, arrivalTimeObj] of Object.entries(arrivalTimes)) {
        const nodeId = arrivalTimeObj.left;
        const arrivalTime = arrivalTimeObj.right;
        const marker = nodeMarkers.find((m) => m.nodeId === parseInt(nodeId));
        if (marker) {
          marker.setPopupContent(`Type: ${marker.type}<br>Arrival Time: ${arrivalTime}`);
        }
      }

      // Update ETA for deliverer in the layer control
      // Get last element in arrivalTimes
      const lastArrival = arrivalTimes[arrivalTimes.length - 1].right || "Unknown";
      delivererETA[delivererId] = lastArrival;
      updateLayerControl();

      activeRequestCounter--;
      // Unblock buttons only after all async requests are done
      if (activeRequestCounter === 0) {
        unblockButtons();
      }
    })
    .catch((err) => {
      console.error("Error fetching /runTSP:", err);
      alert("Erreur lors du calcul de la tournée: " + err.message);
      unblockButtons(); // Unblock buttons on error
    });
}

function generateDeliveriesList( deliveries, numberOfDeliverers = 1, pairColors = {}) {
  const deliveriesListContainer = document.getElementById("deliveries-list");
  deliveriesListContainer.innerHTML = "";

  // Comme deliveries est un Map.values(), on le transforme en tableau
  const deliveriesArray = Array.from(deliveries);

  // Filter out the deliveries with deliveryId -1 (warehouse)
  const filteredDeliveries = deliveriesArray.filter(
    (delivery) => delivery.node?.deliveryId !== -1
  );

  filteredDeliveries.forEach((delivery, index) => {
    const deliveryItem = document.createElement("div");
    deliveryItem.className = "delivery-item";

    // 🔹 Récupérer la couleur associée - utiliser le deliveryId du node
    const deliveryId = delivery.node?.deliveryId ?? index;
    const color = pairColors[deliveryId] || "#999";

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
    select.addEventListener("change", () => {
      updateDelivererDisplay();
    });
    select.className = "delivery-select";
    select.setAttribute("data-delivery-id", delivery.node.id);

    for (let i = 1; i <= numberOfDeliverers; i++) {
      const option = document.createElement("option");
      option.value = i;
      option.textContent = `Livreur ${i}`;
      select.appendChild(option);
    }

    // 🔹 Ajouter les événements hover
    deliveryItem.addEventListener("mouseenter", () => {
      highlightMarkers(deliveryId, true);
    });

    deliveryItem.addEventListener("mouseleave", () => {
      highlightMarkers(deliveryId, false);
    });

    // Assembler les éléments
    deliveryItem.appendChild(colorDot);
    deliveryItem.appendChild(label);
    deliveryItem.appendChild(select);

    deliveriesListContainer.appendChild(deliveryItem);
  });
}

// Fonction pour agrandir/réduire les markers d'un deliveryId
function highlightMarkers(deliveryId, highlight) {
  const markers = deliveryIdToMarkers[deliveryId];
  if (!markers) return;

  const size = highlight ? 48 : 32; // Taille agrandie ou normale

  markers.forEach((marker) => {
    const newIcon = createArrowIcon(marker.color, marker.direction, size);
    marker.setIcon(newIcon);
  });
}

function getNumberOfDeliverers() {
  const input = document.getElementById("numberOfDeliverers");
  return input ? parseInt(input.value) || 1 : 1; // Valeur par défaut: 1
}

function updateDeliverersList() {
  const numberOfDeliverers = getNumberOfDeliverers();
  generateDelivererColors(numberOfDeliverers);
  // Mettre à jour la légende
  updateDelivererDisplay();
  generateDeliveriesList(requestMap.values(), numberOfDeliverers, pairColors);
}

const input = document.getElementById("numberOfDeliverers");
input.addEventListener("change", updateDeliverersList);

function generateDeliverersAssignment() {
  const numberOfDeliverers = getNumberOfDeliverers();

  assignment = {};
  for (let i = 1; i <= numberOfDeliverers; i++) {
    assignment[i] = {};
  }

  const selects = document.querySelectorAll(".delivery-select");

  // Add the warehouse POI to each deliverer
  const warehousePOI = Array.from(tourPOIMap.values()).find(
    (poi) => poi.type === "WAREHOUSE"
  );
  for (let i = 1; i <= numberOfDeliverers; i++) {
    assignment[i][warehousePOI.node.id] = warehousePOI;
  }

  selects.forEach((select) => {
    const deliveryId = parseInt(select.getAttribute("data-delivery-id"));
    const selectedDeliverer = parseInt(select.value);

    // Récupérer le POI pickup correspondant dans tourPOIMap
    const pickupPOI = tourPOIMap.get(deliveryId);

    let deliveryPOI = null;
    tourPOIMap.forEach((poi) => {
      if (poi.associatedPoI == deliveryId) {
        deliveryPOI = poi;
      }
    });

    // Ajouter les POIs au livreur sélectionné
    assignment[selectedDeliverer][deliveryId] = pickupPOI;
    assignment[selectedDeliverer][deliveryPOI.node.id] = deliveryPOI;
  });

  return assignment;
}

const plusBtn = document.getElementById("plusBtn");
const minusBtn = document.getElementById("minusBtn");

plusBtn.addEventListener("click", () => {
  let current = parseInt(numberOfDeliverers);
  if (current < numberOfRequests) {
    numberOfDeliverers = current + 1;
    document.getElementById("numberOfDeliverers").value = numberOfDeliverers;
    updateDeliverersList();
  }
});

minusBtn.addEventListener("click", () => {
  let current = parseInt(numberOfDeliverers);
  if (current > 1) {
    numberOfDeliverers = current - 1;
    document.getElementById("numberOfDeliverers").value = numberOfDeliverers;
    updateDeliverersList();
  }
});

function updateNumberOfRequests() {
  numberOfRequests = requestMap.size;
}
// Fonction pour exporter les tournées en JSON
// Fonction pour exporter les tournées en JSON avec toutes les données nécessaires
function exportToursToJSON() {
  // Vérifier qu'il y a des tournées à exporter
  if (Object.keys(allDeliverersTours).length === 0) {
    alert("Aucune tournée à exporter. Veuillez d'abord calculer les tournées.");
    return;
  }

  // Vérifier que le plan et les POIs sont chargés
  if (!nodeMap || nodeMap.size === 0) {
    alert("Le plan n'est pas chargé. Impossible d'exporter.");
    return;
  }

  if (!tourPOIMap || tourPOIMap.size === 0) {
    alert("Les points de pickup/delivery ne sont pas chargés. Impossible d'exporter.");
    return;
  }

  // Créer l'objet JSON complet avec toutes les informations
  const exportData = {
    exportDate: new Date().toISOString(),
    version: "2.0",
    
    // Informations sur le plan (tous les noeuds et arêtes)
    map: {
      nodes: Array.from(nodeMap.entries()).map(([id, node]) => ({
        id: id,
        latitude: node.latitude,
        longitude: node.longitude
      })),
      edges: edges_list.map(edge => ({
        originId: edge.originId,
        destinationId: edge.destinationId,
        length: edge.length
      }))
    },
    
    // Informations sur les pickups et deliveries
    deliveries: {
      pois: Array.from(tourPOIMap.entries()).map(([id, poi]) => ({
        id: id,
        type: poi.type,
        nodeId: poi.node?.id,
        deliveryId: poi.node?.deliveryId,
        associatedPoI: poi.associatedPoI,
        duration: poi.duration
      })),
      pairColors: pairColors
    },
    
    // Informations sur les livreurs et leurs tournées
    deliverers: {
      numberOfDeliverers: getNumberOfDeliverers(),
      colors: Array.from(delivererColors.entries()).map(([id, color]) => ({
        delivererId: id,
        color: color
      })),
      
      // Tournées détaillées avec les trajets complets
      tours: Object.entries(allDeliverersTours).map(([delivererId, tourData]) => {
        // Récupérer les lignes de cette tournée
        const delivererIdInt = parseInt(delivererId);
        const layerGroup = delivererLayerGroups.get(delivererIdInt);
        const tourLines = [];
        
        if (layerGroup) {
          layerGroup.eachLayer((layer) => {
            if (layer instanceof L.Polyline) {
              const latlngs = layer.getLatLngs();
              tourLines.push({
                from: { lat: latlngs[0].lat, lng: latlngs[0].lng },
                to: { lat: latlngs[1].lat, lng: latlngs[1].lng }
              });
            }
          });
        }
        
        return {
          delivererId: delivererIdInt,
          color: tourData.color,
          tourOrder: tourData.tourOrder,
          tourDetails: tourData.tourDetails,
          // Trajets exacts entre chaque segment
          exactPaths: tourLines
        };
      }),
      
      // Assigning of pickups/deliveries to deliverers
      assignments: assignment,

      // Assignment of tour times
      delivererETA: delivererETA
    }
  };

  // Convertir en JSON avec indentation pour la lisibilité
  const jsonString = JSON.stringify(exportData, null, 2);

  // Créer un blob et télécharger le fichier
  const blob = new Blob([jsonString], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  
  // Créer un lien de téléchargement temporaire
  const link = document.createElement("a");
  link.href = url;
  link.download = `tournee_complete_${new Date().toISOString().split('T')[0]}.json`;
  
  // Déclencher le téléchargement
  document.body.appendChild(link);
  link.click();
  
  // Nettoyer
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
  
  alert("Tournée complète exportée avec succès !");
}

// Fonction pour importer une tournée complète depuis un JSON (avec plan, pickups/deliveries et trajets)
function importToursFromJSON() {
  console.log("Importing complete tour from JSON...");

  let input = document.createElement("input");
  input.type = "file";
  input.accept = ".json";

  input.onchange = (e) => {
    let file = e.target.files[0];

    if (!file) {
      alert("Veuillez sélectionner un fichier JSON");
      return;
    }

    blockButtons();

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const importedData = JSON.parse(event.target.result);

        // Vérifier le format du fichier
        if (!importedData.map || !importedData.deliveries || !importedData.deliverers) {
          alert("Format JSON invalide : données manquantes (map, deliveries ou deliverers)");
          unblockButtons();
          return;
        }

        // ===== 1. CHARGER LE PLAN (NODES ET EDGES) =====
        // Nettoyer les anciens markers et edges du plan
        nodeMarkers.forEach((m) => map.removeLayer(m));
        nodeMarkers = [];
        edgeLines.forEach((l) => map.removeLayer(l));
        edgeLines = [];
        
        // Charger les nodes
        nodeMap.clear();
        importedData.map.nodes.forEach(node => {
          nodeMap.set(node.id, {
            id: node.id,
            latitude: node.latitude,
            longitude: node.longitude
          });
        });
        
        // Charger les edges
        edges_list = importedData.map.edges.map(edge => ({
          originId: edge.originId,
          destinationId: edge.destinationId,
          length: edge.length
        }));
        
        // Créer les polylines des edges (non affichées par défaut)
        importedData.map.edges.forEach(edge => {
          const startNode = nodeMap.get(edge.originId);
          const endNode = nodeMap.get(edge.destinationId);
          if (startNode && endNode) {
            const latlngs = [
              [startNode.latitude, startNode.longitude],
              [endNode.latitude, endNode.longitude]
            ];
            const line = L.polyline(latlngs, { color: "#50d76b" });
            edgeLines.push(line);
          }
        });
        
        // Ajuster la vue sur le plan
        if (importedData.map.nodes.length > 0) {
          const lats = importedData.map.nodes.map(n => n.latitude);
          const lngs = importedData.map.nodes.map(n => n.longitude);
          const bounds = L.latLngBounds(
            [Math.min(...lats), Math.min(...lngs)],
            [Math.max(...lats), Math.max(...lngs)]
          );
          map.flyToBounds(bounds, { duration: 2.0 });
        }
        
        // Activer le bouton "Afficher le plan"
        toggleEdgesBtn = document.getElementById("toggleEdgesBtn");
        if (toggleEdgesBtn) {
          toggleEdgesBtn.style.display = "inline-block";
          toggleEdgesBtn.textContent = "Afficher le plan";
          edgesVisible = false;
          planLoaded = true;
          
          const planButton = document.querySelector(".buttons button:nth-child(1)");
          if (planButton) {
            planButton.style.backgroundColor = "var(--primary-green)";
            planButton.style.color = "white";
          }
        }
        
        // ===== 2. CHARGER LES PICKUPS ET DELIVERIES =====
        
        // Charger les POIs
        tourPOIMap.clear();
        requestMap.clear();
        pairColors = importedData.deliveries.pairColors || {};
        
        importedData.deliveries.pois.forEach(poi => {
          const poiObj = {
            type: poi.type,
            node: nodeMap.get(poi.nodeId),
            associatedPoI: poi.associatedPoI,
            duration: poi.duration
          };
          
          // Ajouter le deliveryId au node si disponible
          if (poiObj.node && poi.deliveryId !== undefined) {
            poiObj.node.deliveryId = poi.deliveryId;
          }
          
          tourPOIMap.set(poi.id, poiObj);
          
          // Ajouter aux requests si c'est un pickup (sauf warehouse)
          if (poi.type === "PICKUP" && poi.deliveryId !== -1) {
            requestMap.set(poi.id, poiObj);
          }
        });
        
        updateNumberOfRequests();

        // Créer les markers pour les pickups/deliveries
        deliveryIdToMarkers = {};
        importedData.deliveries.pois.forEach(poi => {
          const node = nodeMap.get(poi.nodeId);
          if (!node) return;
          
          // Warehouse
          if (poi.deliveryId === -1) {
            nodeMarkers.push(
              L.marker([node.latitude, node.longitude], {
                icon: warehouseIcon
              }).addTo(map)
            );
            return;
          }
          
          // Pickup ou Delivery
          const color = pairColors[poi.deliveryId] || getRandomColor();
          if (!pairColors[poi.deliveryId]) {
            pairColors[poi.deliveryId] = color;
          }
          
          const direction = poi.type === "PICKUP" ? "up" : "down";
          const icon = createArrowIcon(color, direction);
          
          const marker = L.marker([node.latitude, node.longitude], { icon }).addTo(map);
          marker.deliveryId = poi.deliveryId;
          marker.color = color;
          marker.direction = direction;
          
          nodeMarkers.push(marker);
          
          if (!deliveryIdToMarkers[poi.deliveryId]) {
            deliveryIdToMarkers[poi.deliveryId] = [];
          }
          deliveryIdToMarkers[poi.deliveryId].push(marker);
        });
        
        // ===== 3. CONFIGURER LES LIVREURS =====
        const importedNumberOfDeliverers = importedData.deliverers.numberOfDeliverers;
        numberOfDeliverers = importedNumberOfDeliverers;

        // Mettre à jour le nombre de livreurs
        const deliverersInput = document.getElementById("numberOfDeliverers");
        if (deliverersInput) {
          deliverersInput.value = importedNumberOfDeliverers;
        }
        
        // Charger les couleurs des livreurs
        delivererColors.clear();
        importedData.deliverers.colors.forEach(({ delivererId, color }) => {
          delivererColors.set(delivererId, color);
        });
        
        // Générer la liste des deliveries avec les bons assignments
        generateDeliveriesList(requestMap.values(), importedNumberOfDeliverers, pairColors);
        
        // Appliquer les assignments depuis le JSON
        if (importedData.deliverers.assignments) {
          Object.entries(importedData.deliverers.assignments).forEach(([delivererId, pois]) => {
            Object.keys(pois).forEach(poiId => {
              const poi = tourPOIMap.get(parseInt(poiId));
              if (poi && poi.type === "PICKUP" && poi.node && poi.node.deliveryId !== -1) {
                const select = document.querySelector(`select[data-delivery-id="${poiId}"]`);
                if (select) {
                  select.value = delivererId;
                }
              }
            });
          });
        }
        
        // Assign deliverer ETAs
        delivererETA = importedData.deliverers.delivererETA || {};

        // Créer les layer groups pour les livreurs
        generateDelivererColors(importedNumberOfDeliverers);
        updateDelivererDisplay();
        
        // ===== 4. CHARGER LES TOURNÉES AVEC LES TRAJETS EXACTS =====
        // Nettoyer les anciennes tournées
        edgeTourLines.forEach((l) => map.removeLayer(l));
        edgeTourLines = [];
        
        for (const layerGroup of delivererLayerGroups.values()) {
          layerGroup.clearLayers();
        }
        
        // Reconstruire allDeliverersTours
        allDeliverersTours = {};
        importedData.deliverers.tours.forEach(tourData => {
          allDeliverersTours[tourData.delivererId] = {
            delivererId: tourData.delivererId,
            tourOrder: tourData.tourOrder,
            tourDetails: tourData.tourDetails,
            color: tourData.color
          };
        });
        
        // Dessiner les trajets exacts
        importedData.deliverers.tours.forEach(tourData => {
          const delivererIdInt = tourData.delivererId;
          const delivererColor = tourData.color || delivererColors.get(delivererIdInt) || "#000000";
          const layerGroup = delivererLayerGroups.get(delivererIdInt);
          
          if (!layerGroup) {
            console.error(`No layer group found for deliverer ${delivererIdInt}`);
            return;
          }
          
          layerGroup.addTo(map);
          
          // Dessiner tous les segments exacts du trajet
          if (tourData.exactPaths && tourData.exactPaths.length > 0) {
            tourData.exactPaths.forEach(pathSegment => {
              const latlngs = [
                [pathSegment.from.lat, pathSegment.from.lng],
                [pathSegment.to.lat, pathSegment.to.lng]
              ];
              
              const line = L.polyline(latlngs, {
                color: delivererColor,
                weight: 4,
                opacity: 0.7
              }).addTo(layerGroup);
              
              edgeTourLines.push(line);
            });
          }
          
          console.log(`Tour drawn for deliverer ${delivererIdInt} with ${tourData.exactPaths?.length || 0} segments`);
        });
        
        alert(`Tournée complète importée avec succès !`);
        unblockButtons();
      } catch (error) {
        console.error("Error parsing JSON:", error);
        alert("Erreur lors de la lecture du fichier JSON : " + error.message);
        unblockButtons();
      }
    };

    reader.onerror = () => {
      alert("Erreur lors de la lecture du fichier");
      unblockButtons();
    };

    reader.readAsText(file);
  };

  input.oncancel = () => {
    unblockButtons();
  };

  input.click();
}
