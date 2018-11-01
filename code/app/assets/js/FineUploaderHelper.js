var FineUploaderHelper = (function(){

    /**
     *
     * @param options { elementId:String, onComplete:function,subjectId:String}
     * @return Options for fineUploader UI component
     */
    function createOptions(options) {
        var call = beRoutes.controllers.FilesCtrl.apiAddFile(options.subjectId);
        var retVal = {autoUpload: false,
            thumbnails: {
                placeholders: {
                    waitingPath: beRoutes.controllers.Assets.versioned("vnd/fineUploader/placeholders/waiting-generic.png").url,
                    notAvailablePath: beRoutes.controllers.Assets.versioned("vnd/fineUploader/placeholders/not_available-generic.png").url
                }
            },
            element: document.getElementById(options.elementId),
            validation:{
                itemLimit:1
            },
            request: {
                endpoint:call.url,
                method: call.method,
                forceMultipart:true,
                customHeaders:{"Csrf-Token": document.getElementById("Playjax_csrfTokenValue").innerText}
            },
            callbacks: {
                onComplete: options.onComplete,
                onError: function(id, name, errorReason, xhr){
                    console.error("Error loading file " + name + " (" + id + ")" +
                                    ": " + errorReason );
                    console.log( xhr );
                }
            }
        };

        return retVal;
    }

    function connectButton( uploader, uploaderElementId ) {
        $("#" + uploaderElementId) .find("button[data-role='upload-button']").click( function(){
            uploader.uploadStoredFiles();
        });
    }

    function createUploader(options) {
        var uploader = new qq.FineUploader(createOptions(options));
        connectButton( uploader, options.elementId );
        return uploader;
    }

    return {
        create: createUploader,
        createOptions:createOptions,
        connectButton: connectButton
    };

})();