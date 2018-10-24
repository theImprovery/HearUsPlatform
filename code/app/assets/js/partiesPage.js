var partiesList;
function editParty(sender) {
    $(sender.parentNode).find("[type=submit]").removeClass("hidden");
    $(sender).addClass("hidden");
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
                        document.location=beRoutes.knessetMemberCtrl.showParties();
                    } else {
                        Informationals.makeDanger("Deletion of party "+ id +" failed", "See server log for details", 1500).show();
                    }
                });
        }
    });
}

function addRow(){
    if(partiesList){

    } else {
        partiesList = document.createElement('ul');
        partiesList.setAttribute('id', 'partiesList');
        var li = document.createElement("li");
        var form = createElementWithAttr("form", {"action":"@routes.KnessetMemberCtrl.updateParty()", "method":"POST"});
        var i_id = createElementWithAttr("input", {"type":"hidden", "name":"id", "value":""});
        var i_name = createElementWithAttr("input", {"type":"text", "name":"name", "value":""});
        var i_web = createElementWithAttr("input", {"type":"hidden", "name":"webPage", "value":""});
        var webLink = createElementWithAttr("a", {"href":""});
        var saveIcon = createElementWithAttr("i", {"class": "fa fa-save"});
        var saveButton = createElementWithAttr("button",
            {"type":"submit", "class":"btn btn-sm btn-primary"});
        saveButton.appendChild(saveIcon);
        var formChild = [i_id, i_name, i_web, webLink, saveButton];
        for (var i = 0; i < formChild.length; i++) {
            form.appendChild(formChild[i]);
        }
        li.appendChild(form);
        partiesList.appendChild(li);
        var div = document.getElementById('partiesCtnr');
        div.appendChild(partiesList);
    }

}

function createElementWithAttr(name, attr) {
    var emt = document.createElement(name);
    jQuery.each(attr, function(i, val) {
        emt.setAttribute(i, val);
    });
    return emt;
}