function updateRole(cb, userId) {
    var data = {"isAdmin":cb.checked, "id":userId};
    var msg = (cb.checked) ? polyglot.t("admin.upgrade") : polyglot.t("admin.remove");
    new Playjax(beRoutes)
        .using(function (c) {
            return c.UserCtrl.updateRole();
        })
        .fetch(data)
        .then( function (res) {
            if (res.ok) {
                Informationals.makeSuccess(msg, "", 1000).show();
            } else {
                Informationals.makeWarning(polyglot.t("went_wrong"), polyglot.t("try_again"), 1500).show();
            }
        });
}