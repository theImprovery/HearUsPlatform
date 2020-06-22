/* jshint esversion:6 */

const FrontPageEditor = (function(){

    let textChanged = false;

    function setup() {
        // make the editor
        const $editor = $("#frontPageText");
        $editor.summernote({
            height: 500,
            fontSizes: ['8', '9', '10', '12', '14', '16', '24', '36', '48' , '64', '82', '150'],
            // styleTags: [
            //     { title: 'כותרת ראשית', tag: 'h1', className: '', value: 'h1' },
            //     { title: 'כותרת משנה', tag: 'h2', className: '', value: 'h2' },
            //     { title: 'כותרת', tag: 'h3', className: '', value: 'h3' },
            //     { title: 'כותרת קטנה', tag: 'h4', className: '', value: 'h4' }
            // ],
            toolbar: [
                ['font', ['fontname', 'fontsize', 'forecolor','backcolor']],
                ['style', ['bold', 'italic', 'underline', 'clear', 'strikethrough']],
                ['color', ['color']],
                ['para', ['ul', 'ol', 'paragraph', 'left', 'center', 'right']],
                ['insert', ['link','picture','video','hr']],
                ['misc',['fullscreen','codeview']]
            ]
        });

        $editor.on("summernote.change", function(){
            textChanged = true;
        });

        // warn before moving to other page

        window.addEventListener('beforeunload', confirmLeave);

        textChanged = false;
    }

    function confirmLeave(event) {
        if ( textChanged ) {
            // Cancel the event as stated by the standard.
            event.preventDefault();
            // Chrome requires returnValue to be set.
            event.returnValue = '';
        }
    }

    function savePage() {
        // UI on
        const inf = Informationals.showBackgroundProcess(polyglot.t("frontPageEditor.saving"));
        // get the HTML
        const payload = document.getElementById("frontPageText").value;
        // PUT
        new Playjax(beRoutes)
            .using( c=>c.CampaignAdminCtrl.apiPutFrontPageData() )
            .fetch( payload )
            .then( res => {
                if ( res.ok ) {
                    textChanged = false;
                    inf.success();
                } else {
                    inf.dismiss();
                    Informationals.makeDanger(polyglot.t("frontPageEditor.failed")).show();
                    console.log( res );
                }
            });
    }

    return {
        setup: setup,
        savePage: savePage,
        isChanged:function(){ return textChanged; }
    };
})();