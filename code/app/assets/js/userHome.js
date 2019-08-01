function updateKmsAndPartes() {
    var info = Informationals.showBackgroundProcess(polyglot.t("import.km"));
    new Playjax(beRoutes)
        .using(function(c){ return c.ParseCtrl.apiKms();}).fetch()
        .then( function(res){
            if (res.ok) {
                info.success();
            } else {
                Informationals.makeDanger(polyglot.t("went_wrong"), 1500).show();
            }
        });
}

function updateCommittees() {
    var info = Informationals.showBackgroundProcess(polyglot.t("import.committees"));
    new Playjax(beRoutes)
        .using(function(c){ return c.ParseCtrl.apiUpdateCommittees();}).fetch()
        .then( function(res){
            if (res.ok) {
                info.success();
            } else {
                Informationals.makeDanger(polyglot.t("went_wrong"), 1500).show();
            }
        });
}