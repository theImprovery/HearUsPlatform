/* jshint esversion:6 */
function changePosition(emt) {
    var data = {};
    data.camId = Number($("#id").val());
    data.kmId = Number(emt.id.split("-")[0]);
    data.position = emt.id.split("-")[1];
    new Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignMgrCtrl.updatePosition();
        })
        .fetch(data)
        .then( function (res) {
            if (res.ok) {
                Informationals.makeSuccess(polyglot.t("update.position"), "", 1000).show();
            } else {
                Informationals.makeWarning(polyglot.t("went_wrong"), polyglot.t("try_again"), 1500).show();
            }
        });
}

const allKms = $("#kmsPositionList");
function filterKmsList(searchStr) {
    searchStr = searchStr.replace(/'/g, "%");
    searchStr = searchStr.replace(/\"/g, "@");
    searchStr = searchStr.replace(/״/g, "@");
    allKms.find("li").each(function(idx, emt ) {
        if((emt.dataset.kmName.indexOf(searchStr) !== -1) || (emt.dataset.partyName.indexOf(searchStr) !== -1)) {
            emt.style.display = "block";
        } else emt.style.display = "none";
    });
}

const tour = {
    id:"tour",
    steps:[
        {
            title:polyglot.t("tour.positionsPage.section.title"),
            content:polyglot.t("tour.positionsPage.section.content"),
            target:"pageSection_KnessetMembers",
            placement:"bottom"
        },{
            title:polyglot.t("tour.positionsPage.kmsPositionList.title"),
            content:polyglot.t("tour.positionsPage.kmsPositionList.content"),
            target:"kmListCenter",
            placement:"top"
        },{
            title:polyglot.t("tour.positionsPage.searchStr.title"),
            content:polyglot.t("tour.positionsPage.searchStr.content"),
            target:"searchStr",
            placement:"left"
        }
    ],
    showPrevButton: true,
    i18n:{
        nextBtn: polyglot.t("next"),
        prevBtn: polyglot.t("prev"),
        doneBtn: polyglot.t("done")
    }
};