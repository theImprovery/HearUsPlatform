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

var allKms = $("#kmsPositionList");
function filterKmsList(searchStr) {
    allKms.find("li").each(function(idx, emt    ) {
        if((emt.dataset.kmName.indexOf(searchStr) !== -1) || (emt.dataset.partyName.indexOf(searchStr) !== -1)) {
            emt.style.display = "block";
        } else emt.style.display = "none";
    });
}