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