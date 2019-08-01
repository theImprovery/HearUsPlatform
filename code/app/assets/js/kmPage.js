var optionTemplate, platformList;
$(document).ready( function() {
    var kmId = $("#id").val();
    // loadFiles(kmId);
    setupContactOptions(kmId);
});

function showAddImage() {
    $("#fileEditorPanel").slideDown();
    $("#noImagePanel").slideUp();
}

function hideFileEditor() {
    $("#fileEditorPanel").slideUp();
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
                createCard(data);
            }
        });
}

function loadSingleFile(imageId) {
    $.ajax(beRoutes.controllers.FilesCtrl.apiGetImage(imageId))
        .done( function (data) {
            console.log("image data", data);
            $("#noImagePanel").hide(); // in case this is the first file.
            createCard(data);
        });
}

function createCard(data){
    var imageLink = urlPrefix + data.kmId;
    var newFileCard = template.clone();
    newFileCard.data("fileId", data.id);
    newFileCard.find("img[data-role='image']").attr('src', imageLink);
    newFileCard.find("a[data-role='linkToFull']").attr('href', imageLink);
    newFileCard.find("[data-role='credit']").val(data.credit);
    newFileCard.find("[data-role='updateCredit']").click(function() {
        updateFileCredits(newFileCard);
    });
    newFileCard.find("[data-role='deleteFile']").click(function() {
        deleteFile(newFileCard);
    });
    fileCardCtnr.append(newFileCard);

    // second chance for loading image, in case the AJAX stuff is faster
    // than the filesystem on the server.
    var attempts = 0;
    var imageLoader = function(){
        if ( true ) return; //todo remove
        attempts = attempts+1;
        fetch( imageLink ).then( function(response){
            if ( attempts > 10 ) return; // give up

            if (response.status===200) {
                newFileCard.find("img[data-role='image']").attr('src', imageLink);
                Informationals.loader.dismiss();
            } else {
                console.log("reloading image at '" + imageLink + "'");
                window.setTimeout(imageLoader, 1000);
            }
        });
    };
    window.setTimeout(imageLoader, 1000);
}

function updateFileCredits(fileCard) {
    //id, credit
    var update = fileCard.find("[data-role='credit']").val();
    var msgDiv = Informationals.showBackgroundProcess("Updating file");
    new Playjax(beRoutes)
        .using(function (c) {
            return c.FilesCtrl.apiUpdateCredit(fileCard.data("fileId"));})
        .fetch(update)
        .then(function (res) {
            if (res.ok){
                msgDiv.success();
            } else {
                alert("baa");
            }
        });
}

function fileUploadComplete(json) {
    loadSingleFile(json.record.id);
    hideFileEditor();
}

function deleteCard(fileCard) {
    fileCard.remove();
    $("#noImagePanel").slideDown();
}

function deleteFile(fileCard) {
    // var dialog = makeShowDeleteDialogWithCallback(beRoutes.controllers.FilesCtrl.deleteFile, "file", deleteCard);
    // dialog(fileCard.data("fileId"), fileCard.find("[data-role='credit']").val(), fileCard);
    // $("#fileEditorPanel").slideDown();
    swal({
        title:polyglot.t("km.image.delete"),
        icon:"warning",
        buttons: {
            cancel:polyglot.t("cancel"),
            confirm:polyglot.t("confirm")
        }
    }).then( function(willDelete){
        if(willDelete) {
            // Informationals.loader("Deleting file..");
            new Playjax(beRoutes)
                .using(function(c){
                    return c.FilesCtrl.deleteFile(fileCard.data("fileId"));}).fetch()
                .then( function(res){
                    if (res.ok) {
                        deleteCard(fileCard);
                        // Informationals.loader.dismiss();
                    } else {
                        Informationals.makeDanger(polyglot.t("km.image.failed"), polyglot.t("server_logs_details"), 1500).show();
                    }
                });
        }
    });
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
    var chosenParty = $("#parties option:selected").val();
    $("#partyId").val(chosenParty);

});

