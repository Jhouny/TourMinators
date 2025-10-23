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

var requestList = []; // Liste des demandes de livraison
var delivererList = []; // Liste des livreurs

var numberOfDeliverers = 1; // Nombre de livreurs (par défaut 1)

var delivererLayerGroups = new Map(); // Map delivererId -> L.layerGroup
var layerControl = null; // Contrôle des couches Leaflet
var delivererColors = new Map(); // Map delivererId -> couleur

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
  // Nettoyer les anciens layer groups
  /*delivererLayerGroups.forEach((layerGroup) => {
    map.removeLayer(layerGroup);
  });
  delivererLayerGroups.clear();*/

  // Créer un layer group pour chaque livreur
  if (delivererLayerGroups.size === 0) {
    delivererColors.forEach((color, delivererId) => {
      if (delivererId > getNumberOfDeliverers()) return; // Ne pas créer pour les livreurs non utilisés
      delivererLayerGroups.set(delivererId, L.layerGroup());
    });
  } else if (delivererLayerGroups.size < getNumberOfDeliverers()) {
    // Ajouter les layer groups manquants
    for (let i = delivererLayerGroups.size + 1; i <= getNumberOfDeliverers(); i++) {
      const color = delivererColors.get(i);
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
      markers.forEach(marker => {
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
    const delivererId = Array.from(delivererLayerGroups.entries()).find(([id, lg]) => lg === layerGroup)[0];
    if (delivererId > getNumberOfDeliverers()) {
      // Empty other non-used layer groups
      layerGroup.clearLayers();
      return;
    }; // Ne pas ajouter les livreurs non utilisés
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
      overlayMaps[`<span style="display: inline-flex; align-items: center;">
        <span style="width: 12px; height: 12px; background-color: ${color}; 
        border-radius: 50%; display: inline-block; margin-right: 8px; border: 1px solid #ccc;"></span>
        Livreur ${delivererId}
      </span>`] = layerGroup;
    }
  });

  // Créer et ajouter le nouveau contrôle
  layerControl = L.control.layers(null, overlayMaps, {
    collapsed: false, // Toujours ouvert
    position: 'topright'
  }).addTo(map);
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

// Créer ou mettre à jour la légende des livreurs
function updateDelivererLegend(numberOfDeliverers) {
  // Supprimer l'ancienne légende si elle existe
  const oldLegend = document.querySelector('.deliverer-legend');
  if (oldLegend) {
    oldLegend.remove();
  }
  
  // Si pas de livreurs, ne rien afficher
  if (numberOfDeliverers === 0) {
    return;
  }
  
  // Créer la nouvelle légende
  const legend = document.createElement('div');
  legend.className = 'deliverer-legend';
  
  const title = document.createElement('h4');
  title.textContent = 'Livreurs';
  legend.appendChild(title);
  
  // Ajouter chaque livreur avec sa couleur
  delivererColors.forEach((color, delivererId) => {
    if (delivererId > numberOfDeliverers) return; // Ne pas afficher les livreurs non utilisés
    const item = document.createElement('div');
    item.className = 'deliverer-legend-item';
    
    const colorDot = document.createElement('span');
    colorDot.className = 'deliverer-color-dot';
    colorDot.style.backgroundColor = color;
    
    const label = document.createElement('span');
    label.className = 'deliverer-legend-label';
    label.textContent = `Livreur ${delivererId}`;
    
    item.appendChild(colorDot);
    item.appendChild(label);
    legend.appendChild(item);
  });
  
  // Ajouter la légende au container de la carte
  const mapContainer = document.getElementById('map');
  mapContainer.appendChild(legend);
}

// Charger la map en fonction du fichier XML choisi
function load_xml_map() {
  console.log("Loading XML map...");

  let input = document.createElement("input");
  input.type = "file";
  input.accept = ".xml";

  input.onchange = (e) => {
    let file = e.target.files[0];

    if (!file) {
      alert("Veuillez ajouter un fichier XML");
      return;
    }

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
      })
      .catch((error) => console.error("Error loading XML map:", error));
  };

  input.oncancel = () => {
    alert("Veuillez ajouter un fichier XML");
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

        // Créer une map pour retrouver le deliveryId à partir de l'id du node
        const nodeIdToDeliveryId = new Map();
        data.nodes.forEach((node) => {
          nodeIdToDeliveryId.set(node.id, node.deliveryId);
        });

        console.log("nodeIdToDeliveryId Map:", nodeIdToDeliveryId);

        // Reset des Requests de la tournée
        requestMap.clear();

        Object.entries(data.poiMap).forEach(([id, poi]) => {
          // Ajouter le deliveryId au node dans le POI
          const nodeId = Number(id);
          const deliveryId = nodeIdToDeliveryId.get(nodeId);
          
          console.log(`Processing POI with id ${nodeId}, deliveryId: ${deliveryId}`);
          
          if (poi.node) {
            poi.node.deliveryId = deliveryId;
          }

          console.log("POI type:", poi.type, "deliveryId:", deliveryId);
          if (poi.type === "PICKUP" && deliveryId !== -1) {
            console.log("Adding pickup POI with deliveryId:", deliveryId);
            requestMap.set(nodeId, poi);
          }
        });

        // Reset des POIs de la tournée
        Object.entries(data.poiMap).forEach(([id, poi]) => {
          tourPOIMap.set(Number(id), poi);
        });

        console.log("Final requestMap:", requestMap);
        console.log("Updated tourPOIMap:", tourPOIMap);

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
          console.log(
            `élément du type est ${element.type} et id est ${element.deliveryId}`
          );
          const direction = element.type === "pickup" ? "up" : "down";

          const icon = createArrowIcon(color, direction);

          const marker = L.marker([element.latitude, element.longitude], { icon }).addTo(map);
          
          // Stocker les informations du marker pour le hover
          marker.deliveryId = element.deliveryId;
          marker.color = color;
          marker.direction = direction;
          
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
      });
  };

  input.oncancel = () => {
    alert("Veuillez ajouter un fichier XML");
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

  let assignement = generateDeliverersAssignment();
  console.log("Deliverers assignment:", assignement);

  // Diplay the edges tour lines above the existing edges lines
  // Remove previous tour lines
  edgeTourLines.forEach((l) => map.removeLayer(l));
  edgeTourLines = [];

  // Clear control layers content
  for (const layerGroup of delivererLayerGroups.values()) {
    layerGroup.clearLayers();
  }

  // Make separate requests for each deliverer
  for (const [deliverer, poiMap] of Object.entries(assignement)) {
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
    tour: poiMap,  // Map<Long, POI>
  };

  console.log("Computing tour...");

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
      var POIbestSolution = bestSolution;
      var tour = data.solutionPaths;  // Map<String, Map<Long, Long>>
      //var LocalTimebestSolution = bestSolution.map((bs) => bs.time); //List<LocalTime>

      const delivererColor = delivererColors.get(parseInt(deliverer)) || "#000000";

      const layerGroup = delivererLayerGroups.get(parseInt(deliverer));
      if (layerGroup) {
        layerGroup.addTo(map);
      }
      // Draw new tour lines
      for (let i = 0; i < POIbestSolution.length - 1; i++) {
        let fromId = POIbestSolution[i];
        let toId = POIbestSolution[i + 1];
        console.log(`Drawing tour segment from ${fromId} to ${toId}`);
        let subtour = null;
        let key = `(${fromId}, ${toId})`;
        for (el in tour) {
          console.log("Tour element:", el, "  Tour[el]:", tour[el], " Searching for key:", key);
          if (key in tour[el]) {
            subtour = tour[el][key];
          }
        }
        console.log(`Drawing subtour from ${fromId} to ${toId}:`, subtour);
        for (let j = 0; j < Object.keys(subtour).length - 1; j++) {
          let currentId = subtour[j];
          let nextId = subtour[j + 1];
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
                L.polyline(latlngs, { color: delivererColor }).addTo(layerGroup)
              );
            }
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
    })
    .catch((err) => {
      console.error("Error fetching /runTSP:", err);
    });
}

