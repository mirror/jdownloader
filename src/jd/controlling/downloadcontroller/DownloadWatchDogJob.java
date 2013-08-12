package jd.controlling.downloadcontroller;

import java.util.List;

import jd.plugins.DownloadLink;

public interface DownloadWatchDogJob {

    /**
     * returns true if the DownloadWatchDogJob should be executed again
     * 
     * @param downloadWatchDogLoopLinks
     * @return
     */
    public void execute(List<DownloadLink> downloadWatchDogLoopLinks);
}
