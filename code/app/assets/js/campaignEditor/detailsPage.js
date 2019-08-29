/*jshint esversion: 6 */
var choosenSlug = null;

function patchBeforeUnload() {
    var data = {};
    var fields = ["id", "title", "slogan", "website", "contactEmail", "analyticsCode"];
    fields.forEach(function (value) { data[value] = document.getElementById(value).value; });
    var msgDiv = Informationals.showBackgroundProcess(polyglot.t("update.details"));
    var call = beRoutes.controllers.CampaignMgrCtrl.updateDetails(Number(data.id));
    $.ajax({ url: call.url,
        type: call.type,
        data: JSON.stringify(data),
        dataType: "json",
        async: false,
        contentType: "application/json; charset=utf-8",
        headers:{
            'Csrf-Token': document.getElementById("Playjax_csrfTokenValue").innerText
        }
    }).done(function(){
        msgDiv.success();
        return true;
    }).always(function(){
        msgDiv.dismiss();
    }).fail( function(req, status, error){
        console.log( req);
        if ( req.readyState === 4 ) {
            // don't fire if we're navigating away from the page.
            Informationals.show( Informationals.makeDanger("Error updating campaign", status + "\n" + error));
        }
    });
}

function patchDetails() {
    var data = {};
    var fields = ["id", "title", "slogan", "website", "contactEmail", "analyticsCode"];
    fields.forEach(function (value) { data[value] = document.getElementById(value).value; });
    Informationals.loader(polyglot.t("update.details"));

    let req = Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignMgrCtrl.updateDetails(Number(data.id));
        })
        .request(data);
    req.async = false;
    fetch(req).then( function (res) {
            Informationals.loader.dismiss();
            if (res.ok) {
                Informationals.makeSuccess(polyglot.t("update.success"),"", 1500).show();
                return true;
            } else {
                Informationals.makeDanger(polyglot.t("update.failed"), "", 2500).show();
                res.result().then(function(body){
                    console.log(body);
                });
                return false;
            }
        }).catch( err => console.log("err",err) );
}

function chooseSlugName() {
    swal({
        title: polyglot.t("slug.choose"),
        text: polyglot.t("slug.match"),
        content: "input",
        button: {
            text: polyglot.t("save"),
            closeModal: false,
        },
    }).then(slug => {
        if (!slug) throw null;
        else choosenSlug = slug;
        return Playjax(beRoutes)
            .using(function (c) {
                return c.CampaignMgrCtrl.apiCheckAndUpdateSlug(document.getElementById("id").value);
            }).fetch(slug);
    })
    .then(results => {
        return results.json();
    })
    .then(success => {
        if (!success) {
            choosenSlug = null;
            return swal(polyglot.t("slug.exist"),"","error").then((val) => chooseSlugName());
        } else{
            const campUrl = beRoutes.controllers.CampaignPublicCtrl.index(choosenSlug).absoluteURL();
            document.getElementById("hearUsUrl").value=campUrl;
            document.getElementById("hearUsUrlA").href=campUrl;
            swal.stopLoading();
            Informationals.makeSuccess(polyglot.t("slug.update"), "", 1500).show();
            swal.close();
            return choosenSlug;
        }
    })
    .catch(err => {
            if (err) {
                swal(polyglot.t("oh"), polyglot.t("slug.match"), "error").then((val) => chooseSlugName());
            } else {
                swal.stopLoading();
                swal.close();
    }
    });
}

window.onbeforeunload = function() {
    hopscotch.endTour(true);
    return patchBeforeUnload();
};

const metaTourSteps = [
    {
        title:polyglot.t("metaTourSteps.details.title"),
        content: polyglot.t("metaTourSteps.details.content"),
        target: "pageSection_Details",
        placement: "bottom"
    },
    {
        title:polyglot.t("metaTourSteps.messages.title"),
        content: polyglot.t("metaTourSteps.messages.content"),
        target: "pageSection_Messages",
        placement: "bottom"
    },
    {
        title:polyglot.t("metaTourSteps.frontPage.title"),
        content: polyglot.t("metaTourSteps.frontPage.content"),
        target: "pageSection_FrontPage",
        placement: "bottom"
    },
    {
        title:polyglot.t("metaTourSteps.design.title"),
        content: polyglot.t("metaTourSteps.design.content"),
        target: "pageSection_Design",
        placement: "bottom"
    },
    {
        title:polyglot.t("metaTourSteps.knessetMembers.title"),
        content: polyglot.t("metaTourSteps.knessetMembers.content"),
        target: "pageSection_KnessetMembers",
        placement: "bottom"
    },
    {
        title:polyglot.t("metaTourSteps.groups.title"),
        content: polyglot.t("metaTourSteps.groups.content"),
        target: "pageSection_CampaignGroups",
        placement: "bottom"
    },
    {
        title:polyglot.t("metaTourSteps.team.title"),
        content: polyglot.t("metaTourSteps.team.content"),
        target: "pageSection_CampaignTeam",
        placement: "bottom"
    },
];

const tourOptions = {
    id:"metaTour",
    steps:metaTourSteps,
    showPrevButton: true,
    showCloseButton:true,
    i18n:{
        nextBtn: polyglot.t("next"),
        prevBtn: polyglot.t("prev")
    }
};

// var tour = {
//     id:"tour",
//     steps:[
//         {
//             "title":polyglot.t("tour.detailsPage.element.title"),
//             "content":polyglot.t("tour.detailsPage.element.content"),
//             "target":"element",
//             placement:"bottom"
//         }
//     ],
//     showPrevButton: true,
//     i18n:{
//         nextBtn: polyglot.t("next"),
//         prevBtn: polyglot.t("prev")
//     }
// };