document.addEventListener("DOMContentLoaded", function () {
  // Code to run when the DOM is fully loaded
  
  // Reset global state on page load
  resetGlobalState();

  // Reset number of deliverers input
  const input = document.getElementById("numberOfDeliverers");
  input.value = 1;
});


// GPS coordinates for the center of the map (here Lyon) at initialization
let lat = 45.764;
let lon = 4.8357;
let edgesVisible = false; // Whether edges are currently visible on the map
let toggleEdgesBtn = null; // Reference to the button for toggling edge visibility
let planLoaded = false; // Whether the map plan has been loaded

// Initialize the map (passing 'map' which is the ID of the DIV containing the map)
let map = L.map("map", {
  zoom: 13,
  center: [lat, lon],
});

// Add the tile layer to display map images (OpenStreetMap France)
L.tileLayer("https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png", {
  minZoom: 1,
  maxZoom: 20,
  attribution:
    'data © <a href="//osm.org/copyright">OpenStreetMap</a>/ODbL - rendering <a href="//openstreetmap.fr">OSM France</a>',
}).addTo(map);

// Icon for regular nodes (circle)
var newIcon = L.icon({
  iconUrl: "circle-icon.png",
  iconSize: [15, 15], // size of the icon
  iconAnchor: [7, 7], // point of the icon which will correspond to marker's location
});

// Icon for the warehouse node
var warehouseIcon = L.icon({
  iconUrl: "warehouse-icon.png",
  iconSize: [20, 20],
  iconAnchor: [10, 10],
});

var nodeMarkers = []; // Array of all node markers currently on the map
var edgeLines = []; // Array of all edge polylines currently on the map
var pairColors = {}; // Map deliveryId -> color for each pickup/delivery pair
var nodeMap = new Map(); // Map of all loaded nodes (the graph)
var edges_list = []; // Array of all loaded edges
var requestMap = new Map(); // Map of all loaded delivery requests (pickup POIs)
var edgeTourLines = []; // Array of polylines for computed tours
var tourPOIMap = new Map(); // Map of all loaded POIs (pickups and deliveries)
var deliveryIdToMarkers = {}; // Map deliveryId -> array of markers for that pair

var activeRequestCounter = 0; // Counter for ongoing tour calculation requests
var requestList = []; // Array of all delivery requests
var delivererList = []; // Array of all deliverers

var delivererETA = {}; // Map delivererId -> ETA string for each deliverer
var numberOfDeliverers = 1; // Number of deliverers (default is 1)
var numberOfRequests = 1; // Number of delivery requests (initially 1)

var delivererLayerGroups = new Map(); // Map delivererId -> Leaflet layer group for each deliverer
var layerControl = null; // Leaflet layer control for toggling deliverer layers
var delivererColors = new Map(); // Map delivererId -> color for each deliverer

var allDeliverersTours = {}; // Map delivererId -> tour data for each deliverer
var assignment = {}; // Global assignment variable for deliverer/request assignments

// Generates a random hexadecimal color (tries to avoid greenish colors)
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
  } while (isGreenish(color) && attempts < maxAttempts); // Retry if color is greenish

  return color;
}

// Resets all global variables related to deliveries, tours, and deliverers
function resetGlobalState() {
  // Clear all delivery and tour related maps and arrays
  requestMap.clear();           // Clear the map of pickup POIs
  tourPOIMap.clear();           // Clear the map of all POIs (pickup + delivery)
  deliveryIdToMarkers = {};     // Reset the map of markers by deliveryId
  pairColors = {};              // Reset the color mapping for each delivery pair
  edgeTourLines = [];           // Remove all tour polylines
  numberOfDeliverers = 1;       // Reset number of deliverers to default
  numberOfRequests = 0;         // Reset number of requests
  delivererETA = {};            // Clear ETA mapping for deliverers
  allDeliverersTours = {};      // Clear all deliverers' tours
  delivererList = [];           // Clear the list of deliverers

  // Clear all layer groups for deliverers
  for (const layerGroup of delivererLayerGroups.values()) {
    layerGroup.clearLayers();
  }
  delivererLayerGroups.clear();

  // Remove the layer control from the map if it exists
  if (layerControl) {
    map.removeControl(layerControl);
    layerControl = null;
  }

  delivererColors.clear();      // Clear color mapping for deliverers
}

// Checks if a color is "greenish" (to avoid green colors on the map)
function isGreenish(hexColor) {
  // Convert HEX color string to RGB values
  const r = parseInt(hexColor.substr(1, 2), 16); // Red component
  const g = parseInt(hexColor.substr(3, 2), 16); // Green component
  const b = parseInt(hexColor.substr(5, 2), 16); // Blue component

  // Return true if green is dominant and strong (to avoid greenish colors)
  // Green is dominant if G > R and G > B, and strong if G > 100
  return g > r && g > b && g > 100;
}

