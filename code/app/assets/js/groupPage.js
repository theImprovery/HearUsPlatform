function submitForm() {
    var selectedKms = getSelectedKms();
    console.log(selectedKms);
    var asString="";
    for ( var idx in selectedKms ) {
        if ( idx > 0 ) {
            asString = asString + ",";
        }
        asString = asString + selectedKms[idx];
    }
    console.log(asString);
    $("#kmsIds").val(asString);
    $("#editGroupForm")[0].submit();
}

function getSelectedKms() {
    var res = [];
    $('[name=kmSelect]:checkbox:checked').each(function () {
        res.push($(this).prop("id").substring(3));
    });
    return res;
}

function deleteGroup(id) {
    swal({
        title:"Are you sure you want to delete this group?",
        icon:"warning",
        buttons: {
            cancel:true,
            confirm:true
        }
    }).then( function(willDelete){
        if(willDelete) {
            new Playjax(beRoutes)
                .using(function(c){
                    return c.KnessetMemberCtrl.deleteGroup(id);}).fetch()
                .then( function(res){
                    if (res.ok) {
                        window.location = beRoutes.controllers.KnessetMemberCtrl.showGroups().url;
                    } else {
                        Informationals.makeDanger("Deletion of group "+ id +" failed", "See server log for details", 1500).show();
                    }
                });
        }
    });
}