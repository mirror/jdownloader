package org.jdownloader.jdserv.stats;

import org.appwork.remotecall.RemoteCallInterface;

public interface StatisticsInterface extends RemoteCallInterface {
    /**
     * Will be called when JD has been started
     * 
     * @param id
     * @param startTime
     */
    void onStartup(String id, long startTime);

    //
    /**
     * Will be called once the first time jd started
     * 
     * @param id
     * @param time
     */
    void onFreshInstall(String id, long time);

}
