/* jshint esversion:6 */
function deleteAction(id, camId, kmId) {
    console.log("id", id);
    console.log("camIId", camId);
    console.log("kmId", kmId);
    swal({
        title:polyglot.t("action.delete"),
        icon:"warning",
        buttons: {
            cancel:polyglot.t("cancel"),
            confirm:polyglot.t("confirm")
        }
    }).then( function(willDelete){
        if(willDelete) {
            new Playjax(beRoutes)
                .using(function(c){
                    return c.CampaignMgrCtrl.deleteAction(id, camId, kmId);}).fetch()
                .then( function(res){
                    if (res.ok) {
                        window.location = beRoutes.controllers.CampaignMgrCtrl.allActions(camId, kmId).url;
                    } else {
                        Informationals.makeDanger(polyglot.t("action.failed", {id:id}), polyglot.t("server_logs_details"), 1500).show();
                    }
                });
        }
    });
}

const tour = {
    id:"tour",
    steps:[
        {
            title:polyglot.t("tour.actionsPage.kmName.title"),
            content:polyglot.t("tour.actionsPage.kmName.content"),
            target:"kmName",
            placement:"left"
        },{
            title:polyglot.t("tour.actionsPage.actionTable.title"),
            content:polyglot.t("tour.actionsPage.actionTable.content"),
            target:"actionTable",
            placement:"top"
        },{
            title:polyglot.t("tour.actionsPage.actionTableNoData.title"),
            content:polyglot.t("tour.actionsPage.actionTableNoData.content"),
            target:"actionTableNoData",
            placement:"top"
        },{
            title:polyglot.t("tour.actionsPage.addAction.title"),
            content:polyglot.t("tour.actionsPage.addAction.content"),
            target:"addAction",
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