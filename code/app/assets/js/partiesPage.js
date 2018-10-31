var partiesList;

var prevPartyData = {};

// returns party data from an
// li element in a jquery.
function getPartyData( $li ) {
    return {
        id: Number($li.find("[name=id]").val()),
        name: $li.find(".partyName")[0].innerText,
        webPage: $li.find(".partyWebPage")[0].innerText
    };
}

function setPartyData( $li, data ){
    $li.find("[name=id]")[0].value=data.id;
    $li.find(".partyName")[0].innerText = data.name;
    $li.find(".partyWebPage")[0].innerText = data.webPage;
    $li.find(".partyWebPage").attr("href", data.webPage );
}

function setEditable( $li, shouldBeEditable ) {
    $li.find("a").attr("contentEditable", shouldBeEditable);
    $li.find("div.partyName").attr("contentEditable", shouldBeEditable);

    if ( shouldBeEditable ) {
        $li.find("[name=saveBtn]").removeClass("hidden");
        $li.find("[name=cancelEditBtn]").removeClass("hidden");
        $li.find("[name=editBtn]").addClass("hidden");

    } else {
        $li.find("[name=saveBtn]").addClass("hidden");
        $li.find("[name=cancelEditBtn]").addClass("hidden");
        $li.find("[name=editBtn]").removeClass("hidden");
    }
}

function editParty(sender) {
    var $parent = $(sender.parentNode);
    var partyData = getPartyData($parent);
    prevPartyData[partyData.id] = partyData;
    setEditable( $parent, true );
}

function cancelEdit(sender) {
    var $parent = $(sender.parentNode);
    var partyId = getPartyData($parent).id;
    setEditable( $parent, false );
    setPartyData( $parent, prevPartyData[partyId] );
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
        partiesList.addClass('editableList');
        var div = document.getElementById('partiesCtnr');
        div.appendChild(partiesList);

        $(".card.noData").slideUp();
    }
    var idHidden = createElementWithAttr("input", {"type":"hidden", "name":"id"});
    var partyName = createElementWithAttr("div", {"class":"partyName"});
    var webLink = createElementWithAttr("a", {"href":"", "class":"partyWebPage", "target":"_blank"});
    var cancelIcon = createElementWithAttr("i", {"class": "fa fa-close"});
    var cancelEditButton = createElementWithAttr("button",
        {"type":"button", "name":"cancelEditBtn", "class":"btn btn-sm btn-secondary", "onclick":"cancelEdit(this);"});
    cancelEditButton.appendChild(cancelIcon);

    var saveIcon = createElementWithAttr("i", {"class": "fa fa-save"});
    var saveButton = createElementWithAttr("button",
        {"type":"button", "name":"saveBtn", "class":"btn btn-sm btn-primary", "onclick":"saveParty(this);"});
    saveButton.appendChild(saveIcon);

    var editIcon = createElementWithAttr("i", {"class": "fa fa-edit"});
    var editButton = createElementWithAttr("button",
        {"type":"button", "name":"editBtn", "class":"btn btn-sm btn-default hidden", "onclick":"editParty(this);"});
    editButton.appendChild(editIcon);

    var deleteIcon = createElementWithAttr("i", {"class": "fa fa-trash"});
    var deleteButton = createElementWithAttr("button",
        {"type":"button", "name":"deleteBtn", "class":"btn btn-sm btn-danger hidden"});
    deleteButton.appendChild(deleteIcon);

    var liElements = [idHidden, partyName, webLink, editButton, cancelEditButton, saveButton, deleteButton];
    var li = document.createElement("li");
    for (var i = 0; i < liElements.length; i++) {
        li.appendChild(liElements[i]);
        li.appendChild(createFiller());
    }
    partiesList.appendChild(li);
    setPartyData($(li), {id:0, name:"name", webPage:"http://party.org.il"});
    setEditable($(li),true);
}
function createFiller() {
    return document.createTextNode("\n                 ");
}
function createElementWithAttr(name, attr) {
    var emt = document.createElement(name);
    Object.keys(attr).forEach(function(key) {
        emt.setAttribute(key, attr[key]);
    });
    return emt;
}

function saveParty(sender) {
    var $parentLi = $(sender.parentNode);
    var data = getPartyData($parentLi);
    setEditable($parentLi, false);
    patch(data, sender);
}

function patch(data, sender) {
    var msgDiv = Informationals.showBackgroundProcess("Updating..");
    var $parent = $(sender.parentNode);
    new Playjax(beRoutes)
        .using(function (c) {
            return c.KnessetMemberCtrl.updateParty();
        })
        .fetch(data)
        .then( function (res) {
            if (res.ok) {
                msgDiv.success();
                res.json().then(function (json){
                    setPartyData($parent, json);
                    prevPartyData[json.id]=undefined;
                    $parent.find("[name=deleteBtn]").slideDown();
                });

            } else {
                msgDiv.dismiss();
                Informationals.makeWarning("Update Party " + data.name, "Failed", 1500);
                setPartyData($parent, prevPartyData[data.id]);
            }
        });
}

$(document).ready( function() {
    partiesList = document.getElementById("partiesList");
});