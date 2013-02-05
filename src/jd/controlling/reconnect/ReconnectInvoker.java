package jd.controlling.reconnect;

import java.util.ArrayList;

import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.controlling.reconnect.ipcheck.IPController;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.ProcessCallBackAdapter;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;

public abstract class ReconnectInvoker {
    private static final long OFFLINE_TIMEOUT = 30000;
    private RouterPlugin      routerPlugin;
    protected LogSource       logger          = LogController.TRASH;

    public LogSource getLogger() {
        return logger;
    }

    public void setLogger(LogSource logger) {
        if (logger == null) logger = LogController.TRASH;
        this.logger = logger;
    }

    public ReconnectInvoker(RouterPlugin routerPlugin) {
        this.routerPlugin = routerPlugin;
    }

    public abstract void run() throws ReconnectException, InterruptedException;

    public ReconnectResult validate() throws InterruptedException, ReconnectException {
        return validate(createReconnectResult());
    }

    protected ReconnectResult validate(ReconnectResult ret) throws InterruptedException, ReconnectException {
        ret.setInvoker(this);

        // Make sure that we are online

        if (IPController.getInstance().getIpState().isOffline()) {
            IPController.getInstance().invalidate();
            Thread.sleep(1000);
            IPController.getInstance().validate();
            if (IPController.getInstance().getIpState().isOffline()) { throw new ReconnectException(_GUI._.ReconnectInvoker_validate_offline_()); }

        }
        logger.info("IP BEFORE=" + IPController.getInstance().getIP());

        try {

            BalancedWebIPCheck.getInstance().setOnlyUseWorkingServices(true);
            IPController.getInstance().invalidate();
            ret.setStartTime(System.currentTimeMillis());
            testRun();
            Thread.sleep(1 * 1000);
            IPController ipc = IPController.getInstance();
            if (ipc.validate()) {
                // wow this hsa been fast
                logger.info("Successful: REconnect has been very fast!");
                ret.setSuccess(true);
                ret.setOfflineTime(System.currentTimeMillis());
                ret.setSuccessTime(System.currentTimeMillis());
                return ret;
            }
            logger.info("Script done. Wait for offline");
            do {
                // wait until we are offline
                Thread.sleep(1 * 1000);
                if (!ipc.validate() && !ipc.getIpState().isOffline() && (System.currentTimeMillis() - ret.getStartTime()) > OFFLINE_TIMEOUT) {
                    // we are not offline after 30 seconds
                    logger.info("Disconnect failed. Still online after " + OFFLINE_TIMEOUT + " ms");
                    return ret;
                }
            } while (!ipc.getIpState().isOffline() && ipc.isInvalidated());
            ret.setOfflineTime(System.currentTimeMillis());
            logger.info("Offline after " + ret.getOfflineDuration() + " ms");
            if (ipc.isInvalidated()) {
                logger.info("Wait for online status");
                // we have to wait LOOOONG here. reboot may take its time
                final long endTime = System.currentTimeMillis() + 450 * 1000;
                while (System.currentTimeMillis() < endTime) {
                    /* ip change detected then we can stop */
                    long s = System.currentTimeMillis();
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                    if (ipc.validate()) {
                        ret.setSuccessTime(System.currentTimeMillis());
                        ret.setSuccess(true);
                        logger.info("Successful: REconnect after " + ret.getSuccessDuration() + " ms");

                        return ret;
                        //
                    }
                    if (!ipc.getIpState().isOffline()) {
                        logger.info("Failed. returned from offline. But no new ip");
                        return ret;

                    }

                    Thread.sleep(Math.max(0, 1000 - (System.currentTimeMillis() - s)));
                }
                logger.info("Connect failed! Maybe router restart is required. This should NEVER happen!");
                return ret;
            } else {
                ret.setSuccessTime(System.currentTimeMillis());
                ret.setSuccess(true);
                logger.info("Successful: REconnect after " + ret.getSuccessDuration() + " ms");
                return ret;
            }
        } finally {
            logger.info("IP AFTER=" + IPController.getInstance().getIP());
            BalancedWebIPCheck.getInstance().setOnlyUseWorkingServices(false);
        }
    }

    public RouterPlugin getPlugin() {
        return routerPlugin;
    }

    protected ReconnectResult createReconnectResult() {
        return new ReconnectResult();
    }

    protected abstract void testRun() throws ReconnectException, InterruptedException;

    public void doOptimization(ReconnectResult res, ProcessCallBackAdapter processCallBackAdapter) throws InterruptedException {
        java.util.List<ReconnectResult> list = new ArrayList<ReconnectResult>();
        list.add(res);
        // int failed = 0;
        int success = 1;
        long duration = res.getSuccessDuration();
        long offlineDuration = res.getOfflineDuration();
        long maxOfflineDuration = res.getOfflineDuration();
        long maxSuccessDuration = res.getSuccessDuration();
        long startTime = res.getStartTime();
        for (int i = 1; i < JsonConfig.create(ReconnectConfig.class).getOptimizationRounds(); i++) {
            processCallBackAdapter.setProgress(this, (i * 100) / JsonConfig.create(ReconnectConfig.class).getOptimizationRounds());
            ReconnectResult r;
            try {
                r = validate();

            } catch (ReconnectException e) {
                e.printStackTrace();
                r = createReconnectResult();
            }
            list.add(r);
            if (r.isSuccess()) {

                success++;
                duration += r.getSuccessDuration();
                startTime = r.getStartTime();
                offlineDuration = Math.min(offlineDuration, r.getOfflineDuration());
                maxOfflineDuration = Math.max(maxOfflineDuration, r.getOfflineDuration());
                maxSuccessDuration = Math.max(maxSuccessDuration, r.getSuccessDuration());
            }

        }
        duration /= success;
        double successRate = success / (double) JsonConfig.create(ReconnectConfig.class).getOptimizationRounds();
        // increase successduration if successrate is lower than 1.0 (100%)
        res.setAverageSuccessDuration((long) (duration / successRate));
        res.setMaxOfflineDuration(maxOfflineDuration * 2);
        res.setMaxSuccessDuration(maxSuccessDuration * 4);
        res.setOfflineTime(offlineDuration);
        res.setStartTime(startTime);

    }

    public String getName() {
        return "Reconnect";
    }
}
