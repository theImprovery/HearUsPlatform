function updateRole(cb, userId) {
    var data = {"isAdmin":cb.checked, "id":userId};
    var msg = (cb.checked) ? "Update to admin" : "remove Admin role";
    new Playjax(beRoutes)
        .using(function (c) {
            return c.UserCtrl.updateRole();
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