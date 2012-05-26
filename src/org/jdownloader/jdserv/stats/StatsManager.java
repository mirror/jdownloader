package org.jdownloader.jdserv.stats;

import java.util.Calendar;

import jd.utils.JDUtilities;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.update.JDUpdater;

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
    private long                startTime;
    private boolean             enabled = true;

    /**
     * Create a new instance of StatsManager. This is a singleton class. Access
     * the only existing instance by using {@link #getInstance()}.
     */
    private StatsManager() {
        remote = new LoggerRemoteClient(org.jdownloader.jdserv.JD_SERV_CONSTANTS.CLIENT).create(StatisticsInterface.class);
        config = JsonConfig.create(StatsManagerConfig.class);
        id = config.getAnonymID();
        boolean fresh = id != null;

        queue = new Queue("StatsManager Queue") {
        };

        logStart();
        if (fresh) {
            logFreshInstall();
        }
        startTime = System.currentTimeMillis();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                logExit();
            }
        });

    }

    protected void logExit() {
        if (!isEnabled()) return;
        queue.addWait(new AsynchLogger() {
            private long time;
            {
                time = System.currentTimeMillis();
            }

            @Override
            public void doRemoteCall() {
                remote.onExit(id, time, time - startTime);
            }

        });

    }

    private void logFreshInstall() {
        if (!isEnabled()) return;
        queue.add(new AsynchLogger() {
            private long time;
            {
                time = System.currentTimeMillis();
            }

            @Override
            public void doRemoteCall() {
                if (!isEnabled()) return;
                remote.onFreshInstall(id, time);
            }

        });
    }

    public void logAction(String key) {
        if (!isEnabled()) return;
        queue.add(new AsynchLogger() {
            private long time;
            {
                time = System.currentTimeMillis();
            }

            @Override
            public void doRemoteCall() {
                if (!isEnabled()) return;
                remote.onFreshInstall(id, time);
            }

        });

    }

    private void logStart() {
        if (!isEnabled()) return;
        queue.add(new AsynchLogger() {
            private long time;
            {
                time = System.currentTimeMillis();
            }

            @Override
            public void doRemoteCall() {
                if (!isEnabled()) return;
                String nID = remote.onStartup(id, time, Calendar.getInstance().getTimeZone().toString(), CrossSystem.getOSString(), Application.getJavaVersion(), Application.isJared(StatsManager.class), JDUpdater.getInstance().getBranch().getName(), JDUtilities.getRevisionNumber());

                if (id == null) {
                    id = nID;
                    config.setAnonymID(id);

                }
                if (id == null) setEnabled(false);

            }

        });
    }

    /**
     * this setter does not set the config flag. Can be used to disable the
     * logger for THIS session.
     * 
     * @param b
     */
    protected void setEnabled(boolean b) {
        enabled = b;
    }

    public boolean isEnabled() {
        return enabled && config.isEnabled();
    }

}
