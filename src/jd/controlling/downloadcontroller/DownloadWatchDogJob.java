package jd.controlling.downloadcontroller;


public interface DownloadWatchDogJob {

    /**
     * returns true if the DownloadWatchDogJob should be executed again
     * 
     * @param downloadWatchDogLoopLinks
     * @return
     */
    public void execute(DownloadSession currentSession);
}
