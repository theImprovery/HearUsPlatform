/* jshint esversion:6 */

let reportingAsOffensive = false;
function reportAsOffensive(campaignSlug, msg) {
    if ( reportingAsOffensive ) return;
    reportingAsOffensive = true;
    $("#flagAsOffensiveMdl").find(".btn-primary").prop("disabled",true);

    const payload = {};
    payload.report = $("#offensiveText").val();
    payload.url = window.location.href;
    payload.campaignId = campaignSlug;

    new Playjax(feRoutes).using( function(c){return c.CampaignPublicCtrl.doReportAsOffensive(campaignSlug);} )
        .fetch(payload)
        .then( function(res) {
            if ( res.ok ) {
                alert(msg);
            } else {
                console.log(res);
            }
        }).finally(function(c){
            $("#flagAsOffensiveMdl").modal('hide');
    });
}