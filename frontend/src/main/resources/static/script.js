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
        .then(response => response.json())
        .then(data => {
            console.log('Success:', data);
            // Ici, on peut traiter les données reçues du backend (par exemple, afficher les noeuds et les arêtes sur la carte)
        })
        .catch((error) => {
            console.error('Error:', error);
        });
    };
    input.click();
};
