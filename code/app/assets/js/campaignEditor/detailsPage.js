function patchDetails() {
    var data = {};
    var fields = ["subtitle", "website", "themeData", "contactEmail"];
    fields.forEach(function (value) { data[value] = document.getElementById(value).value; });
    data.id = Number(document.getElementById("id").value);
    data.title = document.getElementById("title").value;
    data.isPublish = Boolean(document.getElementById("isPublish").value);
    console.log("data", data);
    var msgDiv = Informationals.showBackgroundProcess("Updating..");
    new Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignMgrCtrl.updateDetails();
        })
        .fetch(data)
        .then( function (res) {
            if (res.ok) {
                msgDiv.success();
            } else {
                msgDiv.dismiss();
                Informationals.makeWarning("Update Campaign " + data.name, "Failed", 1500);
            }
        });
}