// Creates an arrow icon pointing up or down with the given color
function createArrowIcon(color, direction, size = 32, increased = false) {
  // Set rotation for the arrow: down = 180°, up = 0°
  const rotation =
    direction === "down" ? "rotate(180 12 12)" : "rotate(0 12 12)";

  // Return a Leaflet divIcon with an SVG arrow
  return L.divIcon({
    className: "",
    html: `
            <svg class="${ increased ? "highlighted-arrow-icon" : "arrow-icon" }" xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}"
                 viewBox="0 0 24 24">
                <g transform="${rotation}">
                    <path fill="${color}" d="M12 2L5 9h4v9h6V9h4z"/>
                </g>
            </svg>
        `,
    iconSize: [size, size], // Size of the icon
    iconAnchor: [size / 2, size / 2], // Anchor point of the icon
  });
}

// Creates LayerGroups for each deliverer based on assignment
function createDelivererLayerGroups() {
  // Create a layer group for each deliverer if not already present
  if (delivererLayerGroups.size === 0) {
    delivererColors.forEach((color, delivererId) => {
      if (delivererId > getNumberOfDeliverers()) return; // Skip unused deliverers
      delivererLayerGroups.set(delivererId, L.layerGroup());
    });
  } else if (delivererLayerGroups.size < getNumberOfDeliverers()) {
    // Add missing layer groups if the number of deliverers increased
    for (let i = delivererLayerGroups.size + 1; i <= getNumberOfDeliverers(); i++) {
      delivererLayerGroups.set(i, L.layerGroup());
    }
  }

  // Assign markers to the correct deliverer layer group based on selection
  const selects = document.querySelectorAll(".delivery-select");

  selects.forEach((select) => {
    const deliveryId = parseInt(select.getAttribute("data-delivery-id"));
    const selectedDeliverer = parseInt(select.value);

    // Get markers for this deliveryId
    const markers = deliveryIdToMarkers[deliveryId];

    if (markers && delivererLayerGroups.has(selectedDeliverer)) {
      const layerGroup = delivererLayerGroups.get(selectedDeliverer);
      markers.forEach((marker) => {
        // Remove marker from the main map
        map.removeLayer(marker);
        // Add marker to the deliverer's layer group
        layerGroup.addLayer(marker);
      });
    }
  });

  // Add all layer groups to the map (only those actively used)
  delivererLayerGroups.forEach((layerGroup) => {
    // Only add layer groups for active deliverers
    const delivererId = Array.from(delivererLayerGroups.entries()).find(
      ([id, lg]) => lg === layerGroup
    )[0];
    if (delivererId > getNumberOfDeliverers()) {
      // Clear markers from unused layer groups
      layerGroup.clearLayers();
      return;
    }
    // Add the layer group to the map if not already present
    if (!map.hasLayer(layerGroup)) {
      layerGroup.addTo(map);
    }
  });
}

// Updates the Leaflet layer control for deliverers
function updateLayerControl() {
  // Remove the old layer control if it exists
  if (layerControl) {
    map.removeControl(layerControl);
  }

  // Create the overlays object for the control
  const overlayMaps = {};

  // Add each deliverer's layer group to the overlays with a colored label
  delivererColors.forEach((color, delivererId) => {
    const layerGroup = delivererLayerGroups.get(delivererId);
    if (layerGroup) {
      // Use HTML to show the color and ETA in the label
      overlayMaps[`
        <span style="display: inline-flex; align-items: center;">
          <span style="width: 12px; height: 12px; background-color: ${color}; border-radius: 50%; display: inline-block; margin-right: 8px; border: 1px solid #ccc;"></span>
          ${delivererId in delivererETA ? `Deliverer ${delivererId} - ETA ${delivererETA[delivererId]}` : `Deliverer ${delivererId} - ETA --:--`}
        </span>`
      ] = layerGroup;
    }
  });

  // Create and add the new layer control to the map
  layerControl = L.control
    .layers(null, overlayMaps, {
      collapsed: false, // Always open
      position: "topright",
    })
    .addTo(map);
}

// Main function to update the display for deliverers
function updateDelivererDisplay() {
  createDelivererLayerGroups();
  updateLayerControl();
}

// Generate colors for each deliverer
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

