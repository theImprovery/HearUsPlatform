/*jshint esversion: 6 */
const cfpCtrl = (function () {

    const statuses = ["all","for","against","undecided","neutral"];
    const kmData = [];
    const counters = {};
    const counts = {};
    const filterButtons = [];
    const pass = function(){ return true; };
    let d3Selection;
    let blockKmUpdates = false;
    let groupFilter = pass;
    let statusFilter = pass;


    function setup() {
        blockKmUpdates = true;
        // build KM data
        let kmLis = document.getElementById("kms").getElementsByTagName("li");
        for (let i = 0; i < kmLis.length; i++) {
            let li = kmLis.item(i);
            kmData.push({
                id: Number(li.dataset.kmid),
                party: Number(li.dataset.kmparty),
                status: li.classList[0].toLowerCase()
            });
        }

        // D3js selection
        d3Selection = d3.select("#kms").selectAll("li").data(kmData);

        // counters
        statuses.forEach( e => {
           counters[e] = document.getElementById( e + "Count");
           counts[e] = 0;
        });

        updateCounts();

        // collect controls
        const buttons = document.getElementById("filterButtons").getElementsByTagName("button");
        for ( let idx=0; idx<buttons.length; idx++ ) {
            filterButtons.push( buttons.item(idx));
        }

        // OK to start interaction
        blockKmUpdates = false;

    }

    function updateKmDisplay() {
        if (blockKmUpdates) return;
        // KM card animation
        d3Selection.transition().duration(1000).style("width", function (mk) {
            return (groupFilter(mk) && statusFilter(mk)) ? "110px" : "0px";
        }).style("margin-left", function (mk) {
            return (groupFilter(mk) && statusFilter(mk)) ? "14px" : "0px";
        }).style("margin-right", function (mk) {
            return (groupFilter(mk) && statusFilter(mk)) ? "14px" : "0px";
        });

        updateCounts();

        const displayedCount = kmData.filter( km=>groupFilter(km)&&statusFilter(km) ).length;
        if (displayedCount === 0) {
            $("#noMksFound").slideDown();
        } else {
            $("#noMksFound").slideUp();
        }
    }

    function updateCounts() {
        statuses.forEach(e => {
            counts[e] = 0;
        });
        kmData.filter(km => groupFilter(km))
            .forEach(km => {
                counts[km.status] = counts[km.status] + 1;
                counts.all = counts.all + 1;
            });

        statuses.forEach(key => counters[key].innerHTML = counts[key]);
    }

    function filterByStatus(status) {
        if (status === null) {
            statusFilter = pass;
        } else {
            statusFilter = function(mk) {
                return mk.status === status;
            };
        }
        updateKmDisplay();
    }

    function filterByGroup(which) {
        if ( which === "all" ) {
            groupFilter = pass;

        } else {
            let comps = which.split("_");
            if ( comps[0] === "c" ) {
                filterByCommittee( comps[1] );
            } else {
                filterByParty( comps[1] );
            }
        }

        updateKmDisplay();
    }

    function filterByCommittee( committeeId ) {
        committeeId = Number(committeeId);
        groupFilter = function(mk) {
            return mk2cmt[mk.id] && (mk2cmt[mk.id].indexOf(committeeId) >= 0);
        };
    }

    function filterByParty( partyId ) {
        partyId = Number(partyId);
        groupFilter = function(mk) {
            return mk.party === partyId;
        };
    }

    function selectFilterButton(btn) {
        filterButtons.forEach( fBtn => fBtn.classList.remove("selected") );
        btn.classList.add("selected");
    }

    return {
        setup: setup,
        filterByStatus: filterByStatus,
        filterByGroup: filterByGroup,
        selectFilterButton:selectFilterButton,
        __kmData: kmData,
        __d3Selection: function(){ return d3Selection;}
    };
})();
