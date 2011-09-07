package jd.controlling.reconnect;

import java.util.ArrayList;

import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.controlling.reconnect.ipcheck.IPController;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.ProcessCallBackAdapter;
import org.appwork.utils.logging.Log;

public abstract class ReconnectInvoker {
    private static final long OFFLINE_TIMEOUT = 30000;
    private RouterPlugin      routerPlugin;

    public ReconnectInvoker(RouterPlugin routerPlugin) {
        this.routerPlugin = routerPlugin;
    }

    public abstract void run() throws ReconnectException, InterruptedException;

    public ReconnectResult validate() throws InterruptedException, ReconnectException {

        return validate(createReconnectResult());

    }

    public ReconnectResult validate(ReconnectResult ret) throws InterruptedException, ReconnectException {
        ret.setInvoker(this);

        // Make sure that we are online
        while (IPController.getInstance().getIpState().isOffline()) {
            IPController.getInstance().invalidate();
            Thread.sleep(5000);

            IPController.getInstance().validate();
        }
        System.out.println("IP BEFORE=" + IPController.getInstance().getIP());

        BalancedWebIPCheck.getInstance().setOnlyUseWorkingServices(true);
        IPController.getInstance().invalidate();

        try {

            ret.setStartTime(System.currentTimeMillis());
            testRun();

            Thread.sleep(1 * 1000);
            IPController ipc = IPController.getInstance();
            if (ipc.validate()) {
                // wow this hsa been fast
                Log.L.info("Successful: REconnect has been very fast!");
                ret.setSuccess(true);
                ret.setOfflineTime(System.currentTimeMillis());
                ret.setSuccessTime(System.currentTimeMillis());
                return ret;
            }
            Log.L.info("Script done. Wait for offline");
            do {

                // wait until we are offline
                Thread.sleep(1 * 1000);

                if (!ipc.validate() && !ipc.getIpState().isOffline() && (System.currentTimeMillis() - ret.getStartTime()) > OFFLINE_TIMEOUT) {
                    // we are not offline after 30 seconds
                    Log.L.info("Disconnect failed. Still online after " + OFFLINE_TIMEOUT + " ms");
                    return ret;
                }
            } while (!ipc.getIpState().isOffline() && ipc.isInvalidated());
            ret.setOfflineTime(System.currentTimeMillis());

            Log.L.info("Offline after " + ret.getOfflineDuration() + " ms");
            if (ipc.isInvalidated()) {
                Log.L.info("Wait for online status");
                // we have to wait LOOOONG here. reboot may take its time
                final long endTime = System.currentTimeMillis() + 450 * 1000;
                while (System.currentTimeMillis() < endTime) {
                    /* ip change detected then we can stop */
                    long s = System.currentTimeMillis();
                    if (ipc.validate()) {
                        ret.setSuccessTime(System.currentTimeMillis());
                        ret.setSuccess(true);
                        Log.L.info("Successful: REconnect after " + ret.getSuccessDuration() + " ms");

                        return ret;
                        //
                    }
                    if (!ipc.getIpState().isOffline()) {
                        Log.L.info("Failed. returned from offline. But no new ip");
                        return ret;

                    }

                    Thread.sleep(Math.max(0, 1000 - (System.currentTimeMillis() - s)));
                }
                Log.L.info("Connect failed! Maybe router restart is required. This should NEVER happen!");
                return ret;
            } else {
                ret.setSuccessTime(System.currentTimeMillis());
                ret.setSuccess(true);
                Log.L.info("Successful: REconnect after " + ret.getSuccessDuration() + " ms");
                return ret;
            }

        } finally {

            System.out.println("IP AFTER=" + IPController.getInstance().getIP());
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
        ArrayList<ReconnectResult> list = new ArrayList<ReconnectResult>();
        list.add(res);
        // int failed = 0;
        int success = 1;
        long duration = res.getSuccessDuration();
        long offlineDuration = res.getOfflineDuration();
        long maxSuccessDuration = res.getSuccessDuration();
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
                offlineDuration = Math.min(offlineDuration, r.getOfflineDuration());
                maxSuccessDuration = Math.max(maxSuccessDuration, r.getSuccessDuration());
            }

        }
        duration /= success;
        double successRate = success / (double) JsonConfig.create(ReconnectConfig.class).getOptimizationRounds();
        // increase successduration if successrate is lower than 1.0 (100%)
        res.setAverageSuccessDuration((long) (duration / successRate));

        res.setMaxSuccessDuration(maxSuccessDuration * 4);

    }

    public String getName() {
        return "Reconnect";
    }
}
