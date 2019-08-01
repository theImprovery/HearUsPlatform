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
    Informationals.loader("uploading design data");
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
        title: "Are you sure?",
        text: "This operation cannot be undone",
        icon: "warning",
        buttons: true,
        dangerMode: true
    }).then(function(willDelete){
        if (willDelete) {
            new Playjax(beRoutes)
                .using(function(c){return c.CampaignMgrCtrl.deleteCampaignImage(campaignId);})
                .fetch()
                .then( function(res){
                   if ( res.ok ) {
                       Informationals.makeSuccess("Image Deleted", "", 2000).show();
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
    console.log("in before unload");
    postForm();
};