/*jshint esversion: 6 */
var twitterMode=false;
var $characterCounter;
var counterSpan;
var curMessageKey;
var messages = {};
var $contentPane;
let messageChanged = false;

const setMessageChanged = function(){messageChanged=true;};

function messagesPageSetup(){
    $characterCounter = $("#characterCounter");
    counterSpan = $characterCounter.find("span")[0];
    $contentPane = $("#content");

    $contentPane.on("keypress",setMessageChanged);
    messageSelectionChanged();
    loadMessages();
    $("#messageTable td").on( "click", function(){
        if ( this.id.trim().length > 0 ) {
            const comps = this.id.trim().split("_");
            const key = {
                gender: comps[0],
                platform: comps[1],
                position: comps[2]
            };
            document.getElementById("platform"+key.platform).checked = true;
            document.getElementById(key.gender).checked = true;
            document.getElementById(key.position).checked = true;
            messageSelectionChanged();
        }
    });
}


function forAllKeys( f ) {
    ["male", "female"].forEach( function(g){
        ["Email", "Twitter", "WhatsApp"].forEach( function(platform){
            positions.forEach(function(pos){
                f({
                    gender:g,
                    platform:platform,
                    position:pos
                });
            });
        });
    });
}

function updateMessageTable() {
    forAllKeys(function(key){
        const hasData = (load(key).trim().length > 0);
        const $cell = $("#"+key.gender+"_"+key.platform+"_"+key.position);
        if ( hasData ) {
            $cell.removeClass("noMessage");
            $cell.addClass("hasMessage");
            if ( (key.platform === "Twitter") && (load(key).trim().length > 255) ) {
                $cell.addClass("hasWarning");
            } else {
                $cell.removeClass("hasWarning");
            }
        } else {
            $cell.removeClass("hasMessage");
            $cell.addClass("noMessage");
        }
    });
}

function makeMessage( messageKey, content ) {
    return {
        platform:messageKey.platform,
        gender:messageKey.gender,
        position:messageKey.position,
        camId:campaignId,
        text:content
    };
}

function save( messageKey, content ) {
    messages[keyToString(messageKey)] =  makeMessage(messageKey, content);
    updateMessageTable();
}

function load( messageKey ) {
    const message = messages[keyToString(messageKey)];
    return message ? message.text : "";
}

