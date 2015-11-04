var extracting = false;
/* 
================ Check if there are running extraction processes===================== 
This Script es executed in an interval of 1 second (1000 ms) and will check 
the current active state. If the state changes to false, it will play a wav file 
===================================================================================== 
*/


//use an API Call to get all Downloadlinks in your list
var links = callAPI("downloadsV2", "queryLinks", {
    "name": true
});
//loop through all links
for (var i = 0; i < links.length; i++) {
    //convert API Link to a Link Object
    var link = getDownloadLinkByUUID(links[i].uuid);
    //check if the link is currently in extraction state
    var isExtracting = link.getExtractionStatus() == "RUNNING";

    if (isExtracting) {
        //if it is, set variable and break the loop
        extracting = true;
        break;
    }
}

// We are in active state, if the Download Controller is not idle and there is no extraction running
var active = !isDownloadControllerIdle() || extracting;
//check the active value we had in the last interval
var oldActive = getProperty("active", false) == true;
if (active != oldActive) {
    //active state changed
    //set new active state
    setProperty("active", active, false);
    if (!active) {
        //we have been active in the last interval and are not active any more
        playWavAudio(JD_HOME + "/themes/standard/org/jdownloader/sounds/captcha.wav");
    }
}