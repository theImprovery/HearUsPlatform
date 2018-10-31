var partiesList;
function editParty(sender) {
    var $parent = $(sender.parentNode);
    $($parent.find("[name=saveBtn]")[0]).removeClass("hidden");
    $(sender).addClass("hidden");
    $parent.find("a").addClass("hidden");
    $parent.find("[name=webPage]").removeClass("hidden");
    $parent.find("[name=name]").prop('disabled', false);
}

function deleteParty(id) {
    swal({
        title:"Are you sure you want to delete this party?",
        icon:"warning",
        buttons: {
            cancel:true,
            confirm:true
        }
    }).then( function(willDelete){
        if(willDelete) {
            new Playjax(beRoutes)
                .using(function(c){
                    return c.KnessetMemberCtrl.deleteParty(id);}).fetch()
                .then( function(res){
                    if (res.ok) {
                        document.location.reload();
                    } else {
                        Informationals.makeDanger("Deletion of party "+ id +" failed", "Maybe there is a Knesset member linked to the party", 3000).show();
                    }
                });
        }
    });
}

function addRow(){
    if(!partiesList){
        partiesList = document.createElement('ul');
        partiesList.setAttribute('id', 'partiesList');
        // var partiesCtnr = document.createElement('div');
        // partiesCtnr.setAttribute('id', 'partiesCtnr');
        // console.log("not", document.getElementById("partiesCtnrs"));
        $(".card.noData").slideUp();
    }
    var li = document.createElement("li");
    var i_id = createElementWithAttr("input", {"type":"hidden", "name":"id"});
    var i_name = createElementWithAttr("input", {"type":"text", "name":"name"});
    var i_web = createElementWithAttr("input", {"type":"text", "name":"webPage"});
    var webLink = createElementWithAttr("a", {"href":"", "class":"hidden", "target":"_blank"});
    var saveIcon = createElementWithAttr("i", {"class": "fa fa-save"});
    var saveButton = createElementWithAttr("button",
        {"type":"button", "name":"saveBtn", "class":"btn btn-sm btn-primary", "onclick":"saveParty(this);"});
    saveButton.appendChild(saveIcon);
    var editIcon = createElementWithAttr("i", {"class": "fa fa-edit"});
    var editButton = createElementWithAttr("button",
        {"type":"button", "name":"editBtn", "class":"btn btn-sm btn-primary hidden", "onclick":"editParty(this);"});
    editButton.appendChild(editIcon);
    var deleteIcon = createElementWithAttr("i", {"class": "fa fa-trash"});
    var deleteButton = createElementWithAttr("button",
        {"type":"button", "name":"deleteBtn", "class":"btn btn-sm btn-danger hidden"});
    deleteButton.appendChild(deleteIcon);
    // var csrf = document.getElementById("Playjax_csrfTokenValue").innerText;
    // var csrf_input = createElementWithAttr("input", {"style":"display:none", "value":csrf, "name":"csrfToken"});
    var formChild = [i_id, i_name, i_web, webLink, saveButton, editButton, deleteButton];
    for (var i = 0; i < formChild.length; i++) {
        li.appendChild(formChild[i]);
    }
    partiesList.appendChild(li);
    var div = document.getElementById('partiesCtnr');
    div.appendChild(partiesList);

}

function createElementWithAttr(name, attr) {
    var emt = document.createElement(name);
    jQuery.each(attr, function(i, val) {
        emt.setAttribute(i, val);
    });
    return emt;
}

function saveParty(sender) {
    var $parentLi = $(sender.parentNode);
    var data = {};
    data.id = Number($parentLi.find("[name=id]")[0].value);
    data.name = $parentLi.find("[name=name]")[0].value;
    data.webPage = $parentLi.find("[name=webPage]")[0].value;
    console.log("data", data);
    patch(data, sender);
}

function patch(data, sender) {
    var msgDiv = Informationals.showBackgroundProcess("Updating..");
    new Playjax(beRoutes)
        .using(function (c) {
            return c.KnessetMemberCtrl.updateParty();
        })
        .fetch(data)
        .then( function (res) {
            if (res.ok) {
                msgDiv.success();
                res.json().then(function (json){
                    var $parent = $(sender.parentNode);
                    $($parent.find("[name=saveBtn]")[0]).addClass("hidden");
                    $($parent.find("[name=deleteBtn]")[0]).removeClass("hidden");
                    console.log("dlele", $parent.find("[name=deleteBtn]"));
                    $($parent.find("[name=deleteBtn]")[0]).attr("onclick", "deleteParty("+ json.id +");");
                    $($parent.find("[name=editBtn]")[0]).removeClass("hidden");
                    $parent.find("[name=webPage]")[0].value=json.webPage;
                    $($parent.find("[name=webPage]")[0]).addClass("hidden");
                    $parent.find("a")[0].innerText = json.webPage;
                    $parent.find("a").prop("href", json.webPage);
                    $parent.find("a").removeClass("hidden");
                    $parent.find("[name=name]").prop('disabled', true);
                });
            } else {
                msgDiv.dismiss();
                Informationals.makeWarning("Update Party " + data.name, "Failed", 1500);
            }
        });
}

$(document).ready( function() {
    partiesList = document.getElementById("partiesList");
});