// Loads the map based on the selected XML file
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

        // Remove old markers and edges from the map
        nodeMarkers.forEach((m) => map.removeLayer(m));
        nodeMarkers = [];
        edgeLines.forEach((l) => map.removeLayer(l));
        edgeLines = [];

        // Reset the global graph
        nodeMap.clear();
        nodes.forEach((node) => nodeMap.set(node.id, node));

        // Reset the list of edges
        edges_list = edges;

        // Reset the global state related to deliveries
        resetGlobalState();

        // Variables to calculate map bounds
        let topLeftNode = null;
        let bottomRightNode = null;

        // Find the top-left and bottom-right nodes for map bounds
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

        // Add edges to the edgeLines array (not displayed yet)
        edges.forEach((edge) => {
          let startNode = nodeMap.get(edge.originId);
          let endNode = nodeMap.get(edge.destinationId);
          if (startNode && endNode) {
            let latlngs = [
              [startNode.latitude, startNode.longitude],
              [endNode.latitude, endNode.longitude],
            ];
            // Add the edge but don't display it yet
            let line = L.polyline(latlngs, {
              color: "#50d76b",
              opacity: 0.5
            });
            edgeLines.push(line);
          }
        });

        // Adjust the map zoom to fit all nodes
        if (topLeftNode && bottomRightNode) {
          let bounds = L.latLngBounds(
            [bottomRightNode.latitude, topLeftNode.longitude],
            [topLeftNode.latitude, bottomRightNode.longitude]
          );
          map.flyToBounds(bounds, { duration: 2.0 });
          // Enable the "Afficher le plan" button
          toggleEdgesBtn = document.getElementById("toggleEdgesBtn");
          toggleEdgesBtn.style.display = "inline-block";
          toggleEdgesBtn.textContent = "Afficher le plan";
          edgesVisible = false;
          planLoaded = true;

          // Change the color of the "Charger un plan" button
          const planButton = document.querySelector(
            ".buttons button:nth-child(1)"
          );
          planButton.style.backgroundColor = "var(--primary-green)";
          planButton.style.color = "white";
        }

        // Simulate clicking the confirm deliverer button to reset deliverer count
        document.getElementById("confirmBtn").click();

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

// Loads the delivery requests based on the selected XML file
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

        // Create a map to find deliveryId from node id
        const nodeIdToDeliveryId = new Map();
        data.nodes.forEach((node) => {
          nodeIdToDeliveryId.set(node.id, node.deliveryId);
        });

        // Reset global state related to deliveries
        resetGlobalState();

        Object.entries(data.poiMap).forEach(([id, poi]) => {
          // Add deliveryId to node in POI
          const nodeId = Number(id);
          const deliveryId = nodeIdToDeliveryId.get(nodeId);

          if (poi.node) {
            poi.node.deliveryId = deliveryId;
          }

          if (poi.type === "PICKUP" && deliveryId !== -1) {
            requestMap.set(nodeId, poi);
          }
        });

        // Reset POIs for the tour
        Object.entries(data.poiMap).forEach(([id, poi]) => {
          tourPOIMap.set(Number(id), poi);
        });

        updateNumberOfRequests();

        // Remove old markers (refresh)
        nodeMarkers.forEach((m) => map.removeLayer(m));
        nodeMarkers = [];
        deliveryIdToMarkers = {}; // Reset marker map by deliveryId

        // Map deliveryId -> color (persistent for this response)
        const colorMap = new Map();
        let colorIndex = 0;

        data.nodes.forEach((element) => {
          // Safety check if fields are missing
          if (
            typeof element.latitude !== "number" ||
            typeof element.longitude !== "number"
          ) {
            console.warn("Node missing coords:", element);
            return;
          }

          // Warehouse
          if (element.deliveryId === -1) {
            nodeMarkers.push(
              L.marker([element.latitude, element.longitude], {
                icon: warehouseIcon,
              }).addTo(map)
            );
            return;
          }

          // Color associated with pickup/delivery pair
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

          // Store marker info for hover
          marker.deliveryId = element.deliveryId;
          marker.color = color;
          marker.direction = direction;
          marker.type = element.type;
          marker.nodeId = element.id;

          nodeMarkers.push(marker);

          // Group markers by deliveryId
          if (!deliveryIdToMarkers[element.deliveryId]) {
            deliveryIdToMarkers[element.deliveryId] = [];
          }
          deliveryIdToMarkers[element.deliveryId].push(marker);
        });

        // Generate the deliveries list in the right panel
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