function deleteKM(id){
    swal({
        title:polyglot.t("km.delete"),
        icon:"warning",
        buttons: {
            cancel:polyglot.t("cancel"),
            confirm:polyglot.t("confirm")
        }
    }).then( function(willDelete){
        if(willDelete) {
            new Playjax(beRoutes)
                .using(function(c){
                    return c.KnessetMemberCtrl.deleteKM(id);}).fetch()
                .then( function(res){
                    if (res.ok) {
                         window.location = beRoutes.controllers.KnessetMemberCtrl.showKms().url;
                    } else {
                        Informationals.makeDanger(polyglot.t("km.failed"), polyglot.t("server_logs_details"), 1500).show();
                    }
                });
        }
    });
}

function setupContactOptions(kmId) {
    platformList = document.getElementById("platformList");
    optionTemplate = document.getElementById("optionTemplate");
    delete optionTemplate.id;
    optionTemplate.remove();
    if(kmId !== -1) {
        $.ajax(beRoutes.controllers.KnessetMemberCtrl.getContactOptionForKm(kmId))
            .done( function (data) {
                $("#loadingContactOptions").remove();
                if ( !data ) {
                    $("#noContact").slideDown();
                } else {
                    for (var di in data){
                        addPlatform(data[di]);
                    }
                }
            });
    } else addPlatform();
}

function updateEditorField(select) {
    var value = $(select).val();
    var $parentLi = $(select).closest("li");
    var $detailsContainer = $parentLi.find("label[data-role='detailsCtnr']");
    var textField = $detailsContainer.find("input");
    var textArea = $detailsContainer.find("textarea");

    if ( value === "Mail" ) {
        textArea.show();
        textField.hide();
    } else {
        textArea.hide();
        textField.show();
    }
}

function addPlatform(contactOption) {
    var newConOptLi = optionTemplate.cloneNode(true);
    if(contactOption) {
        var $newConOptLi = $(newConOptLi);
        $newConOptLi.find("input[name='contactOptionId']").val(contactOption.id);
        $newConOptLi.find("select").val(contactOption.platform);
        $newConOptLi.find("input[name='details']").val(contactOption.details);
        $newConOptLi.find("input[name='note']").val(contactOption.note);
        $newConOptLi.find("input[name='title']").val(contactOption.title);
        $newConOptLi.find("textarea").val(contactOption.details);
        if ( contactOption.platform === "Mail" ) {
            $newConOptLi.find("textarea").show();
            $newConOptLi.find("input[name='details']").hide();
        }
    }
    platformList.appendChild(newConOptLi);
}

function deleteRow(emt) {
    $(emt).closest("li").remove();
}

function updateContactOptions(id){
    var data = [];
    var values = ["contactOptionId", "title", "details", "note"];
    $(platformList).find("li").each( function(idx, emt) {
        var option = {};
        values.forEach(function (val) {
            var $curLi = $(emt);
            $curLi.find("label [name="+ val +"]").each( function() {
                option[val] = $(this).val();
            });
            option.platform = $curLi.find("select[name='contactType']").val();
            if (option.platform === "Mail") {
                option.details = $curLi.find("textarea").val();
            }
        });
        option.kmId = Number($("#id").val());
        if ( Number(option.contactOptionId) > 0 ) {
            option.id = Number(option.contactOptionId);
        } else {
            option.id = 0;
        }
        delete option.contactOptionId;
        data.push(option);
    });
    new Playjax(beRoutes)
                .using(function (c) {
                    return c.KnessetMemberCtrl.updateContactOption(Number(id));
                })
                .fetch(data)
                .then( function (res) {
                    if (res.ok) {
                        Informationals.makeSuccess(polyglot.t("km.contact.updated"), "", 1000).show();
                    } else {
                        Informationals.makeWarning(polyglot.t("went_wrong"), polyglot.t("km.contact.one_platform"), 1500).show();
                    }
                });
}