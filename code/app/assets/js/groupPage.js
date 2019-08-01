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
        title:polyglot.t("groups.page.delete"),
        icon:"warning",
        buttons: {
            cancel:polyglot.t("cancel"),
            confirm:polyglot.t("confirm")
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
                        Informationals.makeDanger(polyglot.t("groups.page.failed"), polyglot.t("server_logs_details"), 1500).show();
                    }
                });
        }
    });
}