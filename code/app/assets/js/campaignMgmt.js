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

function changeRequestStatus(status, camId) {
    var msg = (status===1) ? polyglot.t("sendToRequest") : polyglot.t("cancelRequest");
    new Playjax(beRoutes)
        .using(function(c){
            return c.CampaignStatusCtrl.changeRequestStatus(camId, status);}).fetch()
        .then( function(res){
            if (res.ok) {
                Informationals.makeInfo(msg,"", 1500).show();
                if(status === 1){
                    document.getElementsByName("publishBtn_"+camId)[0].style.display = "none";
                    document.getElementsByName("cancelPublishBtn_"+camId)[0].style.display = "inline";
                    if(document.getElementsByName("lblWp_"+camId)[0]){
                        document.getElementsByName("lblWp_"+camId)[0].style.display = "none";
                    }
                    document.getElementsByName("lblPr_"+camId)[0].style.display = "inline";
                    if(document.getElementsByName("lblRej_"+camId)){
                        document.getElementsByName("lblRej_"+camId)[0].style.display = "none";
                    }
                } else{
                    document.getElementsByName("cancelPublishBtn_"+camId)[0].style.display = "none";
                    document.getElementsByName("publishBtn_"+camId)[0].style.display = "inline";
                    if(document.getElementsByName("lblWp_"+camId)[0]){
                        document.getElementsByName("lblWp_"+camId)[0].style.display = "inline";
                    }
                    document.getElementsByName("lblPr_"+camId)[0].style.display = "none";
                }
            } else {
                Informationals.makeDanger(polyglot.t("km.failed"), polyglot.t("server_logs_details"), 1500).show();
            }
        });
}