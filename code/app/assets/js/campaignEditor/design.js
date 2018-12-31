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

    cssObj["h1,h2,h3,h4,h5,h6"] = cssObj.h;
    delete cssObj.h;

    var codeLines = [];
    for ( var k in cssObj ) {
        codeLines.push( k + " { ");
        codeLines.push( simpleStringify(cssObj[k]) );
        codeLines.push( "}" );
    }

    codeLines.push("/*******/");
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
    Informationals.loader("uploading design data");
    var css = collectCss();
    var formData = new FormData();
    formData.append( "css", css );
    formData.append( "imageCredit", $("#imageCredit").val() );
    formData.append( "imageFile",   $("#imageFile")[0].files[0] );

    var act = beRoutes.controllers.CampaignMgrCtrl.doUpdateCampaignDesign(campaignId);
    var xhr = new XMLHttpRequest();

    xhr.open( act.method, act.url );
    xhr.setRequestHeader("Csrf-Token", document.getElementById("Playjax_csrfTokenValue").innerText);

    xhr.onload = function(oEvent) {
        Informationals.loader.dismiss();
        if (xhr.status !== 200) {
            Informationals.makeDanger("Error " + xhr.status + " occurred when trying to upload your file.", "", 2000).show();
            console.log("Error uploading design data");
            console.log(oEvent);
            console.log(xhr.status);
        }
    };

    xhr.send( formData );

}