let lastClass="bg-success";
function updateCharacterCount() {
    const charLen = $contentPane.val().length;
    const left = 280-charLen;
    counterSpan.textContent=String(left);
    let curClass = "bg-success";
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
//
function keyToString( aKey ) {
    return aKey.platform + "/" + aKey.gender + "/" + aKey.position;
}

function getMessageKey() {
 const retVal = {};
 const checkedIds = $( "input:checked" ).map(function() {
            return this.id;
        }).get();

 const platform = checkedIds.filter(e=>e.indexOf("platform")===0)[0];

 retVal.platform = platform.replace("platform","");
 retVal.gender = (checkedIds.indexOf("male")>-1) ? "male" : "female";
 for ( var itm in checkedIds ) {
     if ( checkedIds[itm]!==retVal.gender && checkedIds[itm]!==retVal.media ) {
         retVal.position = checkedIds[itm];
     }
 }

 return retVal;
}

function saveMessages() {
    const msgDiv = Informationals.showBackgroundProcess(polyglot.t("update.messages"));

    const curContent = $contentPane.val();
    if ( curContent.trim().length > 0 ) {
        const msgKey = getMessageKey();
        save( msgKey, curContent );
    }

    const arr = [];
    for ( const k in messages ) {
        arr.push( messages[k] );
    }
    new Playjax(beRoutes)
        .using(function (c) {
            return c.CampaignMgrCtrl.updateMessages(campaignId);
        }).fetch(arr)
        .then( function (res) {
            if (res.ok) {
                msgDiv.success();
                messageChanged = false;
                return true;
            } else {
                msgDiv.dismiss();
                Informationals.makeDanger(polyglot.t("update.failed"), "", 2500).show();
                res.json().then(function(body){
                    console.log(body);
                });
                return false;
            }
        });
}

function parseLoadedData( json ){
    json.forEach( function(msg){
        save( msg, msg.text );
    });
    $contentPane.val( load(getMessageKey()) );
    updateCharacterCount();
}

function loadMessages(){
    Informationals.loader(polyglot.t("loading.messages"));
    new Playjax(beRoutes).using( function(c){
        return c.CampaignMgrCtrl.getMessages(campaignId);

    }).fetch().then( function( res){
        if (res.ok) {
            return res.json();
        } else {
            console.log( res );
            throw new Error(polyglot.t("loading.failed") + ":" + res.status);
        }

    }).then( function(json){
        parseLoadedData(json);

    }).catch( function(err){
        console.log( err );
        Informationals.makeDanger(polyglot.t("loading.failed") + ":" + err, "", 2000).show();

    }).finally( function(){
        Informationals.loader.dismiss();
    });
}

function messageSelectionChanged() {
    const key = getMessageKey();
    twitterMode = (key.platform==="Twitter");
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
    const $msgTbl = $("#messageTable");
    $msgTbl.find("td").removeClass("current");
    $msgTbl.find("td#" + key.gender + "_" + key.platform + "_" + key.position).addClass("current");

}


function copyToEmpty() {
    const key = getMessageKey();
    const content = $contentPane.val();
    save( key, content );
    forAllKeys( function(key){
        const keyAsString = keyToString(key);
        const msg = messages[keyAsString];
        if ( !msg || msg.text.trim().length===0 ) {
            messages[keyAsString] = makeMessage(key, content);
        }
    });

    updateMessageTable();
    setMessageChanged();
    Informationals.makeSuccess(polyglot.t("campaignMgr.content.success"),"",1000).show();
}

function copyToSamePos() {
    const curKey = getMessageKey();
    const content = $contentPane.val();
    save( curKey, content );
    forAllKeys( function(key){
        if ( curKey.position === key.position ) {
            const keyAsString = keyToString(key);
            const msg = messages[keyAsString];
            messages[keyAsString] = makeMessage(key, content);
        }
    });

    updateMessageTable();
    setMessageChanged();
    Informationals.makeSuccess(polyglot.t("campaignMgr.content.success"),"",1000).show();
}

function copyToAll() {
    swal({
        title: polyglot.t("campaignMgr.content.copyToAll.title"),
        text: polyglot.t("campaignMgr.content.copyToAll.text"),
        icon: "warning",
        buttons: {
            cancel:polyglot.t("cancel"),
            confirm:polyglot.t("confirm")
        },
        dangerMode: true,
    }).then((copyToAll) => {
        if (copyToAll) {
            const key = getMessageKey();
            const content = $contentPane.val();
            save( key, content );
            forAllKeys( function(key){
                const keyAsString = keyToString(key);
                const msg = messages[keyAsString];
                messages[keyAsString] = makeMessage(key, content);
            });
            updateMessageTable();
            setMessageChanged();
            Informationals.makeSuccess(polyglot.t("campaignMgr.content.success"),"",1000).show();
        }
    });
}

window.addEventListener('beforeunload', function (e) {
    if (messageChanged) {
        e.preventDefault(); // If you prevent default behavior in Mozilla Firefox prompt will always be shown
        e.returnValue = '';
    }
});

window.onload = function() {
    Promise.prototype.finally = Promise.prototype.finally || {
        finally (fn) {
            const onFinally = cb => Promise.resolve(fn()).then(cb);
            return this.then(
                result => onFinally(() => result),
                reason => onFinally(() => Promise.reject(reason))
            );
        }
    }.finally;
};

const tour = {
    id:"tour",
    steps:[
        {
            "title":polyglot.t("tour.messagesPage.section.title"),
            "content":polyglot.t("tour.messagesPage.section.content"),
            "target":"pageSection_Messages",
            placement:"bottom"
        },
        {
            "title":polyglot.t("tour.messagesPage.messageDestinationSelectors.title"),
            "content":polyglot.t("tour.messagesPage.messageDestinationSelectors.content"),
            "target":"messageDestinationSelectors",
            placement:"left"
        },
        {
            "title":polyglot.t("tour.messagesPage.messageTable.title"),
            "content":polyglot.t("tour.messagesPage.messageTable.content"),
            "target":"messageTable",
            placement:"right"
        },
        {
            "title":polyglot.t("tour.messagesPage.content.title"),
            "content":polyglot.t("tour.messagesPage.content.content"),
            "target":"content",
            placement:"top"
        }
    ],
    showPrevButton: true,
    i18n:{
        nextBtn: polyglot.t("next"),
        prevBtn: polyglot.t("prev"),
        doneBtn: polyglot.t("done")
    }
};