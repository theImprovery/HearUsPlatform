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
    $.ajax(beRoutes.controllers.CampaignMgrCtrl.getLabelText(camId))
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
            return c.CampaignMgrCtrl.updateLabels();
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
    $.ajax(beRoutes.controllers.CampaignMgrCtrl.getMessages(camId))
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
            return c.CampaignMgrCtrl.updateMessages();
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
    $.ajax(beRoutes.controllers.CampaignMgrCtrl.getSocialMedia(camId))
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
            return c.CampaignMgrCtrl.updateSocialMedia();
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
