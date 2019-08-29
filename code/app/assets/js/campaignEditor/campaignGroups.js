/*jshint esversion:6*/

let groupsList;
let groupRowTemplate;
$(document).ready(function(){
    $("#groupsListCtnr").hide();
    $("#noResults").hide();
    groupsList = document.getElementById("groupsList");

    groupRowTemplate = document.getElementById("groupTemplate");
    groupRowTemplate.parentElement.removeChild( groupRowTemplate );
    groupRowTemplate.removeAttribute("id");
});

function searchGroups(searchStr) {
    $("#noResults").slideUp();
    if(searchStr !== ""){
        new Playjax(beRoutes)
            .using(function (c) {
                return c.CampaignMgrCtrl.getGroups(searchStr);
            }).fetch(
        ).then( function (res) {
            if (res.ok) {
                res.json()
                    .then(function (json){
                        if( json.length === 0 ) {
                            $("#noResults").slideDown();
                            $("#groupsListCtnr").hide();

                        } else {
                            $("#groupsListCtnr").show();
                            // remove existing rows
                            while ( groupsList.firstChild ) {
                                groupsList.removeChild(groupsList.firstChild);
                            }
                            json.forEach(function (group) {
                                const newGroupRow = groupRowTemplate.cloneNode(true);
                                newGroupRow.dataset.group = JSON.stringify(group);

                                $(newGroupRow).find("em").text(group.name);
                                $(newGroupRow).find("span").text(group.username);

                                if ( groupsIds.indexOf(group.id) > -1 ) {
                                    $(newGroupRow).find("button").remove();
                                }

                                groupsList.appendChild(newGroupRow);
                            });
                        }
                    });
            } else {
                Informationals.makeWarning(polyglot.t("went_wrong"), polyglot.t("try_again"), 1500).show();
            }
        });
    }
}

function addGroup( element ) {

    // get the li
    let row = element;
    while ( row.nodeName !== "LI" ){
        row = row.parentElement;
    }
    let group = JSON.parse(row.dataset.group);
    post( beRoutes.controllers.CampaignMgrCtrl.doAddGroupToCampaign(campaignId), group.id );
}

function removeFromCampaign(groupId, groupName) {
    var data = {"groupId":groupId};
    swal({
        title: polyglot.t("groups.campaign.remove",{name:groupName}),
        icon: "warning",
        buttons: true,
        dangerMode: true,
    }).then((willDelete) => {
        if (willDelete) {
            new Playjax(beRoutes)
                .using(function(c){
                    return c.CampaignMgrCtrl.doRemoveGroupFromCampaign(campaignId);}).fetch(data)
                .then( function(res){
                    if (res.ok) {
                        document.location.reload();
                    } else {
                        Informationals.makeDanger(polyglot.t("groups.campaign.failed",{name:groupName}),"",3000).show();
                    }
                });
        }
    });
}

function post( action, groupId ) {
    let form = document.createElement("form");
    form.setAttribute("method", action.method);
    form.setAttribute("action", action.url);

    let hiddenField = document.createElement("input");
    hiddenField.setAttribute("type", "hidden");
    hiddenField.setAttribute("name", "groupId");
    hiddenField.setAttribute("value", groupId);
    form.appendChild(hiddenField);

    hiddenField = document.createElement("input");
    hiddenField.setAttribute("type", "hidden");
    hiddenField.setAttribute("name", csrfName);
    hiddenField.setAttribute("value", csrfValue);
    form.appendChild(hiddenField);

    document.body.appendChild(form);
    form.submit();
}

// var tour = {
//     id:"tour",
//     steps:[
//         {
//             "title":polyglot.t("tour.groupsPage.element.title"),
//             "content":polyglot.t("tour.groupsPage.element.content"),
//             "target":"element",
//             placement:"bottom"
//         }
//     ],
//     showPrevButton: true,
//     i18n:{
//         nextBtn: polyglot.t("next"),
//         prevBtn: polyglot.t("prev")
//     }
// };