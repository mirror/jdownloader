package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.net.throttledconnection.ThrottledConnection;
import org.appwork.utils.speedmeter.AverageSpeedMeter;
import org.appwork.utils.speedmeter.SpeedMeterInterface;
import org.jdownloader.logging.LogController;

public class DownloadSpeedManager {

    private static class ManagedThrottledConnectionHelper {

        protected final ManagedThrottledConnectionHandler manager;

        private ManagedThrottledConnectionHelper(ManagedThrottledConnectionHandler manager) {
            this.manager = manager;
        }

        protected final HashMap<ThrottledConnection, ThrottledConnectionHelp> connections   = new HashMap<ThrottledConnection, DownloadSpeedManager.ThrottledConnectionHelp>();
        protected final AtomicLong                                            lastTraffic   = new AtomicLong(0l);
        protected final AtomicLong                                            lastTimeStamp = new AtomicLong(0l);
    }

    private static class ThrottledConnectionHelp {
        protected final ThrottledConnection connection;

        private ThrottledConnectionHelp(ThrottledConnection connection) {
            this.connection = connection;
        }

        // protected long lastLimit = 0;
        protected int              currentLimit  = 0;
        // protected long lastSpeed = 0;
        protected final AtomicLong lastTraffic   = new AtomicLong(0l);
        protected final AtomicLong lastTimeStamp = new AtomicLong(0l);
    }

    private final CopyOnWriteArrayList<ManagedThrottledConnectionHandler> connectionHandlers = new CopyOnWriteArrayList<ManagedThrottledConnectionHandler>();
    private final CopyOnWriteArrayList<ManagedThrottledConnectionHandler> removedHandlers    = new CopyOnWriteArrayList<ManagedThrottledConnectionHandler>();

    private NullsafeAtomicReference<Thread>                               watchDogThread     = new NullsafeAtomicReference<Thread>(null);
    protected final int                                                   updateSpeed        = 2000;
    /**
     * download speed in bytes
     */
    protected final AtomicInteger                                         bandwidth          = new AtomicInteger(0);
    protected final SpeedMeterInterface                                   speedMeter         = new AverageSpeedMeter(10);
    /**
     * bytes downloaded
     */
    protected final AtomicLong                                            traffic            = new AtomicLong(0l);
    protected final AtomicInteger                                         connections        = new AtomicInteger(0);

    protected final AtomicInteger                                         limit              = new AtomicInteger(0);

    public void addConnectionHandler(ManagedThrottledConnectionHandler handler) {
        if (connectionHandlers.addIfAbsent(handler)) {
            handler.setManagedBy(this);
            /*
             * we set very low limit here because we want the real speed to get assigned on next speed-assign-loop
             */
            startWatchDog();
        }
    }

    public void removeConnectionHandler(ManagedThrottledConnectionHandler handler) {
        if (connectionHandlers.remove(handler)) {
            handler.setManagedBy(null);
            removedHandlers.add(handler);
        }
    }

