/*jshint esversion:6*/

const tour = {
    id:"tour",
    steps:[
        {
            title:polyglot.t("tour.groupsPage.section.title"),
            content:polyglot.t("tour.groupsPage.section.content"),
            target:"pageSection_CampaignGroups",
            placement:"bottom"
        },{
            title:polyglot.t("tour.groupsPage.groupTable.title"),
            content:polyglot.t("tour.groupsPage.groupTable.content"),
            target:"groupTable",
            placement:"left"
        },{
            title:polyglot.t("tour.groupsPage.groupSearchField.title"),
            content:polyglot.t("tour.groupsPage.groupSearchField.content"),
            target:"groupSearchField",
            placement:"right"
        }
    ],
    showPrevButton: true,
    i18n:{
        nextBtn: polyglot.t("next"),
        prevBtn: polyglot.t("prev"),
        doneBtn: polyglot.t("done")
    }
};

const allGroups = $("#groupsList");
function filterGroupsList(searchStr) {
    searchStr = searchStr.replace(/'/g, "%");
    searchStr = searchStr.replace(/\"/g, "@");
    searchStr = searchStr.replace(/״/g, "@");
    allGroups.find("li").each(function(idx, emt ) {
        if((emt.dataset.groupName.indexOf(searchStr) !== -1)) {
            emt.style.display = "flex";
        } else emt.style.display = "none";
    });
}

function changeGroup(emt) {
    var data = {};
    var msg = emt.checked ? polyglot.t("update.group.add") : polyglot.t("update.group.remove");
    data.camId = Number($("#id").val());
    data.groupId = Number(emt.id.split("-")[0]);
    data.add = emt.checked;
    new Playjax(beRoutes)
        .using(function (c) {
            return (emt.checked ? c.CampaignMgrCtrl.addGroupToCamp() : c.CampaignMgrCtrl.removeGroupFromCamp());
        })
        .fetch(data)
        .then( function (res) {
            if (res.ok) {
                Informationals.makeSuccess(msg, "", 1000).show();
            } else {
                Informationals.makeWarning(polyglot.t("went_wrong"), polyglot.t("try_again"), 1500).show();
            }
        });
}