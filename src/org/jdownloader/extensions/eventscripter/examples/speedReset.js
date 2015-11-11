//check if downloads are running at all
if (isDownloadControllerRunning() && !isDownloadControllerStopping()) {
    var running = getRunningDownloadLinks();
    //loop through all running Downloads
    for (var i = 0; i < running.length; i++) {
        //check if the download has been running at least 30 seconds
        if (running[i].getDownloadDuration() > 30000) {
            //check if the current speed is below 50kb/s
            if (running[i].getSpeed() < 50* 1024) {
                //reset the download
                //running[i].reset();
                //stop the download and restart it
                running[i].abort();
            }

        }
    }
}
