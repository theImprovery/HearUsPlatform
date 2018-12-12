function patchDetails() {
    var data = {};
    var fields = ["title", "subtitle", "website", "themeData", "contactEmail"];
    fields.forEach(function (value) { data[value] = document.getElementById(value).value; });
    data.id = Number(document.getElementById("id").value);
    var msgDiv = Informationals.showBackgroundProcess("Updating..");
    new Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignCtrl.updateDetails();
        })
        .fetch(data)
        .then( function (res) {
            if (res.ok) {
                msgDiv.success();
            } else {
                msgDiv.dismiss();
                Informationals.makeWarning("Update Campaign " + data.name, "Failed", 1500);
            }
        });
}

function deleteCampaign(id) {
    swal({
        title:"Are you sure you want to delete this campaign?",
        icon:"warning",
        buttons: {
            cancel:true,
            confirm:true
        }
    }).then( function(willDelete){
        if(willDelete) {
            new Playjax(beRoutes)
                .using(function(c){
                    return c.CampaignCtrl.deleteCampaign(id);}).fetch()
                .then( function(res){
                    if (res.ok) {
                        window.location = beRoutes.controllers.CampaignCtrl.showCampaigns().url;
                    } else {
                        Informationals.makeDanger("Deletion of campaign "+ id +" failed", "See server log for details", 1500).show();
                    }
                });
        }
    });
}

function deleteRow(emt) {
    $(emt).closest("li").remove();
}

var labelTemplate, labelList;
var messageTemplate, messageList;
var smTemplate, smList;
$(document).ready( function() {
    var camId = $("#id").val();
    labelHandler(camId);
    messageHandler(camId);
    socialMediaHandler(camId);
});

function labelHandler(camId) {
    labelList = document.getElementById("labelList");
    labelTemplate = document.getElementById("labelTemplate");
    $(labelTemplate).removeAttr("id");
    labelTemplate.remove();
    $.ajax(beRoutes.controllers.CampaignCtrl.getLabelText(camId))
        .done( function (data) {
            $("#loadingLabels").remove();
            if ( data.length === 0 ) {
                $("#noLabels").slideDown();
            } else {
                for (var di in data){
                    addLabel(data[di].position, data[di].gender, data[di].text);
                }
            }
        });
}

function addLabel(position, gender, label) {
    var newLabel = labelTemplate.cloneNode(true);
    if(position) {
        $(newLabel).find("select").val(position);
        $(newLabel).find("#l-"+gender)[0].checked = true;
        $(newLabel).find("input[name='label']").val(label);
    }
    labelList.appendChild(newLabel);
}

function updateLabels(id) {
    var data = [];
    $(labelList).find("li").each( function(idx, emt) {
        var option = {};
        option.camId = id;
        option.position = $(emt).find("#lPosition option:selected").val();
        option.text = $(emt).find("label [name=label]").val();
        option.gender = $(emt).find("input[name=lGender]:checked").val();
        console.log("option ", option);
        data.push(option);
    });
    new Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignCtrl.updateLabels();
        })
        .fetch(data)
        .then( function (res) {
            if (res.ok) {
                Informationals.makeSuccess("Updated Labels", "", 1000).show();
            } else {
                Informationals.makeWarning("Something went wrong", "Each position and gender can appear once", 1500).show();
            }
        });
}

function messageHandler(camId) {
    messageList = document.getElementById("messagesList");
    messageTemplate = document.getElementById("messageTemplate");
    $(messageTemplate).removeAttr("id");
    messageTemplate.remove();
    $.ajax(beRoutes.controllers.CampaignCtrl.getMessages(camId))
        .done( function (data) {
            $("#loadingMessages").remove();
            if ( data.length === 0 ) {
                $("#noMessages").slideDown();
            } else {
                for (var di in data){
                    addMessage(data[di].platform, data[di].position, data[di].gender, data[di].text);
                }
            }
        });
}

function addMessage(platform, position, gender, message) {
    var newMessage = messageTemplate.cloneNode(true);
    if(platform) {
        $(newMessage).find("select")[0].value = platform;
        $(newMessage).find("select")[1].value = position;
        $(newMessage).find("#m-"+gender)[0].checked = true;
        $(newMessage).find("input[name='message']").val(message);
    }
    messageList.appendChild(newMessage);
}

function updateMessages(id) {
    var data = [];
    $(messageList).find("li").each( function(idx, emt) {
        var option = {};
        option.camId = id;
        option.platform = $(emt).find("#mPlatform option:selected").val();
        option.position = $(emt).find("#mPosition option:selected").val();
        option.text = $(emt).find("label [name=message]").val();
        option.gender = $(emt).find("input[name=mGender]:checked").val();
        console.log("option ", option);
        data.push(option);
    });
    new Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignCtrl.updateMessages();
        })
        .fetch(data)
        .then( function (res) {
            if (res.ok) {
                Informationals.makeSuccess("Updated Messages", "", 1000).show();
            } else {
                Informationals.makeWarning("Something went wrong", "Each platform, position and gender can appear once", 1500).show();
            }
        });
}

function socialMediaHandler(camId) {
    smList = document.getElementById("smList");
    smTemplate = document.getElementById("smTemplate");
    $(smTemplate).removeAttr("id");
    smTemplate.remove();
    $.ajax(beRoutes.controllers.CampaignCtrl.getSocialMedia(camId))
        .done( function (data) {
            $("#loadingSm").remove();
            if ( data.length === 0 ) {
                $("#noSm").slideDown();
            } else {
                for (var di in data){
                    addSm(data[di].name, data[di].service, data[di].id);
                }
            }
        });
}

function addSm(name, service, smId) {
    var newSm = smTemplate.cloneNode(true);
    if(name){
        $(newSm).find("input[name=smId]").val(smId);
        $(newSm).find("input[name=smName]").val(name);
        $(newSm).find("input[name=smService]").val(service);
    }
    smList.appendChild(newSm);
}

function updateSm(id) {
    var data = [];
    $(smList).find("li").each( function(idx, emt) {
        var option = {};
        option.id = Number($(emt).find("[name=smId]").val());
        option.camId = id;
        option.name = $(emt).find("label [name=smName]").val();
        option.service = $(emt).find("label [name=smService]").val();
        data.push(option);
    });
    new Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignCtrl.updateSocialMedia();
        })
        .fetch(data)
        .then( function (res) {
            if (res.ok) {
                Informationals.makeSuccess("Updated Social media", "", 1000).show();
            } else {
                Informationals.makeWarning("Something went wrong", "Try again", 1500).show();
            }
        });
}

function changePosition(emt) {
    var data = {};
    data.camId = Number($("#id").val());
    data.kmId = Number(emt.id.split("-")[0]);
    data.position = emt.id.split("-")[1];
    new Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignCtrl.updatePosition();
        })
        .fetch(data)
        .then( function (res) {
            if (res.ok) {
                Informationals.makeSuccess("Updated Position", "", 1000).show();
            } else {
                Informationals.makeWarning("Something went wrong", "try again", 1500).show();
            }
        });
}

function showCleanDialog() {
    $("#actionModal").modal();
}

var allKms = $("#kmsPositionList");
function filterKmsList(searchStr) {
    allKms.find("li").each(function(idx, emt    ) {
       if((emt.dataset.kmName.indexOf(searchStr) !== -1) || (emt.dataset.partyName.indexOf(searchStr) !== -1)) {
           emt.style.display = "block";
       } else emt.style.display = "none";
    });
}