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
var nodeMap = new Map(); // Graphe déjà chargé
var edges_list = []; // Liste des edges déjà chargés
var tourPOIList = []; // POI de la tournée déjà chargés
var edgeTourLines = [];

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

        // Reset des POI de la tournée
        tourPOIList = [];
        data.tours.forEach((poi) => {
          let poiObject = {
            node: poi.node,
            type: poi.type,
            associatedPoI: poi.associatedPoI,
            duration: poi.duration,
          };
          tourPOIList.push(poiObject);
        });

        // Supprime les anciens marqueurs (on veut rafraîchir)
        nodeMarkers.forEach((m) => map.removeLayer(m));
        nodeMarkers = [];

        // map deliveryId -> couleur (persistant pour cette réponse)
        const colorMap = new Map();
        let colorIndex = 0;

        tourPOIList.forEach((element) => {
          // entree de sécurité si les champs manquent
          if (
            typeof element.node.latitude !== "number" ||
            typeof element.node.longitude !== "number"
          ) {
            console.warn("Node missing coords:", element);
            return;
          }

          // entrepôt
          // On suppose que chaque associatedPoI correspond à une paire (pickup/delivery)
          if (element.type === "WAREHOUSE") {
            // entrepôt
            nodeMarkers.push(
              L.marker([element.node.latitude, element.node.longitude], {
                icon: warehouseIcon,
              }).addTo(map)
            );
          } else {
            // Couleur associée à la paire pickup/delivery
            if (!window.pairColors) window.pairColors = {};
            if (!pairColors[element.node.id] && !pairColors[element.associatedPoI]) {
              pairColors[element.associatedPoI] = getRandomColor();
              pairColors[element.node.id] = pairColors[element.associatedPoI];
            }

            const color = pairColors[element.node.id];
            console.log(`élement du type est ${element.type} et id est ${element.node.id}`);
            const direction = element.type === "PICKUP" ? "up" : "down";

            const icon = createArrowIcon(color, direction);

            nodeMarkers.push(
              L.marker([element.node.latitude, element.node.longitude], { icon }).addTo(
                map
              )
            );
          }
        });

        // Générer la liste des livraisons dans le panneau de droite
        generateDeliveriesList(tourPOIList.values(), getNumberOfDeliverers());
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
  console.log("Sending data to compute tour:");
  console.log("All Nodes:", Object.fromEntries(nodeMap));
  console.log("All Edges:", Array.from(edges_list.values()));
  console.log("Tour POIs:", Array.from(tourPOIList));

  // Create body of an object with all needed data
  const requestBody = {
    allNodes: Object.fromEntries(nodeMap),
    allEdges: Array.from(edges_list.values()),
    tour: Array.from(tourPOIList)
  };

  console.log("Computing tour...");

  fetch("http://localhost:8090/runTSP", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(requestBody)
  })
    .then((response) => {
      if (!response.ok) throw new Error("HTTP error " + response.status);
      return response.json();
    })
    .then((data) => {
      console.log("Tour response:", data);
      if (!data.solutionOrder) {
        console.error("No tour in response:", data);
        return;
      }
      var POIbestSolution = data.solutionPaths.map((bs) => bs.id); //List<Long>
      console.log("POIbestSolution:", POIbestSolution);

      // Diplay the edges tour lines above the existing edges lines
      // Remove previous tour lines
      edgeTourLines.forEach((l) => map.removeLayer(l));
      edgeTourLines = [];

      // Draw new tour lines
      for (const trajectory of data.solutionPaths) {
        for (const orig of Object.keys(trajectory)) {
          const dest = trajectory[orig];
          console.log(`Drawing edge from ${orig} to ${dest}`);
          let startNode = nodeMap.get(parseInt(orig));
          let endNode = nodeMap.get(parseInt(dest));
          if (startNode && endNode) {
            let latlngs = [
              [startNode.latitude, startNode.longitude],
              [endNode.latitude, endNode.longitude],
            ];
            edgeTourLines.push(
              L.polyline(latlngs, { color: map_color_from_index(data.solutionPaths.indexOf(trajectory), data.solutionPaths.length) }).addTo(map)
            );
          }
        }
      }
    })
    .catch((err) => {
      console.error("Error fetching /computeTour:", err);
    });
}

function generateDeliveriesList(deliveries, numberOfDeliverers = 1) {
  const deliveriesListContainer = document.getElementById("deliveries-list");
  deliveriesListContainer.innerHTML = "";

  deliveries.forEach((delivery, index) => {
    const deliveryItem = document.createElement("div");
    deliveryItem.className = "delivery-item";

    // Label de la demande
    const label = document.createElement("span");
    label.className = "delivery-label";
    label.textContent = `Demande no. ${index + 1}`;

    const select = document.createElement("select");
    select.className = "delivery-select";
    select.setAttribute("data-delivery-id", delivery.id || index);

    // Ajouter les options de livreurs
    for (let i = 1; i <= numberOfDeliverers; i++) {
      const option = document.createElement("option");
      option.value = i;
      option.textContent = `Livreur ${i}`;
      select.appendChild(option);
    }

    // Assembler l'élément
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
  generateDeliveriesList(tourPOIList.values(), numberOfDeliverers);
}

const input = document.getElementById("numberOfDeliverers");
input.addEventListener("input", updateDeliverersList);

function map_color_from_index(index, total) {
  // Calculate HEX color based on index (gradient from dark purple to light purple)
  const hue = (index / total) * 50; // From 0 to 100
  const saturation = 100;
  const lightness = 50;

  // Convert HSL to HEX
  const [r, g, b] = hslToRgb(hue, saturation, lightness);
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
}

function hslToRgb(h, s, l) {
  s /= 100;
  l /= 100;

  let c = (1 - Math.abs(2 * l - 1)) * s;
  let x = c * (1 - Math.abs(((h / 60) % 2) - 1));
  let m = l - c / 2;
  let r = 0, g = 0, b = 0;

  if (0 <= h && h < 60) {
    r = c; g = x; b = 0;
  } else if (60 <= h && h < 120) {
    r = x; g = c; b = 0;
  } else if (120 <= h && h < 180) {
    r = 0; g = c; b = x;
  } else if (180 <= h && h < 240) {
    r = 0; g = x; b = c; 
  } else if (240 <= h && h < 300) {
    r = x; g = 0; b = c; 
  } else if (300 <= h && h < 360) {
    r = c; g = 0; b = x; 
  }

  r = Math.round((r + m) * 255);
  g = Math.round((g + m) * 255);
  b = Math.round((b + m) * 255);

  return [r, g, b];
}