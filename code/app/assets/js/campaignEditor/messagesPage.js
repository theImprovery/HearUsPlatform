var twitterMode=false;
var $characterCounter;
var counterSpan;
var curMessageKey;
var messages = {};
var $contentPane;

function setup(){
    $characterCounter = $("#characterCounter");
    counterSpan = $characterCounter.find("span")[0];
    $contentPane = $("#content");
    messageSelectionChanged();
}

function save( messageKey, content ) {
    messages[keyToString(messageKey)] =  {
        platform:messageKey.media,
        gender:messageKey.gender,
        position:messageKey.position,
        camId:campaignId,
        text:content
    };
}

function load( messageKey ) {
    var message = messages[keyToString(messageKey)];
    return message ? message.text : "";
}

var lastClass="bg-success";
function updateCharacterCount() {
    var charLen = $contentPane.val().length;
    var left = 280-charLen;
    counterSpan.textContent=String(left);
    var curClass = "bg-success";
    if ( left < 100 ) {
        curClass = "bg-warning";
    } else if ( left < 0 ) {
        curClass = "bg-danger";
    }

    if ( lastClass !== curClass ) {
        $characterCounter.removeClass(lastClass);
        $characterCounter.addClass(curClass);
        lastClass = curClass;
    }
}

function keyToString( aKey ) {
    return aKey.media + "/" + aKey.gender + "/" + aKey.position;
}

function getMessageKey() {
 var retVal = {};
 var status = $( "input:checked" ).map(function() {
            return this.id;
        }).get();

 retVal.media = (status.indexOf("mediaTwitter")>-1) ? "Twitter" : "Email";
 retVal.gender = (status.indexOf("male")>-1) ? "male" : "female";
 for ( var itm in status ) {
     if ( status[itm]!==retVal.gender && status[itm]!==retVal.media ) {
         retVal.position = status[itm];
     }
 }

 return retVal;
}

function saveMessages() {
    Informationals.loader("Saving");
    messageSelectionChanged(); // save current message;
    
    var arr = [];
    for ( var k in messages ) {
        arr.push( messages[k] );
    }
    new Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignMgrCtrl.updateMessages(campaignId);
        })
        .fetch(arr)
        .then( function (res) {
            Informationals.loader.dismiss();
            if (res.ok) {
                Informationals.makeSuccess("Messages updated " + data.name, "OK", 1500).show();
            } else {
                Informationals.makeDanger("Update Campaign " + data.name, "Failed", 2500).show();
                res.result().then(function(body){
                    console.log(body);
                });
            }
        });
}

function messageSelectionChanged() {
    var key = getMessageKey();
    twitterMode = (key.media==="twitter");
    if ( twitterMode ) {
        $characterCounter.slideDown();
    } else {
        $characterCounter.slideUp();
    }

    if ( curMessageKey ) {
        save( curMessageKey, $contentPane.val() );
    }
    curMessageKey = key;
    $contentPane.val( load(curMessageKey) );
    updateCharacterCount();
}