// Toggles the visibility of edges on the map
function toggleEdges() {
  if (!planLoaded) {
    alert("Veuillez d'abord charger un plan.");
    return;
  }

  if (edgesVisible) {
    // Hide edges
    edgeLines.forEach((l) => map.removeLayer(l));
    toggleEdgesBtn.textContent = "Afficher le plan";
    toggleEdgesBtn.classList.remove("active");
  } else {
    // Show edges
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

// Main function to compute the tours for all deliverers
function compute_tour() {
  if (!nodeMap || nodeMap.size === 0) {
    alert("Veuillez d'abord importer un plan avant de calculer une tournée.");
    return;
  }

  blockButtons();
  activeRequestCounter = 0;

  assignment = generateDeliverersAssignment();

  if (Object.keys(assignment).length === 0) {
    unblockButtons();
    return;
  }

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
              L.polyline(latlngs, {
                color: delivererColor,
                weight: 5,
              }).addTo(layerGroup)
            );
            L.polylineDecorator(edgeTourLines[edgeTourLines.length - 1], {
              patterns: [
                { offset: '50%', repeat: 0, symbol: L.Symbol.arrowHead({ pixelSize: 10, polygon: false, pathOptions: { stroke: true, color: delivererColor } }) }
              ]
            }).addTo(layerGroup);
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

// Generates the deliveries list in the right panel
function generateDeliveriesList( deliveries, numberOfDeliverers = 1, pairColors = {}) {
  const deliveriesListContainer = document.getElementById("deliveries-list");
  deliveriesListContainer.innerHTML = "";

  // Convert deliveries Map.values() to array
  const deliveriesArray = Array.from(deliveries);

  // Filter out the deliveries with deliveryId -1 (warehouse)
  // Filter to exclude warehouse (deliveryId === -1) and undefined/null entries
  const filteredDeliveries = deliveriesArray.filter(
    (delivery) => delivery.node?.deliveryId !== -1 && delivery.node?.deliveryId != null && delivery.node !== undefined
  );

  filteredDeliveries.forEach((delivery, index) => {
    const deliveryItem = document.createElement("div");
    deliveryItem.className = "delivery-item";

    // Get the associated color - use the node's deliveryId
    const deliveryId = delivery.node?.deliveryId ?? index;
    const color = pairColors[deliveryId] || "#999";
    if (deliveryId == null) {
      console.warn("Skipping POI without deliveryId:", delivery);
      return; // skip this entry
    }

    // Create the colored dot
    const colorDot = document.createElement("span");
    colorDot.className = "color-dot";
    colorDot.style.backgroundColor = color;

    // Label for the request
    const label = document.createElement("span");
    label.className = "delivery-label";
    label.textContent = `Demande no. ${index + 1}`;

    // Deliverer selector
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

    // Add hover events
    deliveryItem.addEventListener("mouseenter", () => {
      highlightMarkers(deliveryId, true);
    });

    deliveryItem.addEventListener("mouseleave", () => {
      highlightMarkers(deliveryId, false);
    });

    // Create the delete button (red X) that calls deletePOIByDeliveryId(pairId)
    const deleteBtn = document.createElement("button");
    deleteBtn.className = "delete-btn user-action-button";
    deleteBtn.type = "button";
    deleteBtn.title = "Supprimer";
    deleteBtn.textContent = "×";
    // Call the existing function inline to follow your simple model
    deleteBtn.setAttribute("onclick", `deletePOIByDeliveryId(${deliveryId})`);

    // Assemble the elements
    deliveryItem.appendChild(colorDot);
    deliveryItem.appendChild(label);
    deliveryItem.appendChild(select);
    deliveryItem.appendChild(deleteBtn);

    deliveriesListContainer.appendChild(deliveryItem);
  });
}

// Function to enlarge/reduce markers for a deliveryId
function highlightMarkers(deliveryId, highlight) {
  const markers = deliveryIdToMarkers[deliveryId];
  if (!markers) return;

  const size = highlight ? 48 : 32; // Enlarged or normal size

  markers.forEach((marker) => {
    const newIcon = createArrowIcon(marker.color, marker.direction, size, highlight);
    marker.setIcon(newIcon);
  });
}

// Retrieves the current number of deliverers from the input field
function getNumberOfDeliverers() {
  const input = document.getElementById("numberOfDeliverers");
  return input ? parseInt(input.value) || 1 : 1; // Default value: 1
}

// Updates the deliverers list and display based on the current number of deliverers
function updateDeliverersList() {
  const numberOfDeliverers = getNumberOfDeliverers();
  generateDelivererColors(numberOfDeliverers);
  // Update the legend
  updateDelivererDisplay();
  generateDeliveriesList(requestMap.values(), numberOfDeliverers, pairColors);
}

// Event listener for changes in the number of deliverers inputhh
const input = document.getElementById("numberOfDeliverers");
input.addEventListener("change", updateDeliverersList);

// Generates the assignment of POIs to deliverers based on selections
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

  if (!warehousePOI) {
    alert ("Aucun entrepôt trouvé dans les POIs de la tournée. Veuillez soumettre un fichier de demande valide.");
    return assignment;
  }

  for (let i = 1; i <= numberOfDeliverers; i++) {
    assignment[i][warehousePOI.node.id] = warehousePOI;
  }

  selects.forEach((select) => {
    const deliveryId = parseInt(select.getAttribute("data-delivery-id"));
    const selectedDeliverer = parseInt(select.value);

    // Get the corresponding pickup POI in tourPOIMap
    const pickupPOI = tourPOIMap.get(deliveryId);

    let deliveryPOI = null;
    tourPOIMap.forEach((poi) => {
      if (poi.associatedPoI == deliveryId) {
        deliveryPOI = poi;
      }
    });

    // If there are no nodes, alert and skip
    if (!deliveryPOI) {
      console.warn("No delivery POI found for deliveryId. Node could have been deleted using deleteByID", deliveryId);
      return;
    }
    // Add POIs to the selected deliverer
    assignment[selectedDeliverer][deliveryId] = pickupPOI;
    assignment[selectedDeliverer][deliveryPOI.node.id] = deliveryPOI;
  });

  return assignment;
}

// Get references to the buttons
const plusBtn = document.getElementById("plusBtn");
const minusBtn = document.getElementById("minusBtn");
const confirmBtn = document.getElementById("confirmBtn");

// Event listeners for the plus, minus, and confirm buttons
plusBtn.addEventListener("click", () => {
  let current = parseInt(numberOfDeliverers);
  if (current < numberOfRequests) {
    numberOfDeliverers = current + 1;
    document.getElementById("numberOfDeliverers").value = numberOfDeliverers;
  }
});

// Decrease the number of deliverers, minimum 1
minusBtn.addEventListener("click", () => {
  let current = parseInt(numberOfDeliverers);
  if (current > 1) {
    numberOfDeliverers = current - 1;
    document.getElementById("numberOfDeliverers").value = numberOfDeliverers;
  }
});

// Confirm button to update the deliverers list
confirmBtn.addEventListener("click", () => {
  updateDeliverersList();
});

// Updates the number of requests based on the requestMap size
function updateNumberOfRequests() {
  numberOfRequests = requestMap.size;
}

// Function to export tours to JSON with all necessary data
function exportToursToJSON() {
  // Check that there are tours to export
  if (Object.keys(allDeliverersTours).length === 0) {
    alert("Aucune tournée à exporter. Veuillez d'abord calculer les tournées.");
    return;
  }

  // Check that the map and POIs are loaded
  if (!nodeMap || nodeMap.size === 0) {
    alert("Le plan n'est pas chargé. Impossible d'exporter.");
    return;
  }

  if (!tourPOIMap || tourPOIMap.size === 0) {
    alert("Les points de pickup/delivery ne sont pas chargés. Impossible d'exporter.");
    return;
  }

  // Create the complete JSON object with all information
  const exportData = {
    exportDate: new Date().toISOString(),
    version: "2.0",
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
    deliverers: {
      numberOfDeliverers: getNumberOfDeliverers(),
      colors: Array.from(delivererColors.entries()).map(([id, color]) => ({
        delivererId: id,
        color: color
      })),
      tours: Object.entries(allDeliverersTours).map(([delivererId, tourData]) => {
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
          exactPaths: tourLines
        };
      }),
      assignments: assignment,
      delivererETA: delivererETA
    }
  };

  // Convert to JSON with indentation for readability
  const jsonString = JSON.stringify(exportData, null, 2);

  // Create a blob and download the file
  const blob = new Blob([jsonString], { type: "application/json" });
  const url = URL.createObjectURL(blob);

  // Create a temporary download link
  const link = document.createElement("a");
  link.href = url;
  link.download = `tournee_complete_${new Date().toISOString().split('T')[0]}.json`;

  // Trigger the download
  document.body.appendChild(link);
  link.click();

  // Clean up
  document.body.removeChild(link);
  URL.revokeObjectURL(url);

  alert("Tournée complète exportée avec succès !");
}

