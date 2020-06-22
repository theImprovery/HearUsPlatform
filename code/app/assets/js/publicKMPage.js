/* jshint esversion:6 */

const PublicKMPage = (function(){
    const EULA_ACCEPTANCE_KEY = "EULA_ACCEPTANCE_KEY";

    let nextUrl;

    function ensureEulaAcceptance() {
        if (!document.cookie.split('; ').find(row => row.startsWith(EULA_ACCEPTANCE_KEY))) {
            $("#eulaApprovalModal").modal("show");
        } else {
            return true;
        }
    }

    function approveEula() {
        document.cookie = "EULA_ACCEPTANCE_KEY=true; expires=Fri, 31 Dec 9999 23:59:59 GMT";
        $("#eulaApprovalModal").modal("close");
        perform();
    }

    function perform() {
        document.location.href=nextUrl;
    }

    function contactKm( url ) {
        nextUrl = url;
        if ( ensureEulaAcceptance() ) {
            perform();
        }
    }

    return {
        contactKm:contactKm,
        approveEula:approveEula

    };
})();