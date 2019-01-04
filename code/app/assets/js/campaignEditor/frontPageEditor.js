/*jshint esversion: 6 */

function frontPageEditorSetup(){
    $("#bodyText").summernote({
        height: 500,
        toolbar: [
            // [groupName, [list of button]]
            ['font', ['fontname', 'fontsize', 'forecolor','backcolor']],
            ['style', ['bold', 'italic', 'underline', 'clear', 'strikethrough']],
            ['color', ['color']],
            ['para', ['ul', 'ol', 'paragraph', 'left', 'center', 'right']],
            ['insert', ['link','picture','video','hr']],
            ['misc',['fullscreen','codeview']]
        ],
        fontNames: ['Alef', 'Arial', 'Courier New'],
        fontNamesIgnoreCheck:['Alef']

    });

}

function prepareForm() {
    var groupValue = positionStrings.map( (pos)=> document.getElementById("group"+pos).value ).join("\t");
    document.getElementById("groupLabels").value = groupValue;
    var mkValues = [];
    for ( var g in genderStrings ) {
        for ( var p in positionStrings ) {
            var key = genderStrings[g]+positionStrings[p]+"Label";
            mkValues.push( document.getElementById(key).value );
        }
    }
    document.getElementById("kmLabels").value = mkValues.join("\t");

    console.log( document.getElementById("kmLabels").value );
}