/*jshint esversion: 6 */

function frontPageEditorSetup(){
    $("#bodyText").summernote({
        height: 500,
        fontSizes: ['8', '9', '10', '12', '14', '16', '24', '36', '48' , '64', '82', '150'],
        toolbar: [
            // [groupName, [list of button]]
            ['font', ['fontname', 'fontsize', 'forecolor','backcolor']],
            ['style', ['bold', 'italic', 'underline', 'clear', 'strikethrough']],
            ['color', ['color']],
            ['para', ['ul', 'ol', 'paragraph', 'left', 'center', 'right']],
            ['insert', ['link','picture','video','hr']],
            ['misc',['fullscreen','codeview']]
        ],
        fontNames: ['Alef', 'Arial', 'Courier New'],
        fontNamesIgnoreCheck:['Alef']

    });

}

function prepareForm() {
    var groupValue = positionStrings.map( (pos)=> document.getElementById("group"+pos).value ).join("\t");
    document.getElementById("groupLabels").value = groupValue;
    var mkValues = [];
    for ( var g in genderStrings ) {
        for ( var p in positionStrings ) {
            var key = genderStrings[g]+positionStrings[p]+"Label";
            mkValues.push( document.getElementById(key).value );
        }
    }
    document.getElementById("kmLabels").value = mkValues.join("\t");

    console.log( document.getElementById("kmLabels").value );
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