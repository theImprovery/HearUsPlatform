/* jshint esversion:6 */
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
                        window.location.reload();
                    } else {
                        Informationals.makeDanger(polyglot.t("campaign.delete.failed"), polyglot.t("server_logs_details"), 1500).show();
                    }
                });
        }
    });
}

function changeRequestStatus(status, camId) {
    const msg = (status===1) ? polyglot.t("sendToRequest") : polyglot.t("cancelRequest");
    new Playjax(beRoutes)
        .using(function(c){
            return c.CampaignStatusCtrl.changeRequestStatus(camId, status);}).fetch()
        .then( function(res){
            if (res.ok) {
                Informationals.makeInfo(msg,"", 1500).show();
                if ( status === 1 ) {
                    document.getElementById("publishBtn_"+camId).style.display = "none";
                    document.getElementById("cancelPublishBtn_"+camId).style.display = "inline";
                    if(document.getElementById("lblWp_"+camId)){
                        document.getElementById("lblWp_"+camId).style.display = "none";
                    }
                    document.getElementById("lblPr_"+camId).style.display = "inline";
                    if(document.getElementById("lblRej_"+camId)){
                        document.getElementById("lblRej_"+camId).style.display = "none";
                    }
                } else {
                    document.getElementById("cancelPublishBtn_"+camId).style.display = "none";
                    document.getElementById("publishBtn_"+camId).style.display = "inline";
                    if(document.getElementById("lblWp_"+camId)){
                        document.getElementById("lblWp_"+camId).style.display = "inline";
                    }
                    document.getElementById("lblPr_"+camId).style.display = "none";
                }
            } else {
                Informationals.makeDanger(polyglot.t("campaign.statusChangeFailed"), polyglot.t("server_logs_details"), 1500).show();
            }
        });
}