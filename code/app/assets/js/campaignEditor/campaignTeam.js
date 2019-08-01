/*jshint esversion:6*/

let campaignerRowTemplate;
let campaignersList;

function campaignTeamSetup() {
    $("#campaignersListCtnr").hide();
    $("#noResults").hide();
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
                Informationals.makeWarning(polyglot.t("went_wrong"), polyglot.t("try_again"), 1500).show();
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
        title = polyglot.t("team.myself.regular_user.title");
        text = polyglot.t("team.myself.regular_user.text");
    } else {
        title = polyglot.t("team.other_regular_user_title",{name:teamUsernames[userId]});
        text = "";
    }
    swal({
        title: title,
        text: text,
        icon: "warning",
        buttons: {
            cancel:polyglot.t("cancel"),
            confirm:polyglot.t("confirm")
        },
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
        title = polyglot.t("team.myself.remove.title");
        text = polyglot.t("team.myself.remove.text");
    } else {
        title = polyglot.t("team.other_remove_title",{name:teamUsernames[userId]});
        text = "";
    }
    swal({
        title: title,
        text: text,
        icon: "warning",
        buttons: {
            cancel:polyglot.t("cancel"),
            confirm:polyglot.t("confirm")
        },
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