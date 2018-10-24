var FineUploaderHelper = (function(){

    /**
     *
     * @param options { elementId:String, onComplete:function,subjectId:String}
     * @return Options for fineUploader UI component
     */
    function createOptions(options) {
        var retVal = {autoUpload: false,
            thumbnails: {
                placeholders: {
                    waitingPath: beRoutes.controllers.Assets.versioned("vnd/fineUploader/placeholders/waiting-generic.png").url,
                    notAvailablePath: beRoutes.controllers.Assets.versioned("vnd/fineUploader/placeholders/not_available-generic.png").url
                }
            },
            element: document.getElementById(options.elementId),
            request: {
                endpoint:beRoutes.controllers.FilesCtrl.apiAddFile(options.subjectId).url
            },
            callbacks: {
                onComplete: options.onComplete
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