    private void startWatchDog() {
        synchronized (watchDogThread) {
            Thread thread = watchDogThread.get();
            if (thread != null && thread.isAlive()) return;
            /* we have to start a new watchDog */
            thread = new Thread() {
                @Override
                public void run() {
                    try {
                        this.setName("DownloadSpeedManager");
                        /* reset SpeedMeter */
                        speedMeter.resetSpeedMeter();
                        final HashMap<ManagedThrottledConnectionHandler, ManagedThrottledConnectionHelper> speedAssignHelpMap = new HashMap<ManagedThrottledConnectionHandler, ManagedThrottledConnectionHelper>();
                        java.util.List<ManagedThrottledConnectionHandler> removedHandlers = new ArrayList<ManagedThrottledConnectionHandler>();
                        java.util.List<ThrottledConnectionHelp> currentHelpers = new ArrayList<ThrottledConnectionHelp>();

                        while (true) {
                            synchronized (watchDogThread) {
                                if (connectionHandlers.size() == 0 && DownloadSpeedManager.this.removedHandlers.size() == 0) {
                                    if (watchDogThread.compareAndSet(Thread.currentThread(), null)) {
                                        bandwidth.set(0);
                                        connections.set(0);
                                        speedMeter.resetSpeedMeter();
                                    }
                                    break;
                                }
                            }
                            final long sleepTime = Math.max(1000, updateSpeed);
                            try {
                                Thread.sleep(sleepTime);
                            } catch (final InterruptedException e) {
                                LogController.GL.log(e);
                            }

                            int newBandwidth = 0;
                            final long lastRoundTraffic = traffic.get();
                            currentHelpers.clear();
                            removedHandlers.clear();
                            /* fetch live stats from all connections */
                            for (final ManagedThrottledConnectionHandler manager : DownloadSpeedManager.this.removedHandlers) {
                                removedHandlers.add(manager);
                                ManagedThrottledConnectionHelper managerHelper = speedAssignHelpMap.remove(manager);
                                long trafficDifference = 0;
                                long bandWidth = 0;
                                if (managerHelper != null) {
                                    long latestTimeStamp = managerHelper.lastTimeStamp.getAndSet(System.currentTimeMillis());
                                    long latestTraffic = managerHelper.lastTraffic.getAndSet(manager.getTraffic());
                                    trafficDifference = Math.max(0, managerHelper.lastTraffic.get() - latestTraffic);
                                    long timeDifference = Math.max(0, managerHelper.lastTimeStamp.get() - latestTimeStamp);
                                    if (timeDifference > 0) bandWidth = (int) (trafficDifference * 1000 / timeDifference);
                                } else {
                                    trafficDifference = manager.getTraffic();
                                    bandWidth = trafficDifference * 1000 / sleepTime;
                                }
                                newBandwidth += bandWidth;
                                traffic.addAndGet(trafficDifference);
                            }
                            DownloadSpeedManager.this.removedHandlers.removeAll(removedHandlers);

                            for (final ManagedThrottledConnectionHandler manager : connectionHandlers) {
                                /* manager handling */
                                ManagedThrottledConnectionHelper managerHelper = speedAssignHelpMap.get(manager);
                                if (managerHelper == null) {
                                    managerHelper = new ManagedThrottledConnectionHelper(manager);
                                    speedAssignHelpMap.put(manager, managerHelper);
                                    managerHelper.lastTimeStamp.set(System.currentTimeMillis());
                                    managerHelper.lastTraffic.set(manager.getTraffic());
                                } else {
                                    long latestTimeStamp = managerHelper.lastTimeStamp.getAndSet(System.currentTimeMillis());
                                    long latestTraffic = managerHelper.lastTraffic.getAndSet(manager.getTraffic());
                                    long trafficDifference = Math.max(0, managerHelper.lastTraffic.get() - latestTraffic);
                                    long timeDifference = Math.max(0, managerHelper.lastTimeStamp.get() - latestTimeStamp);
                                    if (timeDifference > 0) newBandwidth += (int) (trafficDifference * 1000 / timeDifference);
                                    traffic.addAndGet(trafficDifference);
                                }
                                /* connection handling */
                                for (ThrottledConnection connection : manager.getConnections()) {
                                    ThrottledConnectionHelp connectionHelper = managerHelper.connections.get(connection);
                                    if (connectionHelper == null) {
                                        connectionHelper = new ThrottledConnectionHelp(connection);
                                        connectionHelper.lastTimeStamp.set(System.currentTimeMillis());
                                        connectionHelper.lastTraffic.set(connection.transfered());
                                    } else {
                                        connectionHelper.lastTimeStamp.getAndSet(System.currentTimeMillis());
                                        connectionHelper.lastTraffic.getAndSet(connection.transfered());
                                        // long trafficDifference = Math.max(0, connectionHelper.lastTraffic.get() - latestTraffic);
                                        // long timeDifference = Math.max(0, connectionHelper.lastTimeStamp.get() - latestTimeStamp);
                                        // connectionHelper.lastSpeed = (int) (trafficDifference * 1000 / timeDifference);
                                    }
                                    currentHelpers.add(connectionHelper);
                                }
                            }
                            bandwidth.set(newBandwidth);
                            speedMeter.putSpeedMeter(Math.max(0, traffic.get() - lastRoundTraffic), sleepTime);
                            connections.set(currentHelpers.size());
                            int left = currentHelpers.size();
                            final int currentLimit = limit.get();
                            int limitLeft = currentLimit;
                            // int step = 1024 * 50;
                            /* recalculate new speed limits for each connection */
                            for (final ThrottledConnectionHelp helper : currentHelpers) {
                                /* calculate new speed limit for this connection */
                                int newLimit = limitLeft / left;
                                // int lastLimit = helper.currentLimit;
                                if (currentLimit > 0) {
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
                                // helper.lastLimit = lastLimit;
                                /*
                                 * this speed limit can be assigned to rest of connections
                                 */
                                left--;
                                // System.out.println(helper.currentLimit);
                                limitLeft -= helper.currentLimit;
                            }
                            /* assign new speed limits to each connection */
                            for (final ThrottledConnectionHelp helper : currentHelpers) {
                                helper.connection.setLimit(helper.currentLimit);
                            }
                        }
                    } catch (Throwable e) {
                        LogController.GL.log(e);
                    }
                }
            };
            watchDogThread.set(thread);
            thread.start();
        }

    }

    public void setLimit(final int newLimit) {
        limit.set(Math.max(0, newLimit));
    }

    public int getLimit() {
        return limit.get();
    }

    /**
     * @return the current download speed in bytes
     */
    public int getSpeed() {
        return bandwidth.get();
    }

    public SpeedMeterInterface getSpeedMeter() {
        return this.speedMeter;
    }

    /**
     * @return how many bytes have been transfered
     */
    public long getTraffic() {
        return traffic.get();
    }

    public int connections() {
        return connections.get();
    }
}
