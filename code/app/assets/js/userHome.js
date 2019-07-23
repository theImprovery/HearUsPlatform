function updateKmsAndPartes() {
    var info = Informationals.showBackgroundProcess("Finish import Knesset members");
    new Playjax(beRoutes)
        .using(function(c){ return c.ParseCtrl.apiKms();}).fetch()
        .then( function(res){
            if (res.ok) {
                info.success();
            } else {
                Informationals.makeDanger("Something went wrong", 1500).show();
            }
        });
}

function updateCommittees() {
    var info = Informationals.showBackgroundProcess("Finish import committees");
    new Playjax(beRoutes)
        .using(function(c){ return c.ParseCtrl.apiUpdateCommittees();}).fetch()
        .then( function(res){
            if (res.ok) {
                info.success();
            } else {
                Informationals.makeDanger("Something went wrong", 1500).show();
            }
        });
}