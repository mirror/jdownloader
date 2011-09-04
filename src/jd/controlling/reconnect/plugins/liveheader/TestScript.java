package jd.controlling.reconnect.plugins.liveheader;

import java.io.IOException;
import java.util.HashMap;

import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.controlling.reconnect.plugins.liveheader.remotecall.RouterData;
import jd.http.Request;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging.Log;
import org.w3c.dom.Node;

public class TestScript {

    private int                 testDuration;

    private LiveHeaderReconnect liveHeaderReconnect;

    private RouterData          routerData;

    private String              routerIP;

    private String              username;

    private String              password;

    private int                 offlineDuration;

    public String getUsername() {
        return username;
    }

    public TestScript(LiveHeaderReconnect plugin, RouterData dat, String gatewayAdressHost, String username, String password) {
        this.liveHeaderReconnect = plugin;
        this.routerIP = gatewayAdressHost;
        this.username = username;
        this.password = password;
        routerData = dat;
    }

    public RouterData getRouterData() {
        return routerData;
    }

    public boolean run(LiveHeaderReconnect plugin) throws InterruptedException {
        System.out.println("Test:\r\n" + routerData.getScript());

        ReconnectConfig settings = JsonConfig.create(ReconnectConfig.class);
        long start = System.currentTimeMillis();
        IPController ipc = IPController.getInstance();
        // Make sure that we are online
        while (IPController.getInstance().getCurrentLog().isOffline()) {
            Log.L.severe("We are offline at teststart");
            Thread.sleep(5000);
            IPController.getInstance().validate();
        }
        System.out.println("IP BEFORE=" + IPController.getInstance().getIP());

        BalancedWebIPCheck.getInstance().setOnlyUseWorkingServices(true);
        try {

            IPController.getInstance().invalidate();

            liveHeaderReconnect.invoke(new LHProcessFeedback() {
                private int successRequests;
                private int failedRequests;

                {
                    successRequests = 0;
                    failedRequests = 0;
                }

                public void onRequestExceptionOccured(IOException e, String request) throws ReconnectFailedException {
                    failedRequests++;

                    if (failedRequests > successRequests) throw new ReconnectFailedException("Request Failed");

                }

                public void onVariablesUpdated(HashMap<String, String> variables) throws ReconnectFailedException {
                }

                public void onVariableParserFailed(String pattern, Request request) throws ReconnectFailedException {
                    throw new ReconnectFailedException("Variable Parser Failed");
                }

                public void onRequesterror(Request request) throws ReconnectFailedException {
                }

                public void onNewStep(String nodeName, Node toDo) throws ReconnectFailedException {
                }

                public void onRequestOK(Request request) throws ReconnectFailedException {
                    successRequests++;
                }

            }, routerData.getScript(), username, password, routerIP);
            start = System.currentTimeMillis();
            Thread.sleep(1 * 1000);

            if (ipc.validate()) {
                // wow this hsa been fast
                Log.L.info("Successful: REconnect has been very fast!");
                return true;
            }
            Log.L.info("Script done. Wait for offline");
            do {

                // wait until we are offline
                Thread.sleep(1 * 1000);

                offlineDuration = (int) (System.currentTimeMillis() - start);

                if (!ipc.validate() && !ipc.getCurrentLog().isOffline() && offlineDuration > 30000) {
                    // we are not offline after 30 seconds
                    Log.L.info("Disconnect failed. Still online after " + offlineDuration + " ms");
                    return false;
                }
            } while (!ipc.getCurrentLog().isOffline() && ipc.isInvalidated());

            Log.L.info("Offline after " + offlineDuration + " ms");
            if (ipc.isInvalidated()) {
                Log.L.info("Wait for online status");
                // we have to wait LOOOONG here. reboot may take its time
                final long endTime = System.currentTimeMillis() + 450 * 1000;
                while (System.currentTimeMillis() < endTime) {
                    /* ip change detected then we can stop */
                    if (ipc.validate()) {
                        Log.L.info("Successful: REconnect after " + (System.currentTimeMillis() - start) + " ms");
                        return true;
                        //
                    }
                    if (!ipc.getCurrentLog().isOffline()) {
                        Log.L.info("Failed. returned from offline. But no new ip");
                        return false;

                    }

                    Thread.sleep(1000 * 1);
                }
                Log.L.info("Connect failed! Maybe router restart is required. This should NEVER happen!");
                return false;
            } else {
                Log.L.info("Successful: after " + (int) (System.currentTimeMillis() - start) + " ms");
                return true;
            }

        } catch (final ReconnectException e) {
            e.printStackTrace();
        } finally {
            testDuration = (int) (System.currentTimeMillis() - start);
            System.out.println("IP AFTER=" + IPController.getInstance().getIP());
            BalancedWebIPCheck.getInstance().setOnlyUseWorkingServices(false);
        }
        return false;
    }

    public int getOfflineDuration() {
        return offlineDuration;
    }

    public String getPassword() {
        return password;
    }

    public int getTestDuration() {
        return testDuration;
    }

    public void setTestDuration(int testDuration) {
        this.testDuration = testDuration;
    }

    public void setOfflineDuration(int offlineDuration) {
        this.offlineDuration = offlineDuration;
    }

    public String getRouterIP() {
        return routerIP;
    }

}
