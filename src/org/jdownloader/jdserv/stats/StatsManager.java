package org.jdownloader.jdserv.stats;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.queue.Queue;

public class StatsManager {
    private static final StatsManager INSTANCE = new StatsManager();

    /**
     * get the only existing instance of StatsManager. This is a singleton
     * 
     * @return
     */
    public static StatsManager I() {
        return StatsManager.INSTANCE;
    }

    private StatisticsInterface remote;
    private StatsManagerConfig  config;
    private Queue               queue;
    private String              id;

    /**
     * Create a new instance of StatsManager. This is a singleton class. Access
     * the only existing instance by using {@link #getInstance()}.
     */
    private StatsManager() {
        remote = org.jdownloader.jdserv.JD_SERV_CONSTANTS.CLIENT.create(StatisticsInterface.class);
        config = JsonConfig.create(StatsManagerConfig.class);
        boolean fresh = config.isFreshInstall();
        config.setFreshInstall(false);
        id = config.getAnonymID();

        queue = new Queue("StatsManager Queue") {
        };

        logStart();
        if (fresh) {
            logFreshInstall();
        }

    }

    private void logFreshInstall() {
        queue.add(new AsynchLogger() {
            private long time;
            {
                time = System.currentTimeMillis();
            }

            @Override
            public void doRemoteCall() {
                remote.onFreshInstall(id, time);
            }

        });
    }

    private void logStart() {

        queue.add(new AsynchLogger() {
            private long time;
            {
                time = System.currentTimeMillis();
            }

            @Override
            public void doRemoteCall() {
                remote.onStartup(id, time);
            }

        });
    }

}
