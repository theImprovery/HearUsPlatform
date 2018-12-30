function deleteCampaign(id) {
    swal({
        title:"Are you sure you want to delete this campaign?",
        icon:"warning",
        buttons: {
            cancel:true,
            confirm:true
        }
    }).then( function(willDelete){
        if(willDelete) {
            new Playjax(beRoutes)
                .using(function(c){
                    return c.CampaignAdminCtrl.deleteCampaign(id);}).fetch()
                .then( function(res){
                    if (res.ok) {
                        window.location = beRoutes.controllers.CampaignAdminCtrl.showCampaigns().url;
                    } else {
                        Informationals.makeDanger("Deletion of campaign "+ id +" failed", "See server log for details", 1500).show();
                    }
                });
        }
    });
}


function showCleanDialog() {
    $("#actionModal").modal();
}

