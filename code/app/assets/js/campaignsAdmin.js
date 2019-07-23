function updatePublish(cb, campId) {
    var data = {"isPublish":cb.checked, "id":campId};
    var msg = (cb.checked) ? "Publish" : "Unpublish";
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
                    return c.CampaignAdminCtrl.deleteCampaign(id, from);}).fetch()
                .then( function(res){
                    if (res.ok) {
                        console.log("res", res);
                        window.location.reload();
                    } else {
                        Informationals.makeDanger("Deletion of campaign "+ id +" failed", "See server log for details", 1500).show();
                    }
                });
        }
    });
}