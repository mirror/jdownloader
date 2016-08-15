package jd.controlling.reconnect.pluginsinc.easybox804;

import java.awt.Component;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.controlling.proxy.NoProxySelector;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPCheckException;
import jd.controlling.reconnect.ipcheck.IPCheckProvider;
import jd.controlling.reconnect.ipcheck.InvalidIPException;
import jd.http.Browser;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class EasyBox804 extends RouterPlugin implements IPCheckProvider {

    public static final String        ID = "EasyBox804";

    private Icon                      icon;

    private ReconnectInvoker          invoker;

    private ExtPasswordField          txtPassword;

    private EasyBox804ReconnectConfig config;

    private ExtTextField              txtIP;

    private Browser                   br;

    // @Override
    public int getIpCheckInterval() {
        return 1000;
    }

    // @Override
    public IP getExternalIP() throws IPCheckException {

        throw new InvalidIPException("Unknown");

    }

    public EasyBox804() {
        super();
        config = JsonConfig.create(EasyBox804ReconnectConfig.class);
        icon = new AbstractIcon(IconKey.ICON_RECONNECT, 16);
        // setIPCheckProvider(this);
        invoker = new ReconnectInvoker(this) {

            @Override
            protected void testRun() throws ReconnectException, InterruptedException {
                run();
            }

            @Override
            public void run() throws ReconnectException {
                try {
                    br = new Browser();

                    br.setVerbose(true);
                    br.setDebug(true);
                    br.setProxySelector(new NoProxySelector());
                    if (System.getProperty("fiddler") != null) {
                        br.setProxy(new HTTPProxy(HTTPProxy.TYPE.HTTP, "localhost", 8888));
                    }
                    br.getPage(absUrl("/main.cgi?page=login.html"));
                    String dmCookie = br.getRegex("dm_cookie\\s*=\\s*'([a-fA-F0-9]+)").getMatch(0);
                    logger.info("Dm Cookie: " + dmCookie);
                    br.getPage(absUrl("/main.cgi?js=rg_config.js"));
                    String authKey = br.getRegex("auth_key\\s*=\\s*'(\\d+)").getMatch(0);

                    logger.info("Auth key " + authKey);
                    logger.info("Hash: " + Hash.getMD5(config.getPassword() + authKey));
                    String soap = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header><DMCookie>" + dmCookie + "</DMCookie></soapenv:Header><soapenv:Body><cwmp:Login xmlns=\"\"><ParameterList><Username>vodafone</Username><Password>" + Hash.getMD5(config.getPassword() + authKey) + "</Password><AllowRelogin>0</AllowRelogin></ParameterList></cwmp:Login></soapenv:Body></soapenv:Envelope>";
                    Browser xhr = br.cloneBrowser();

                    xhr.getHeaders().put("SOAPAction", "cwmp:Login");
                    xhr.getHeaders().put("SOAPServer", "");
                    br.postPageRaw(absUrl("/data_model.cgi"), soap);

                    br.getPage(absUrl("/main.cgi?page=app.html"));
                    dmCookie = br.getRegex("dm_cookie\\s*=\\s*'([a-fA-F0-9]+)").getMatch(0);

                    soap = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header><DMCookie>" + dmCookie + "</DMCookie></soapenv:Header><soapenv:Body><cwmp:SetParameterValues xmlns=\"\"><ParameterList><ParameterValueStruct><Name>InternetGatewayDevice.WANDevice.6.X_JUNGO_COM_Reconnect</Name><Value>1</Value></ParameterValueStruct></ParameterList></cwmp:SetParameterValues></soapenv:Body></soapenv:Envelope>";
                    xhr = br.cloneBrowser();

                    xhr.getHeaders().put("SOAPAction", "cwmp:SetParameterValues");
                    xhr.getHeaders().put("SOAPServer", "");
                    br.postPageRaw(absUrl("/data_model.cgi"), soap);

                } catch (IOException e) {
                    e.printStackTrace();
                    throw new ReconnectException(e);
                }
            }

        };
    }

    private String absUrl(String string) throws IOException {
        final String host = config.getRouterIP();
        if (StringUtils.isEmpty(host)) {
            throw new IOException("host is empty for url:" + string);
        } else {
            return "http://" + host + string;
        }
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
        txtPassword.setText(config.getPassword());
        txtIP.setText(config.getRouterIP());
        p.add(label(_GUI.T.lit_router_ip()));
        p.add(txtIP);
        p.add(label(_GUI.T.lit_password()));
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
        return ID;
    }

    @Override
    public String getName() {
        return "EasyBox 804 Reconnect";
    }

    @Override
    public ReconnectInvoker getReconnectInvoker() {
        return invoker;
    }

}