package jd.controlling.reconnect.pluginsinc.speedporthybrid;

import java.awt.Component;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.Hash;
import org.appwork.utils.logging2.extmanager.Log;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.RouterPlugin;
import jd.http.Browser;
import jd.http.QueryInfo;
import net.miginfocom.swing.MigLayout;

/**
 * Plugin to use an extern tool for reconnection
 */
public class SpeedPortHybrid extends RouterPlugin {

    public static final String             ID              = "SpeedPortHybrid";

    private static final Class             SpeedPortHybrid = null;

    private Icon                           icon;

    private ReconnectInvoker               invoker;

    private ExtPasswordField               txtPassword;

    private SpeedPortHybridReconnectConfig config;

    private ExtTextField                   txtIP;

    public SpeedPortHybrid() {
        super();
        config = JsonConfig.create(SpeedPortHybridReconnectConfig.class);
        icon = new AbstractIcon(IconKey.ICON_RECONNECT, 16);
        invoker = new ReconnectInvoker(this) {
            @Override
            protected void testRun() throws ReconnectException, InterruptedException {
                run();
            }

            @Override
            public void run() throws ReconnectException {

                try {
                    Browser br = new Browser();
                    br.setVerbose(true);
                    br.setDebug(true);
                    br.setProxy(new HTTPProxy(TYPE.HTTP, "localhost", 8888));

                    br.postPage("http://" + config.getRouterIP() + "/data/Login.json?lang=de", new QueryInfo().append("csrf_token", "nulltoken", true).append("showpw", "0", true).append("challengev", "null", true));
                    String challengev = br.getRegex("\"challengev\",.*?\"varvalue\":\"(.*?)\"").getMatch(0);

                    Log.info("Challenge: " + challengev);
                    br.postPage("http://" + config.getRouterIP() + "/data/Login.json?lang=de", new QueryInfo().append("csrf_token", "nulltoken", true).append("showpw", "0", true).append("password", Hash.getSHA256(challengev) + ":" + config.getPassword(), true));

                } catch (IOException e) {
                    throw new ReconnectException(e);
                }
            }
        };
    }

    @Override
    public Icon getIcon16() {
        return icon;
    }

    @Override
    public JComponent getGUI() {

        final JPanel p = new JPanel(new MigLayout("ins 0,wrap 2", "[][grow,fill]", "[][][grow,fill][]"));
        p.setOpaque(false);

        txtPassword = new ExtPasswordField() {
            public void onChanged() {
                config.setPassword(txtPassword.getText());
            };
        };

        txtIP = new ExtTextField() {
            @Override
            public void onChanged() {
                config.setRouterIP(txtIP.getText());
            }
        };
        p.add(label("Router IP"));
        p.add(txtIP);
        p.add(label("Password"));
        p.add(txtPassword);
        return p;
    }

    private Component label(String string) {
        JLabel ret = new JLabel(string);
        SwingUtils.toBold(ret);
        ret.setEnabled(false);
        ret.setHorizontalAlignment(JLabel.RIGHT);
        return ret;
    }

    @Override
    public String getID() {
        return "SpeedPortHybrid";
    }

    @Override
    public String getName() {
        return "Speed Port Hybrid Reconnect";
    }

    @Override
    public ReconnectInvoker getReconnectInvoker() {
        return invoker;
    }

}