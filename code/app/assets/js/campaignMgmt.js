function deleteCampaign(id, from) {
    swal({
        title:polyglot.t("campaign.delete.warning"),
        icon:"warning",
        buttons: {
            cancel:polyglot.t("cancel"),
            confirm:polyglot.t("confirm")
        },
    }).then( function(willDelete){
        if(willDelete) {
            new Playjax(beRoutes)
                .using(function(c){
                    return c.CampaignAdminCtrl.deleteCampaign(id, from);}).fetch()
                .then( function(res){
                    if (res.ok) {
                        console.log("res", res);
                        window.location.reload();
                    } else {
                        Informationals.makeDanger(polyglot.t("campaign.delete.failed"), polyglot.t("server_logs_details"), 1500).show();
                    }
                });
        }
    });
}

function askForPublish(id) {
    new Playjax(beRoutes)
        .using(function(c){
            return c.CampaignMgrCtrl.sendToApprove(id);}).fetch()
        .then( function(res){
            if (res.ok) {
                window.location = beRoutes.controllers.CampaignMgrCtrl.index().url;
            } else {
                Informationals.makeDanger(polyglot.t("km.failed"), polyglot.t("server_logs_details"), 1500).show();
            }
        });
}