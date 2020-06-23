/* jshint esversion:6 */
const PublicKMPage = (function(){
    const EULA_ACCEPTANCE_KEY = "EULA_ACCEPTANCE_KEY";
    const pjx = new Playjax(feRoutes);
    let pendingAction;
    let campaignId, kmId;

    function ensureEulaAcceptance() {
        if (!document.cookie.split('; ').find(row => row.startsWith(EULA_ACCEPTANCE_KEY))) {
            $("#eulaApprovalModal").modal("show");
            return false;
        } else {
            return true;
        }
    }

    function approveEula() {
        document.cookie = "EULA_ACCEPTANCE_KEY=true; expires=Fri, 31 Dec 9999 23:59:59 GMT";
        $("#eulaApprovalModal").modal("hide");
        perform();
    }

    function perform() {

        pjx.using(c=>c.CampaignPublicCtrl.apiReportInteraction()).fetch({
            id:0,
            campaignId :campaignId,
            kmId       :kmId,
            medium     :pendingAction.medium,
            link       :pendingAction.url
        }).finally(function(){
            if ( pendingAction.url.indexOf("http")=== 0 ) {
                const win = window.open(pendingAction.url, '_blank');
                win.focus();
            } else {
                document.location.href=decodeURI(pendingAction.url);
            }
        });

    }

    function contactKm( medium, url ) {
        pendingAction = {medium:medium, url:url};
        if ( ensureEulaAcceptance() ) {
            perform();
        }
    }

    return {
        contactKm:contactKm,
        approveEula:approveEula,
        setKm: function(aKM){ kmId = aKM; },
        setCampaign: function(aCampaign){ campaignId = aCampaign; }
    };
})();