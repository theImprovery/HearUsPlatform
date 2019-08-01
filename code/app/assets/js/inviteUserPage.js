/*jshint esversion: 6  */

function deleteUuid(uuid){
    swal({
        title:polyglot.t("invitation.delete"),
        icon:"warning",
        buttons: {
            cancel:polyglot.t("cancel"),
            confirm:polyglot.t("confirm")
        }
    }).then( function(willDelete){
        if(willDelete) {
            new Playjax(beRoutes)
                .using(function(c){ return c.UserCtrl.apiDeleteInvitation(uuid);}).fetch()
                .then( function(res){
                    if (res.ok) {
                        window.location.reload();
                    } else {
                        Informationals.makeDanger(polyglot.t("invitation.failed"), polyglot.t("server_logs_details"), 1500).show();
                    }
                });
        }
    });
}

function resendEmail(uuid){
    new Playjax(beRoutes).using(c=>c.UserCtrl.apiReInviteUser(uuid)).fetch()
        .then( resp => {
            if (resp.ok) {
                Informationals.makeSuccess(polyglot.t("invitation.resend"), "", 1500).show();
            } else {
                Informationals.makeDanger(polyglot.t("invitation.resend_failed"), "", 1500).show();
            }
        });
}