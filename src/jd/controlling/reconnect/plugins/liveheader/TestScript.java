package jd.controlling.reconnect.plugins.liveheader;

import jd.config.Configuration;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.utils.JDUtilities;

public class TestScript {

    private String manufactor;

    public String getManufactor() {
        return manufactor;
    }

    public void setManufactor(String manufactor) {
        this.manufactor = manufactor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getUser() {
        return user;
    }

    public String getScript() {
        return script;
    }

    private String model;
    private String routerIP;
    private String user;
    private String pass;
    private String script;

    private int    testDuration;

    public TestScript(String manufactor, String model) {
        this.manufactor = manufactor;
        this.model = model;
    }

    public void setRouterIP(String ip) {
        this.routerIP = ip;
    }

    public String getRouterIP() {
        return routerIP;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String pass) {
        this.pass = pass;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public boolean run(LiveHeaderReconnect plugin) throws InterruptedException {

        plugin.setRouterIP(routerIP);
        plugin.setUser(user);
        plugin.setPassword(pass);
        plugin.setScript(script);

        final int waitTimeBefore = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_WAITFORIPCHANGE, 30);
        try {
            // at least after 5 (init) +10 seconds, we should be offline. if
            // we
            // are offline, reconnectsystem increase waittime about 120
            // seconds
            // anyway
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WAITFORIPCHANGE, 10);
            try {
                final long start = System.currentTimeMillis();
                IPController.getInstance().invalidate();
                if (ReconnectPluginController.getInstance().doReconnect(plugin)) {
                    // restore afterwards
                    return true;

                }
                testDuration = (int) (System.currentTimeMillis() - start);
            } catch (final ReconnectException e) {
                e.printStackTrace();
            }
        } finally {
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WAITFORIPCHANGE, waitTimeBefore);
        }
        return false;
    }

    public int getTestDuration() {
        return testDuration;
    }

}
