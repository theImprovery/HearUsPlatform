var campaignersList, campaignerTemplate;
var campaignerName;
$(document).ready( function() {
    campaignersList = document.getElementById("campaignersList");
    campaignerTemplate = document.getElementById("campaignerTemplate");
    campaignerTemplate.removeAttribute("id");
    campaignerTemplate.remove();
    campaignerName = document.getElementById("campaignerName");
});

function searchCampaigner(input) {
    var searchStr = input.value;
    while (campaignersList.hasChildNodes()) {
        campaignersList.removeChild(campaignersList.lastChild);
    }
    $("#noCampaigners").slideUp();
    if(searchStr !== ""){
        new Playjax(beRoutes)
            .using(function (c) {
                return c.CampaignAdminCtrl.getCampaigners(searchStr);
            })
            .fetch()
            .then( function (res) {
                if (res.ok) {
                    res.json().then(function (json){
                        if( json.length === 0 ) {
                            $("#noCampaigners").slideDown();
                        } else {
                            json.forEach(function (campaigner) {
                                var newCamp = campaignerTemplate.cloneNode(true);
                                $(newCamp).find("label").text(campaigner.name);
                                $(newCamp).find("label")[0].dataset.camId = campaigner.id;
                                campaignersList.appendChild(newCamp);
                            });
                        }
                    });
                } else {
                    Informationals.makeWarning("Something went wrong", "try again", 1500).show();
                }
            });
    } else{
        $("#noCampaigners").slideDown();
    }
}

function changeCampaigner(elm) {
    campaignerName.value = elm.innerText;
    campaignerName.dataset.camId = elm.dataset.camId;
    console.log("elm", elm.dataset.camId);
    $("#campaigner")[0].value = elm.dataset.camId;
}

$("#campaignForm").submit(function () {
   if(campaignerName.dataset.camId === ""){
       $("#noCampaigner").slideDown();
       return false;
   }
});