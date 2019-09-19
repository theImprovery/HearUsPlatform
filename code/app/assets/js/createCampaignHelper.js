/*jshint esversion: 6 */
var test;
function chooseTitleForCampaign(routes) {
    swal({
        title: polyglot.t("name.choose"),
        text: polyglot.t("name.description"),
        content: "input",
        buttons: {
            cancel: {
                text: polyglot.t("cancel"),
                value: null,
                visible: true,
                className: "",
                closeModal: true,
            },
            confirm: {
                text:  polyglot.t("save"),
                visible: true,
                className: "",
                closeModal: true
            }},
    }).then(title => {
        if (!title) throw null;
        if ( typeof beRoutes !== "undefined" ) {
            return Playjax(beRoutes)
                .using(function (c) {
                    return c.CampaignMgrCtrl.createCampaign(title);
                }).fetch(title);
        } else {
            return Playjax(feRoutes)
                .using(function (c) {
                    return c.UserCtrl.showSignupPageForNewCamp(title);
                }).fetch();
        }
    })
        .then( function (res) {
            test = res;
                if (!res.ok) {
                    swal(polyglot.t("went_wrong"), polyglot.t("try_again"), "error").then((val) => chooseTitleForCampaign());
                }else {
                    window.location = res.url;
                }
        });
}