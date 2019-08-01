function deleteCampaign(id) {
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
                    return c.CampaignAdminCtrl.deleteCampaign(id);}).fetch()
                .then( function(res){
                    if (res.ok) {
                        window.location = beRoutes.controllers.CampaignAdminCtrl.showCampaigns().url;
                    } else {
                        Informationals.makeDanger(polyglot.t("campaign.delete.failed"), polyglot.t("server_logs_details"), 1500).show();
                    }
                });
        }
    });
}


function showCleanDialog() {
    $("#actionModal").modal();
}

