/*jshint esversion:6*/

let campaignerRowTemplate;
let teamTableRowTemplate;
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
                                newCampaignerRow.dataset.campaigner = JSON.stringify(campaigner);

                                $(newCampaignerRow).find("em").text(campaigner.name);
                                $(newCampaignerRow).find("span").text(campaigner.username);

                                if ( teamIds.indexOf(campaigner.id) > -1 ) {
                                    $(newCampaignerRow).find("button").remove();
                                }

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
    let campaigner = JSON.parse(row.dataset.campaigner);
    post( beRoutes.controllers.CampaignMgrCtrl.doAddToTeam(campaignId), campaigner.id );
}

function makeAdmin(userId){
    post( beRoutes.controllers.CampaignMgrCtrl.doMakeAdminInTeam(campaignId), userId );
}

function removeAdmin(userId) {
    let title, text;
    if ( userId === currentUserId ) {
        title = "Make yourself a regular user?";
        text = "You will not be able to undo this action on your own.";
    } else {
        title = "Make " + teamUsernames[userId] + " a regular user?";
        text = "";
    }
    swal({
        title: title,
        text: text,
        icon: "warning",
        buttons: true,
        dangerMode: true,
    }).then((willDelete) => {
        if (willDelete) {
            post(beRoutes.controllers.CampaignMgrCtrl.doRemoveAdminInTeam(campaignId), userId);
        }
    });
}

function removeFromTeam(userId) {
    let title, text;
    if ( userId === currentUserId ) {
        title = "Remove yourself from the team?";
        text = "You will not be able to undo this action on your own.";
    } else {
        title = "Remove user " + teamUsernames[userId] + " from the team?";
        text = "";
    }
    swal({
        title: title,
        text: text,
        icon: "warning",
        buttons: true,
        dangerMode: true,
    }).then((willDelete) => {
        if (willDelete) {
          post(beRoutes.controllers.CampaignMgrCtrl.doRemoveFromTeam(campaignId), userId);
        }
    });
}

function post( action, userId ) {
    let form = document.createElement("form");
    form.setAttribute("method", action.method);
    form.setAttribute("action", action.url);

    let hiddenField = document.createElement("input");
    hiddenField.setAttribute("type", "hidden");
    hiddenField.setAttribute("name", "userId");
    hiddenField.setAttribute("value", userId);
    form.appendChild(hiddenField);

    hiddenField = document.createElement("input");
    hiddenField.setAttribute("type", "hidden");
    hiddenField.setAttribute("name", csrfName);
    hiddenField.setAttribute("value", csrfValue);
    form.appendChild(hiddenField);

    document.body.appendChild(form);
    form.submit();
}