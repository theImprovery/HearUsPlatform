/*jshint esversion: 6 */
var choosenSlug = null;

function patchBeforeUnload() {
    var data = {};
    var fields = ["id", "title", "slogan", "website", "contactEmail", "analyticsCode"];
    fields.forEach(function (value) { data[value] = document.getElementById(value).value; });
    var displayStr = "Updating details";
    var msgDiv = Informationals.showBackgroundProcess(displayStr);
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
            console.log("Error");
            console.log( req );
            console.log( status );
            console.log( error );
            Informationals.show( Informationals.makeDanger("Error updating campaign", status + "\n" + error));
        }
    });
}

function patchDetails() {
    var data = {};
    var fields = ["id", "title", "slogan", "website", "contactEmail", "analyticsCode"];
    fields.forEach(function (value) { data[value] = document.getElementById(value).value; });
    Informationals.loader("Updating..");
    console.log("Informationals ");

    let req = Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignMgrCtrl.updateDetails(Number(data.id));
        })
        .request(data);
    req.async = false;
    fetch(req).then( function (res) {
            Informationals.loader.dismiss();
            if (res.ok) {
                Informationals.makeSuccess("Update Campaign " + data.name, "OK", 1500).show();
                return true;
            } else {
                Informationals.makeDanger("Update Campaign " + data.name, "Failed", 2500).show();
                res.result().then(function(body){
                    console.log(body);
                });
                return false;
            }
        }).catch( err => console.log("err",err) );
}

function chooseSlugName() {
    swal({
        title: 'Choose new Slug for your campaign',
        text: 'The slug should be match to [A-Za-z1-9_-]',
        content: "input",
        button: {
            text: "Ok!",
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
            return swal("This slug already exist").then((val) => chooseSlugName());
        } else{
            campUrl = beRoutes.controllers.CampaignPublicCtrl.index(choosenSlug).absoluteURL();
            document.getElementById("hearUsUrl").value=campUrl;
            document.getElementById("hearUsUrlA").href=campUrl;
            swal.stopLoading();
            Informationals.makeSuccess("Update Slug ", "OK", 1500).show();
            swal.close();
            return choosenSlug;
        }
    })
    .catch(err => {
            if (err) {
                swal("Oh noes!", "The slug should be match to [A-Za-z1-9_-]", "error").then((val) => chooseSlugName());
            } else {
                swal.stopLoading();
                swal.close();
    }
    });
}

window.onbeforeunload = function() {
    return patchBeforeUnload();
};
