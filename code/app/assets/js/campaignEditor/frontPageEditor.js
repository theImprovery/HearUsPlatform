/*jshint esversion: 6 */

function frontPageEditorSetup(){
    $("#bodyText").summernote({
        height: 500,
        fontSizes: ['8', '9', '10', '12', '14', '16', '24', '36', '48' , '64', '82', '150'],
        styleTags: [
            { title: 'כותרת ראשית', tag: 'h1', className: '', value: 'h1' },
            { title: 'כותרת משנה', tag: 'h2', className: '', value: 'h2' },
            { title: 'כותרת', tag: 'h3', className: '', value: 'h3' },
            { title: 'כותרת קטנה', tag: 'h4', className: '', value: 'h4' }
        ],
        toolbar: [
            ['minimal', ['bold', 'italic', 'underline', 'clear', 'strikethrough',
                'ul', 'ol', 'paragraph', 'left', 'center', 'right',
                'link','picture','video'
            ]],
            ['style', ['style']]
        ]
    });
}

function prepareForm() {
    const groupValue = positionStrings.map((pos) => document.getElementById("group" + pos).value).join("\t");
    document.getElementById("groupLabels").value = groupValue;
    const mkValues = [];
    for (let g in genderStrings) {
        for (let p in positionStrings) {
            let key = genderStrings[g] + positionStrings[p] + "Label";
            mkValues.push(document.getElementById(key).value);
        }
    }
    document.getElementById("kmLabels").value = mkValues.join("\t");
}

window.onbeforeunload = function() {
    submitBeforeUnload();
};

function submitBeforeUnload() {
    prepareForm();
    var data = {};
    var fields = ["title", "subtitle", "bodyText", "footer", "groupLabels", "kmLabels"];
    data.campaignId = Number(document.getElementById("campaignId").value);
    fields.forEach(function (value) { data[value] = document.getElementById(value).value; });
    var msgDiv = Informationals.showBackgroundProcess(polyglot.t("update.front_page"));
    var call = beRoutes.controllers.CampaignMgrCtrl.apiUpdateFrontPage(data.campaignId);
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
        console.log( "req",req);
        console.log( "err",error);
        console.log( "st",status);
        if ( req.readyState === 4 ) {
            // don't fire if we're navigating away from the page.
            console.log("Error");
            console.log( req );
            console.log( status );
            console.log( error );
            Informationals.show( Informationals.makeDanger("Error updating campaign", status + "\n" + error));
        }
    });
}

const tour = {
    id:"tour",
    steps:[
        {
            title:polyglot.t("tour.frontPage.section.title"),
            content:polyglot.t("tour.frontPage.section.content"),
            target:"pageSection_FrontPage",
            placement:"bottom"
        },
        {
            title:polyglot.t("tour.frontPage.sampleHelp.title"),
            target:"sampleHelp",
            placement:"top"
        }
    ],
    showPrevButton: true,
    i18n:{
        nextBtn: polyglot.t("next"),
        prevBtn: polyglot.t("prev"),
        doneBtn: polyglot.t("done")
    }
};