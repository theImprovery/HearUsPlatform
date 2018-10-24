var optionTemplate, platformList;
$(document).ready( function() {
    var kmId = $("#id").val();
    loadFiles(kmId);
    setupContactOptions(kmId);

});

function showAddImage() {
    $("#fileEditorPanel").slideDown();
    $("#noImagePanel").slideUp();
}

function hideFileEditor() {
    $("#noImagePanel").slideDown();
}

var fileCardCtnr, template;
function loadFiles(kmId) {
    if ( ! fileCardCtnr ) {
        fileCardCtnr = $("#imageCtnr");
        template = fileCardCtnr.find("div[data-role='imageTemplate']");
        template.remove();
    }
    $.ajax(beRoutes.controllers.FilesCtrl.apiFilesForKm(kmId))
        .done( function (data) {

            $("#loadingImagePanel").remove();
            if ( !data ) {
                $("#noImagePanel").slideDown();
            } else {
                for (var di in data){
                    createCard(data[di]);
                }
            }
        });
}

function loadSingleFile(imageId) {
    $.ajax(beRoutes.controllers.FilesCtrl.apiGetImage(imageId))
        .done( function (data) {
            $("#noImagePanel").hide(); // in case this is the first file.
            createCard(data);
        });
}

function createCard(data){
    var imageLink = urlPrefix + data.relatedType + "/" + data.relatedId + "/" + data.id + "." + data.suffix;
    var newFileCard = template.clone();
    newFileCard.data("fileId", data.id);
    newFileCard.find("img[data-role='image']").attr('src', imageLink);
    newFileCard.find("a[data-role='linkToFull']").attr('href', imageLink);
    newFileCard.find("[data-role='credit']").val(data.credit);
    newFileCard.find("[data-role='updateCredit']").click(function() {
        updateFileCaptions(newFileCard);
    });
    newFileCard.find("[data-role='deleteFile']").click(function() {
        deleteFile(newFileCard);
    });
    fileCardCtnr.append(newFileCard);

    // second chance for loading image, in case the AJAX stuff is faster
    // than the filesystem on the server.
    var attempts = 0;
    var imageLoader = function(){
        attempts = attempts+1;
        fetch( imageLink ).then( function(response){
            if ( attempts > 10 ) return; // give up

            if (response.status===200) {
                newFileCard.find("img[data-role='image']").attr('src', imageLink);
            } else {
                console.log("reloading image at '" + imageLink + "'");
                window.setTimeout(imageLoader, 1000);
            }
        });
    };
    window.setTimeout(imageLoader, 1000);
}

function updateFileCaptions(fileCard) {
    //id, captions
    var update = {};
    eachLang(function (lang) {
        update[lang] = fileCard.find("[data-role='credit']").val();
    });
    var msgDiv = Informationals.showBackgroundProcess("Updating file");
    var call = beRoutes.controllers.FilesCtrl.apiUpdateCaption(fileCard.data("fileId"));
    return $.ajax({ url: call.url,
        type: call.type,
        data: JSON.stringify(update),
        dataType: "json",
        contentType: "application/json; charset=utf-8"
    }).done(function(){
        msgDiv.success();
    }).always(function(){
        msgDiv.dismiss();
    }).fail( function(req, status, error){
        if ( req.readyState === 4 ) {
            // don't fire if we're navigating away from the page.
            console.log("Error");
            console.log( req );
            console.log( status );
            console.log( error );
            Informationals.show( Informationals.makeDanger("Error updating knesset member", status + "\n" + error));
        }
    });
}

function fileUploadComplete(json) {
    loadSingleFile(json.id);
    hideFileEditor();
}

function deleteCard(fileCard) {
    fileCard.remove();
}

function deleteFile(fileCard) {
    var dialog = makeShowDeleteDialogWithCallback(beRoutes.controllers.FilesCtrl.deleteFile, "file", deleteCard);
    dialog(fileCard.data("fileId"), fileCard.find("[data-role='credit']").val(), fileCard);
    $("#fileEditorPanel").slideDown();
}

function preProcessFormAndSend() {
    //TODO error if name is empty
    var fieldsNames = ["id", "name", "gender", "isActive", "webPage", "partyId"];
    var dataObj = {};
    fieldsNames.forEach(function (e, i) {
        dataObj[e] = $("#" + e).val();
    });
    var updateKmCall = beRoutes.controllers.KnessetMemberCtrl.editKM();
    $.ajax({
        url:updateKmCall.url,
        type:updateKmCall.type,
        data: JSON.stringify(dataObj),
        dataType: "json",
        contentType: "application/json; charset=utf-8"
    }).done(function(data, status, xhr) {

    });
    return false;
}
$('#editKMForm').submit(function() {
    var choosenParty = $("#parties option:selected").val();
    $("#partyId").val(choosenParty);

});

function deleteKM(id){
    swal({
        title:"Are you sure you want to delete this knesset member?",
        icon:"warning",
        buttons: {
            cancel:true,
            confirm:true
        }
    }).then( function(willDelete){
        if(willDelete) {
            new Playjax(beRoutes)
                .using(function(c){
                    return c.KnessetMemberCtrl.deleteKM(id);}).fetch()
                .then( function(res){
                    if (res.ok) {
                        document.location=beRoutes.knessetMemberCtrl.showKms();
                    } else {
                        Informationals.makeDanger("Deletion of knesset member "+ id +" failed", "See server log for details", 1500).show();
                    }
                });
        }
    });
}

function setupContactOptions(kmId) {
    platformList = document.getElementById("platformList");
    optionTemplate = document.getElementById("optionTemplate");
    optionTemplate.id = undefined;
    optionTemplate.remove();
    if(kmId !== -1) {
        $.ajax(beRoutes.controllers.KnessetMemberCtrl.getContactOptionForKm(kmId))
            .done( function (data) {
                $("#loadingContactOptions").remove();
                if ( !data ) {
                    $("#noImagePanel").slideDown();
                } else {
                    for (var di in data){
                        addPlatform(data[di].platform, data[di].details, data[di].note, data[di].title);
                    }
                }
            });
    } else addPlatform();
}

function addPlatform(platform, details, note, title) {
    var newPlatform = optionTemplate.cloneNode(true);
    if(platform) {
        $(newPlatform).find("select").val(platform);
        $(newPlatform).find("input [placeholder='details']").val(details);
        $(newPlatform).find("input [placeholder='note']").val(note);
        $(newPlatform).find("input [placeholder='title']").val(title);
    }
    platformList.appendChild(newPlatform);
}

function deleteRow(emt) {
    $(emt).closest("li").remove();
}

function updateContactOptions(id){
    var data = [];
    var values = ["title", "details", "note"];
    $(platformList).find("li").each( function(idx, emt) {
        var option = {};
        values.forEach(function (val) {
            $(emt).find("label [name="+ val +"]").each( function() {
                option[val] = $(emt).find("input").val();
            });
        });
        data.push(option);
    });
    console.log("before ajax");
    console.log(data);
    $.ajax({
        url: beRoutes.controllers.KnessetMemberCtrl.updateContactOption(Number(id)).url,
        type: "POST",
        data: JSON.stringify(data),
        dataType: "json",
        contentType: "application/json; charset=utf-8"
    }).done(function (data, status, xhr) {
        console.log(data);
        if (dataObj.id === -1) {
            // window.location = jsRoutes.controllers.RoutesCtrl.showRouteEditorPage(data.id).url;
        } else {
            // window.location = jsRoutes.controllers.RoutesCtrl.index(null, null, null, null).url;
        }
    });
}