function deleteAction(id, camId, kmId) {
    console.log("id", id);
    console.log("camIId", camId);
    console.log("kmId", kmId);
    swal({
        title:"למחוק פעולה זו?",
        icon:"warning",
        buttons: {
            cancel:"ביטול",
            confirm:"אישור"
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
                        Informationals.makeDanger("Deletion of action "+ id +" failed", "See server log for details", 1500).show();
                    }
                });
        }
    });
}