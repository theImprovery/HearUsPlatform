/*jshint esversion:6*/

let campaignerRowTemplate;
let campaignersList;

function campaignTeamSetup() {
    $("#campaignersListCtnr").hide();
    campaignersList = document.getElementById("campaignersList");

    campaignerRowTemplate = document.getElementById("campaignerTemplate");
    campaignerRowTemplate.parentElement.removeChild( campaignerRowTemplate );
    campaignerRowTemplate.removeAttribute("id");

}

function searchCampaigners(searchStr) {

    $("#noResults").slideUp();

    if(searchStr !== ""){
        new Playjax(beRoutes)
            .using(function (c) {
                return c.CampaignAdminCtrl.getCampaigners(searchStr);
        }).fetch(
        ).then( function (res) {
            if (res.ok) {
                res.json()
                    .then(function (json){
                        if( json.length === 0 ) {
                            $("#noResults").slideDown();
                            $("#campaignersListCtnr").hide();

                        } else {
                            $("#campaignersListCtnr").show();
                            // remove existing rows
                            while ( campaignersList.firstChild ) {
                                campaignersList.removeChild(campaignersList.firstChild);
                            }
                            json.forEach(function (campaigner) {
                                const newCampaignerRow = campaignerRowTemplate.cloneNode(true);
                                console.log(campaigner);
                                newCampaignerRow.dataset.campaigner = JSON.stringify(campaigner);

                                $(newCampaignerRow).find("em").text(campaigner.name);
                                $(newCampaignerRow).find("span").text(campaigner.username);

                                campaignersList.appendChild(newCampaignerRow);
                            });
                        }
                    });
            } else {
                Informationals.makeWarning("Something went wrong", "try again", 1500).show();
            }
        });
    }
}

function addUser( element ) {

    // get the li
    let row = element;
    while ( row.nodeName !== "LI" ){
        row = row.parentElement;
    }
    console.log(JSON.parse(row.dataset.campaigner));
}