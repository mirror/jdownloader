package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appwork.utils.logging.Log;
import org.appwork.utils.net.throttledconnection.ThrottledConnection;
import org.appwork.utils.speedmeter.AverageSpeedMeter;
import org.appwork.utils.speedmeter.SpeedMeterInterface;

public class DownloadSpeedManager {

    private static class SpeedAssignHelp {
        protected long lastLimit     = 0;
        protected int  currentLimit  = 0;
        protected long lastSpeed     = 0;
        protected long lastTraffic   = 0;
        protected long lastTimeStamp = 0;
    }

    java.util.List<ManagedThrottledConnectionHandler> connectionHandlers = new ArrayList<ManagedThrottledConnectionHandler>();
    private final Object                         watchDogLOCK       = new Object();
    private Thread                               watchDog           = null;
    protected volatile int                       limit              = 0;
    protected int                                updateSpeed        = 2000;
    /**
     * download speed in bytes
     */
    protected volatile int                       bandwidth          = 0;
    protected SpeedMeterInterface                speedMeter         = new AverageSpeedMeter(10);
    /**
     * bytes downloaded
     */
    protected volatile long                      traffic            = 0;
    protected int                                connections        = 0;

    public void addConnectionHandler(ManagedThrottledConnectionHandler handler) {
        if (this.connectionHandlers.contains(handler)) { return; }
        synchronized (this) {
            final java.util.List<ManagedThrottledConnectionHandler> newConnectionHandlers = new ArrayList<ManagedThrottledConnectionHandler>(connectionHandlers);
            newConnectionHandlers.add(handler);
            handler.setManagedBy(this);
            this.connectionHandlers = newConnectionHandlers;
        }
        /*
         * we set very low limit here because we want the real speed to get assigned on next speed-assign-loop
         */
        startWatchDog();
    }

    public void removeConnectionHandler(ManagedThrottledConnectionHandler handler) {
        if (!this.connectionHandlers.contains(handler)) { return; }
        synchronized (this) {
            final java.util.List<ManagedThrottledConnectionHandler> newConnectionHandlers = new ArrayList<ManagedThrottledConnectionHandler>(connectionHandlers);
            newConnectionHandlers.remove(handler);
            handler.setManagedBy(null);
            this.connectionHandlers = newConnectionHandlers;
        }
    }

    private void startWatchDog() {
        synchronized (this.watchDogLOCK) {
            if (this.watchDog != null && this.watchDog.isAlive()) { return; }
            /* we have to start a new watchDog */
            this.watchDog = new Thread() {
                @Override
                public void run() {
                    this.setName("DownloadSpeedManager");
                    /* reset SpeedMeter */
                    speedMeter.resetSpeedMeter();
                    final HashMap<ThrottledConnection, SpeedAssignHelp> speedAssignHelpMap = new HashMap<ThrottledConnection, SpeedAssignHelp>();
                    while (true) {
                        final java.util.List<ManagedThrottledConnectionHandler> lconnectionHandlers = connectionHandlers;
                        if (lconnectionHandlers.size() == 0) {
                            break;
                        }
                        final long sleepTime = Math.max(1000, updateSpeed);
                        try {
                            Thread.sleep(sleepTime);
                        } catch (final InterruptedException e) {
                            Log.exception(e);
                        }
                        long lastTraffic = 0;
                        int newBandwidth = 0;
                        long lastRound = 0;
                        long lastRoundTraffic = 0;
                        java.util.List<ThrottledConnection> allCons = new ArrayList<ThrottledConnection>();
                        /* fetch live stats from all connections */
                        for (final ManagedThrottledConnectionHandler conH : lconnectionHandlers) {
                            List<ThrottledConnection> lconnections = conH.getConnections();
                            allCons.addAll(lconnections);
                            for (ThrottledConnection con : lconnections) {
                                SpeedAssignHelp helper = speedAssignHelpMap.get(con);
                                if (helper == null) {
                                    helper = new SpeedAssignHelp();
                                    speedAssignHelpMap.put(con, helper);
                                    helper.lastTraffic = con.transfered();
                                    helper.lastTimeStamp = System.currentTimeMillis();
                                } else {
                                    final long sleepTimeCon = System.currentTimeMillis() - helper.lastTimeStamp;
                                    lastTraffic = con.transfered();
                                    helper.lastTimeStamp = System.currentTimeMillis();
                                    /* update new traffic stats */
                                    traffic += lastRound = lastTraffic - helper.lastTraffic;
                                    helper.lastTraffic = lastTraffic;
                                    lastRoundTraffic += lastRound;
                                    /* update new bandwidth stats */
                                    newBandwidth += helper.lastSpeed = (int) (lastRound * 1000 / sleepTimeCon);
                                }
                            }
                        }
                        bandwidth = newBandwidth;
                        speedMeter.putSpeedMeter(lastRoundTraffic, sleepTime);
                        connections = allCons.size();
                        int left = allCons.size();
                        int limitLeft = limit;
                        int step = 1024 * 50;
                        /* recalculate new speed limits for each connection */
                        for (final ThrottledConnection con : allCons) {
                            final SpeedAssignHelp helper = speedAssignHelpMap.get(con);
                            /* calculate new speed limit for this connection */
                            int newLimit = limitLeft / left;
                            int lastLimit = helper.currentLimit;
                            if (limit > 0) {
                                // /*
                                // * only calculate this stuff when we have an
                                // * active speed limit
                                // */
                                // if (helper.lastSpeed <= newLimit) {
                                // /* last round was slower than minimum */
                                // System.out.println("last round was slower");
                                // final int limit2 = Math.min((int)
                                // helper.lastLimit, newLimit);
                                // if (helper.lastSpeed + step <= limit2) {
                                // System.out.println("last round was slower: we can decrease minimum");
                                // newLimit = limit2 - step;
                                // } else {
                                // if (helper.lastSpeed > newLimit) {
                                // System.out.println("speed is fine, no need to increase");
                                // } else {
                                // System.out.println("last round was slower: increase maybe it can be faster");
                                // newLimit = limit2 + step;
                                // }
                                // }
                                // } else {
                                // newLimit = Math.max((int) helper.lastLimit,
                                // newLimit);
                                // }
                                helper.currentLimit = Math.max(10, newLimit);
                            } else {
                                helper.currentLimit = 0;
                            }
                            helper.lastLimit = lastLimit;
                            /*
                             * this speed limit can be assigned to rest of connections
                             */
                            left--;
                            // System.out.println(helper.currentLimit);
                            limitLeft -= helper.currentLimit;
                        }
                        /* assign new speed limits to each connection */
                        for (final ThrottledConnection con : allCons) {
                            final SpeedAssignHelp helper = speedAssignHelpMap.get(con);
                            con.setLimit(helper.currentLimit);
                        }
                    }
                    /* this watchDog is finished, do some cleanup stuff */
                    synchronized (watchDogLOCK) {
                        watchDog = null;
                        bandwidth = 0;
                        connections = 0;
                        speedMeter.resetSpeedMeter();
                    }
                }
            };
            this.watchDog.setDaemon(true);
            this.watchDog.start();
        }
    }

    public void setLimit(final int limit) {
        this.limit = Math.max(0, limit);
    }

    public int getLimit() {
        return this.limit;
    }

    /**
     * @return the current download speed in bytes
     */
    public int getSpeed() {
        return this.bandwidth;
    }

    public SpeedMeterInterface getSpeedMeter() {
        return this.speedMeter;
    }

    /**
     * @return how many bytes have been transfered
     */
    public long getTraffic() {
        return this.traffic;
    }

    public int connections() {
        return connections;
    }
}
