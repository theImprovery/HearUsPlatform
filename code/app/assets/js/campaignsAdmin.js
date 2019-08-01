function updatePublish(cb, campId) {
    var data = {"isPublish":cb.checked, "id":campId};
    var msg = (cb.checked) ? polyglot.t("campaign.publish") : polyglot.t("campaign.unpublish");
    new Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignAdminCtrl.updatePublish();
        })
        .fetch(data)
        .then( function (res) {
            if (res.ok) {
                Informationals.makeSuccess(msg, "", 1000).show();
            } else {
                Informationals.makeWarning("Something went wrong", "try again", 1500).show();
            }
        });
}

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