// Function to import a complete tour from JSON (with map, pickups/deliveries, and paths)
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

        // Check file format
        if (!importedData.map || !importedData.deliveries || !importedData.deliverers) {
          alert("Format JSON invalide : données manquantes (map, deliveries ou deliverers)");
          unblockButtons();
          return;
        }

        // ===== 1. LOAD THE MAP (NODES AND EDGES) =====
        // Remove old markers and edges from the map
        nodeMarkers.forEach((m) => map.removeLayer(m));
        nodeMarkers = [];
        edgeLines.forEach((l) => map.removeLayer(l));
        edgeLines = [];

        // Load nodes
        nodeMap.clear();
        importedData.map.nodes.forEach(node => {
          nodeMap.set(node.id, {
            id: node.id,
            latitude: node.latitude,
            longitude: node.longitude
          });
        });

        // Load edges
        edges_list = importedData.map.edges.map(edge => ({
          originId: edge.originId,
          destinationId: edge.destinationId,
          length: edge.length
        }));

        // Create polylines for edges (not displayed by default)
        importedData.map.edges.forEach(edge => {
          const startNode = nodeMap.get(edge.originId);
          const endNode = nodeMap.get(edge.destinationId);
          if (startNode && endNode) {
            const latlngs = [
              [startNode.latitude, startNode.longitude],
              [endNode.latitude, endNode.longitude]
            ];
            const line = L.polyline(latlngs, { 
              color: "#50d76b",
              opacity: 0.5
            });
            edgeLines.push(line);
          }
        });

        // Adjust view on the map
        if (importedData.map.nodes.length > 0) {
          const lats = importedData.map.nodes.map(n => n.latitude);
          const lngs = importedData.map.nodes.map(n => n.longitude);
          const bounds = L.latLngBounds(
            [Math.min(...lats), Math.min(...lngs)],
            [Math.max(...lats), Math.max(...lngs)]
          );
          map.flyToBounds(bounds, { duration: 2.0 });
        }

        // Enable the "Afficher le plan" button
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

        // ===== 2. LOAD PICKUPS AND DELIVERIES =====

        // Load POIs
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

          // Add deliveryId to node if available
          if (poiObj.node && poi.deliveryId !== undefined) {
            poiObj.node.deliveryId = poi.deliveryId;
          }

          tourPOIMap.set(poi.id, poiObj);

          // Add to requests if it's a pickup (except warehouse)
          if (poi.type === "PICKUP" && poi.deliveryId !== -1) {
            requestMap.set(poi.id, poiObj);
          }
        });

        updateNumberOfRequests();

        // Create markers for pickups/deliveries
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

          // Pickup or Delivery
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

        // ===== 3. CONFIGURE DELIVERERS =====
        const importedNumberOfDeliverers = importedData.deliverers.numberOfDeliverers;
        numberOfDeliverers = importedNumberOfDeliverers;

        // Update the number of deliverers
        const deliverersInput = document.getElementById("numberOfDeliverers");
        if (deliverersInput) {
          deliverersInput.value = importedNumberOfDeliverers;
        }

        // Load deliverer colors
        delivererColors.clear();
        importedData.deliverers.colors.forEach(({ delivererId, color }) => {
          delivererColors.set(delivererId, color);
        });

        // Generate the deliveries list with correct assignments
        generateDeliveriesList(requestMap.values(), importedNumberOfDeliverers, pairColors);

        // Apply assignments from JSON
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

        // Create layer groups for deliverers
        generateDelivererColors(importedNumberOfDeliverers);
        updateDelivererDisplay();

        // ===== 4. LOAD TOURS WITH EXACT PATHS =====
        // Remove old tours
        edgeTourLines.forEach((l) => map.removeLayer(l));
        edgeTourLines = [];

        for (const layerGroup of delivererLayerGroups.values()) {
          layerGroup.clearLayers();
        }

        // Rebuild allDeliverersTours
        allDeliverersTours = {};
        importedData.deliverers.tours.forEach(tourData => {
          allDeliverersTours[tourData.delivererId] = {
            delivererId: tourData.delivererId,
            tourOrder: tourData.tourOrder,
            tourDetails: tourData.tourDetails,
            color: tourData.color
          };
        });

        // Draw exact path segments
        importedData.deliverers.tours.forEach(tourData => {
          const delivererIdInt = tourData.delivererId;
          const delivererColor = tourData.color || delivererColors.get(delivererIdInt) || "#000000";
          const layerGroup = delivererLayerGroups.get(delivererIdInt);

          if (!layerGroup) {
            console.error(`No layer group found for deliverer ${delivererIdInt}`);
            return;
          }

          layerGroup.addTo(map);

          // Draw all exact path segments
          if (tourData.exactPaths && tourData.exactPaths.length > 0) {
            tourData.exactPaths.forEach(pathSegment => {
              const latlngs = [
                [pathSegment.from.lat, pathSegment.from.lng],
                [pathSegment.to.lat, pathSegment.to.lng]
              ];

              const line = L.polyline(latlngs, {
                color: delivererColor,
                weight: 5
              }).addTo(layerGroup);
              L.polylineDecorator(line, {
                patterns: [
                  { offset: '50%', repeat: 0, symbol: L.Symbol.arrowHead({ pixelSize: 10, polygon: false, pathOptions: { stroke: true, color: delivererColor } }) }
                ]
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

// Geocoding API interface
function geocodingAPIRequest(address) {
  if (!address || !address.trim()) return null;
  const encoded = encodeURIComponent(address.trim());

  // Add an email param for the public Nominatim service (replace with your email or app contact)
  const url = `https://data.geopf.fr/geocodage/search?q=${encoded}`;
  return fetch(url, {
    method: "GET",
    headers: {
      "Accept": "application/json"
    }
  }).then(res => {
    if (!res.ok) {
      console.warn("Returned HTTP: ", res.status);
      return null;
    }
    return res.json();
  }).catch(err => {
    console.error("Geocoding error", err);
    return null;
  });
}

// Function to retrieve a list of matching addresses for autocomplete
function getAddressSuggestions(query) {
  let json = geocodingAPIRequest(query);
  return json.then(json => {
    if (!json || !json.features || json.features.length === 0) {
      console.warn("No suggestions for query:", query);
      return [];
    }
    const suggestions = json.features.map(feature => feature.properties.label);
    return suggestions;
  }).catch(err => {
    console.error("Geocoding error", err);
    return [];
  });
}

// Function to get coordinates from an address using Geocoding API
function getCoordinatesFromAddress(address) {
  let json = geocodingAPIRequest(address);
  return json.then(json => {
    console.log("Geocoding result for address", address, ":", json);
    if (!json || !json.features || json.features.length === 0) {
      console.warn("No results for address:", address);
      return null;
    }
    const best = json.features[0].geometry.coordinates;
    return { lat : parseFloat(best[1]), lon: parseFloat(best[0]) };
  }).catch(err => {
    console.error("Geocoding error", err);
    return null;
  });
}

let debounceTimer = null;
const DELAY_MS = 500;
// Event listeners for input fields with debouncing for API calls
document.getElementById("inputPickup").addEventListener("input", () => {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
  getAddressSuggestions(document.getElementById("inputPickup").value)
      .then(addresses => {
        // Add the options to the suggestions list
        const dataList = document.getElementById("suggestions-adresse");
        dataList.innerHTML = "";
        if (addresses) {
          for (const address of addresses) {
            const option = document.createElement("option");
            option.className = "suggestion-option";
            option.value = address;
            option.textContent = address;
            option.addEventListener("click", () => {
              document.getElementById("inputPickup").value = address;
              // Empty the suggestions list
              dataList.innerHTML = "";
            });
            dataList.appendChild(option);
          }
        }
      });
  }, DELAY_MS);
});
document.getElementById("inputDelivery").addEventListener("input", () => {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    getAddressSuggestions(document.getElementById("inputDelivery").value)
      .then(addresses => {
        // Add the options to the suggestions list
        const dataList = document.getElementById("suggestions-adresse");
        dataList.innerHTML = "";
        if (addresses) {
          for (const address of addresses) {
            const option = document.createElement("option");
            option.className = "suggestion-option";
            option.value = address;
            option.textContent = address;
            option.addEventListener("click", () => {
              document.getElementById("inputDelivery").value = address;
              // Empty the suggestions list
              dataList.innerHTML = "";
            });
            dataList.appendChild(option);
          }
        }
      });
  }, DELAY_MS);
})

// Function to clear the suggestions list when losing focus
function clearSuggestionsOnBlur() {
  setTimeout(() => {
    const dataList = document.getElementById("suggestions-adresse");
    dataList.innerHTML = "";
  }, 200);
}
document.getElementById("inputPickup").addEventListener("blur", clearSuggestionsOnBlur);
document.getElementById("inputDelivery").addEventListener("blur", clearSuggestionsOnBlur);

// Function to get node IDs for pickup and delivery addresses
async function getNodeIdsByNames(pickupName, deliveryName) {
  let pickupId = null;
  let deliveryId = null;

  const coords1 = await getCoordinatesFromAddress(pickupName);     // {lat, lon} or null
  const coords2 = await getCoordinatesFromAddress(deliveryName);
  if (!coords1 || !coords2) {
    // handle not found (return nulls, throw, or inform UI)
    return [pickupId, deliveryId];
  }
  let minDistPickup = Infinity;
  let minDistDelivery = Infinity;

  nodeMap.forEach((node, id) => {
    const distToPickup = Math.sqrt(
      Math.pow(node.latitude - coords1.lat, 2) +
      Math.pow(node.longitude - coords1.lon, 2)
    );
    const distToDelivery = Math.sqrt(
      Math.pow(node.latitude - coords2.lat, 2) +
      Math.pow(node.longitude - coords2.lon, 2)
    );
    if (distToPickup < minDistPickup) {
      minDistPickup = distToPickup;
      pickupId = id;
    }
    if (distToDelivery < minDistDelivery) {
      minDistDelivery = distToDelivery;
      deliveryId = id;
    }
  });
  const MAXDISTANCE = 0.001; // approx ~100m (in degrees)
  if (minDistPickup >= MAXDISTANCE || minDistDelivery >= MAXDISTANCE) {
    alert("Les points de pickup ou delivery sont trop éloignés de la carte actuelle. Veuillez essayer une autre adresse.");
    return [null, null];
  }
  return [pickupId, deliveryId];
}

// Function to add a new POI (pickup + delivery) to the map and data structures
async function addPOI() {
  if(!planLoaded) {
    alert("Good morning client (Killian). You'll have to try harder than that to break our code. Please load the map first.");
    return false;
  }

  const pickupName = document.getElementById("inputPickup").value;
  const deliveryName = document.getElementById("inputDelivery").value;

  const [pickupId, deliveryId] = await getNodeIdsByNames(pickupName, deliveryName);

  if (!pickupId || !deliveryId) {
    console.warn("Could not find node(s) for addresses", pickupName, deliveryName, pickupId, deliveryId);
    return false;
  }

  if (pickupId === deliveryId) {
    alert("Les points de pickup et delivery correspondent au même noeud. Choisir une autre adresse.");
    return false;
  }

  const pickupNode = nodeMap.get(pickupId);
  const deliveryNode = nodeMap.get(deliveryId);

  let lastPairId = 0;
  for (let [, poi] of tourPOIMap) {
    if (poi.node.deliveryId && poi.node.deliveryId > lastPairId) {
      lastPairId = poi.node.deliveryId;    }
  }

  let pairId = lastPairId + 1;
  pickupNode.deliveryId = pairId;
  deliveryNode.deliveryId = pairId;

  const pickupPOI = { type: "PICKUP", node: pickupNode, associatedPoI: deliveryId, duration: null };
  const deliveryPOI = { type: "DELIVERY", node: deliveryNode, associatedPoI: pickupId, duration: null };

  // requestMap contains PICKUP entries (like load_xml_delivery)
  requestMap.set(pickupId, pickupPOI);

  // tourPOIMap must contain all POIs (pickup + delivery) for compute_tour
  tourPOIMap.set(pickupId, pickupPOI);
  tourPOIMap.set(deliveryId, deliveryPOI);

  if (!pairColors[pairId]) {
    pairColors[pairId] = getRandomColor();
  } else{
    console.warn("Pair color already exists for pairId. Verify if pairID is unique", pairId, ":", pairColors[pairId]);
  }
  const color = pairColors[pairId];

  // Create and add markers to the map (pickup = up, delivery = down)
  const pickupIcon = createArrowIcon(color, "up");
  const deliveryIcon = createArrowIcon(color, "down");

  const pickupMarker = L.marker([pickupNode.latitude, pickupNode.longitude], { icon: pickupIcon }).addTo(map);
  pickupMarker.deliveryId = pairId;
  pickupMarker.color = color;
  pickupMarker.direction = "up";

  const deliveryMarker = L.marker([deliveryNode.latitude, deliveryNode.longitude], { icon: deliveryIcon }).addTo(map);
  deliveryMarker.deliveryId = pairId;
  deliveryMarker.color = color;
  deliveryMarker.direction = "down";

  // Store markers for hover/highlight/later deletion management
  nodeMarkers.push(pickupMarker, deliveryMarker);
  if (!deliveryIdToMarkers[pairId]) deliveryIdToMarkers[pairId] = [];
  deliveryIdToMarkers[pairId].push(pickupMarker, deliveryMarker);

  // Update the UI
  generateDeliveriesList(requestMap.values(), getNumberOfDeliverers(), pairColors);
  generateDelivererColors(getNumberOfDeliverers());
  updateDeliverersList();

  // Optional: clear inputs after adding
  document.getElementById("inputPickup").value = "";
  document.getElementById("inputDelivery").value = "";

  return true;
}

// Function to delete a POI pair by deliveryId

async function deletePOIByDeliveryId(delId) {
    if (!planLoaded) {
        alert("Veuillez charger une carte d'abord.");
        return false;
    }

    // Normalize id and check validity
    const parsedId = Number(delId);
    if (!Number.isInteger(parsedId) || isNaN(parsedId)) {
      console.warn("deletePOIByDeliveryId called with invalid id:", delId);
      return false;
    }
    
    // Remove corresponding divs in the list immediately (instant UX)
    try {
      const selector = `.delivery-item[deliveryID="${parsedId}"]`;
      const items = document.querySelectorAll(selector);
      if (items && items.length > 0) {
        items.forEach(it => it.remove());
        console.log(`Removed ${items.length} delivery-item DOM element(s) for deliveryID ${parsedId}`);
      } else {
        // fallback: try lowercase attribute if generated differently
        const altItems = document.querySelectorAll(`.delivery-item[deliveryId="${parsedId}"]`);
        if (altItems && altItems.length > 0) {
          altItems.forEach(it => it.remove());
          console.log(`Removed ${altItems.length} delivery-item DOM element(s) for deliveryId ${parsedId}`);
        }
      }
    } catch (e) {
      console.warn("Error removing delivery-item DOM elements:", e);
    }

    // Remove markers from map
    if (deliveryIdToMarkers[delId]) {
        deliveryIdToMarkers[delId].forEach((m) => {
            try { map.removeLayer(m); } catch(e) {}
        });
        delete deliveryIdToMarkers[delId];
        nodeMarkers = nodeMarkers.filter((m) => m.deliveryId !== delId);
    }

    // Check that there is a corresponding pair to delete
    let exists = false;
    if (deliveryIdToMarkers[parsedId] && deliveryIdToMarkers[parsedId].length > 0) {
      exists = true;
    } else {
      for (let [, poi] of tourPOIMap) {
        if (poi.node && poi.node.deliveryId === parsedId) {
          exists = true;
          break;
        }
      }
    }
    if (!exists) {
      console.warn(`No POI pair found for deliveryId ${parsedId} — aborting deletion.`);
      return false;
    }

    // Remove POIs from tourPOIMap and clear deliveryId on nodes
    const tourKeysToDelete = [];
    for (let [nodeId, poi] of tourPOIMap) {
        if (poi.node && poi.node.deliveryId === delId) {
            if (poi.node) poi.node.deliveryId = undefined;
            tourKeysToDelete.push(nodeId);
        }
    }
    tourKeysToDelete.forEach((k) => tourPOIMap.delete(k));

    // Remove pickups from requestMap
    const reqKeysToDelete = [];
    for (let [nodeId, poi] of requestMap) {
        if (poi.node && poi.node.deliveryId === delId) {
            reqKeysToDelete.push(nodeId);
        }
    }
    reqKeysToDelete.forEach((k) => requestMap.delete(k));

    // Remove color
    if (pairColors[delId]){
      delete pairColors[delId];
      console.log(`Deleted POI pair ${delId}`);
    } 

    // Update UI (regenerate list) and re-add delete buttons
    generateDeliveriesList(requestMap.values(), getNumberOfDeliverers(), pairColors);
    // regenerate colors/legend if needed
    generateDelivererColors(getNumberOfDeliverers());

    return true;
}