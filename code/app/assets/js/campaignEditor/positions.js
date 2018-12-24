function changePosition(emt) {
    var data = {};
    data.camId = Number($("#id").val());
    data.kmId = Number(emt.id.split("-")[0]);
    data.position = emt.id.split("-")[1];
    new Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignMgrCtrl.updatePosition();
        })
        .fetch(data)
        .then( function (res) {
            if (res.ok) {
                Informationals.makeSuccess("Updated Position", "", 1000).show();
            } else {
                Informationals.makeWarning("Something went wrong", "try again", 1500).show();
            }
        });
}