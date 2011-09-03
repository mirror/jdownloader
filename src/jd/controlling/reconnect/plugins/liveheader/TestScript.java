package jd.controlling.reconnect.plugins.liveheader;

import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ipcheck.IPController;

import org.appwork.storage.config.JsonConfig;

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

    public String getUser() {
        return user;
    }

    public String getScript() {
        return script;
    }

    private String model;
    private String routerIP;
    private String user;

    private String script;

    private int    testDuration;
    private String password;

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
        this.password = pass;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public boolean run(LiveHeaderReconnect plugin) throws InterruptedException {
        System.out.println("Test:\r\n" + getScript());
        JsonConfig.create(LiveHeaderReconnectSettings.class).setRouterIP(routerIP);
        JsonConfig.create(LiveHeaderReconnectSettings.class).setUserName(user);
        JsonConfig.create(LiveHeaderReconnectSettings.class).setPassword(password);
        JsonConfig.create(LiveHeaderReconnectSettings.class).setScript(script);
        ReconnectConfig settings = JsonConfig.create(ReconnectConfig.class);
        final int waitTimeBefore = settings.getSecondsToWaitForIPChange();
        try {

            settings.setSecondsToWaitForIPChange(30);
            try {
                final long start = System.currentTimeMillis();
                IPController.getInstance().invalidate();
                try {
                    if (ReconnectPluginController.getInstance().doReconnect(plugin)) {
                        // restore afterwards
                        return true;

                    }
                } finally {
                    testDuration = (int) (System.currentTimeMillis() - start);
                }

            } catch (final ReconnectException e) {
                e.printStackTrace();
            }
        } finally {
            settings.setSecondsToWaitForIPChange(waitTimeBefore);

        }
        return false;
    }

    public String getPassword() {
        return password;
    }

    public int getTestDuration() {
        return testDuration;
    }

}
