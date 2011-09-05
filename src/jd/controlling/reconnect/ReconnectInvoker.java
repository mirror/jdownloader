package jd.controlling.reconnect;

import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.controlling.reconnect.ipcheck.IPController;

import org.appwork.utils.logging.Log;

public abstract class ReconnectInvoker {
    private static final long OFFLINE_TIMEOUT = 30000;

    public abstract void run() throws ReconnectException, InterruptedException;

    public ReconnectResult validate() throws InterruptedException, ReconnectException {

        return validate(createReconnectResult());

    }

    public ReconnectResult validate(ReconnectResult ret) throws InterruptedException, ReconnectException {
        // Make sure that we are online
        while (IPController.getInstance().getIpState().isOffline()) {

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
                    return null;
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

    protected ReconnectResult createReconnectResult() {
        return new ReconnectResult();
    }

    protected abstract void testRun() throws ReconnectException, InterruptedException;

}
