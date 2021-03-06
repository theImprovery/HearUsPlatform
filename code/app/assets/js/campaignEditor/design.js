/* jshint esversion:6 */
function collectCss() {
    var $colors = $("input[type='color']");
    var valueTriplets = $colors.map( function(i,c){
        var $c = $(c);
        return [[$c.data("selector"), $c.data("key"), $c.val()]];
    }).get();

    var cssObj = {};
    valueTriplets.forEach( function(t){
       if ( !cssObj[t[0]] ) {
           cssObj[t[0]] = {};
       }
       cssObj[t[0]][t[1]]=t[2];
    });

    var codeLines = [];
    for ( var k in cssObj ) {
        codeLines.push( k + " { ");
        codeLines.push( simpleStringify(cssObj[k]) );
        codeLines.push( "}" );
    }

    codeLines.push("/*---*/");
    codeLines.push( $("#extraCss").val() );

    return codeLines.join("\n");
}

function simpleStringify( obj ) {
    var arr = [];
    for ( var k in obj ) {
        var val = obj[k];
        arr.push( "  " + k + ": " + val + ";" );
    }
    return arr.join("\n");
}

function postForm() {
    Informationals.loader(polyglot.t("update.design"));
    var css = collectCss();
    var formData = new FormData();
    formData.append( "css", css );
    formData.append( "imageCredit", $("#imageCredit").val() );
    formData.append( "imageFile",   $("#imageFile")[0].files[0] );

    var act = beRoutes.controllers.CampaignMgrCtrl.doUpdateCampaignDesign(campaignId);
    var xhr = new XMLHttpRequest();

    xhr.open( act.method, act.url, false );
    xhr.setRequestHeader("Csrf-Token", document.getElementById("Playjax_csrfTokenValue").innerText);

    xhr.onload = function(oEvent) {
        if (xhr.status !== 200) {
            Informationals.loader.dismiss();
            Informationals.makeDanger("Error " + xhr.status + " occurred when trying to upload your file.", "", 2000).show();
            console.log("Error uploading design data");
            console.log(oEvent);
            console.log(xhr.status);
        } else {
            if ( $("#imageFile")[0].files[0] ) {
                window.setTimeout(function(){window.location.reload();}, 3000);
            } else {
                Informationals.loader.dismiss();
            }
        }
    };

    xhr.send( formData );

}

function setColor( selector, key, value ) {
    $("input[type='color']"
     ).filter(function(i,c){
         var data = $(c).data();
         return data.selector === selector && data.key===key;
    }).val(value);
}

function deleteImage() {
    swal({
        title: polyglot.t("are_you_sure"),
        text: polyglot.t("operation_undo"),
        icon: "warning",
        buttons: {
            cancel:polyglot.t("cancel"),
            confirm:polyglot.t("confirm")
        },
        dangerMode: true
    }).then(function(willDelete){
        if (willDelete) {
            new Playjax(beRoutes)
                .using(function(c){return c.CampaignMgrCtrl.deleteCampaignImage(campaignId);})
                .fetch()
                .then( function(res){
                   if ( res.ok ) {
                       Informationals.makeSuccess(polyglot.t("image_delete"), "", 2000).show();
                       setHasImage( false );
                   }
                });
    }});
}

function setHasImage( hasImage ) {
    if ( hasImage ) {
        $("#noImage").hide();
        $("#imageDiv").show();
        $("#deleteImageBtn").show();
    } else {
        $("#noImage").show();
        $("#imageDiv").hide();
        $("#deleteImageBtn").hide();
    }
}

window.onbeforeunload = function () {
    postForm();
};

const tour = {
    id:"tour",
    steps:[
        {
            title:polyglot.t("tour.designPage.section.title"),
            content:polyglot.t("tour.designPage.section.content"),
            target:"pageSection_Design",
            placement:"bottom"
        },
        {
            title:polyglot.t("tour.designPage.colors.title"),
            content:polyglot.t("tour.designPage.colors.content"),
            target:"colorsRow",
            placement:"left"
        },
        {
            title:polyglot.t("tour.designPage.imageRow.title"),
            content:polyglot.t("tour.designPage.imageRow.content"),
            target:"imageRow",
            placement:"left"
        },
        {
            title:polyglot.t("tour.designPage.cssRow.title"),
            content:polyglot.t("tour.designPage.cssRow.content"),
            target:"cssRow",
            placement:"left"
        }
    ],
    showPrevButton: true,
    i18n:{
        nextBtn: polyglot.t("next"),
        prevBtn: polyglot.t("prev"),
        doneBtn: polyglot.t("done")
    }
};