function generateDeliveriesList(
  deliveries,
  numberOfDeliverers = 1,
  pairColors = {}
) {
  const deliveriesListContainer = document.getElementById("deliveries-list");
  deliveriesListContainer.innerHTML = "";

  // Comme deliveries est un Map.values(), on le transforme en tableau
  const deliveriesArray = Array.from(deliveries);

  // Filtrer pour exclure le warehouse (deliveryId === -1)
  const filteredDeliveries = deliveriesArray.filter(
    (delivery) => delivery.node?.deliveryId !== -1
  );

  filteredDeliveries.forEach((delivery, index) => {
    const deliveryItem = document.createElement("div");
    deliveryItem.className = "delivery-item";

    // 🔹 Debug: afficher la structure complète
    console.log("Delivery object:", delivery);
    console.log("Node:", delivery.node);
    console.log("Node deliveryId:", delivery.node?.deliveryId);

    // 🔹 Récupérer la couleur associée - utiliser le deliveryId du node
    const deliveryId = delivery.node?.deliveryId ?? index;
    const color = pairColors[deliveryId] || "#999";

    console.log(
      `Delivery ID: ${deliveryId}, Color: ${pairColors[deliveryId]}, Available colors:`,
      pairColors
    );

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
  
  markers.forEach(marker => {
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
input.addEventListener("input", updateDeliverersList);

function generateDeliverersAssignment() {
  const numberOfDeliverers = getNumberOfDeliverers();

  const assignment = {};
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
