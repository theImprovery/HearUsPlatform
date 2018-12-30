function patchDetails() {
    var data = {};
    var fields = ["id", "title", "slogan", "website", "contactEmail"];
    fields.forEach(function (value) { data[value] = document.getElementById(value).value; });
    Informationals.loader("Updating..");
    new Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignMgrCtrl.updateDetails(Number(data.id));
        })
        .fetch(data)
        .then( function (res) {
            Informationals.loader.dismiss();
            if (res.ok) {
                Informationals.makeSuccess("Update Campaign " + data.name, "OK", 1500).show();
            } else {
                Informationals.makeDanger("Update Campaign " + data.name, "Failed", 2500).show();
                res.result().then(function(body){
                    console.log(body);
                });
            }
        });
}