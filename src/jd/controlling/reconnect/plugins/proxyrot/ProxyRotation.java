package jd.controlling.reconnect.plugins.proxyrot;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.IP;
import jd.controlling.reconnect.IP_NA;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.RouterPlugin;
import jd.http.Browser;
import jd.http.JDProxy;
import jd.parser.Regex;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.TextComponentChangeListener;

/**
 * Rotates proxy instead of a reconnect
 * 
 * @author thomas
 * 
 */
public class ProxyRotation extends RouterPlugin {

    private static final String PROXY_LIST = "PROXY_LIST";
    private JTextPane           txtList;

    public ProxyRotation() {
        super();
    }

    @Override
    protected void performReconnect() throws ReconnectException {
        try {
            final String[] lines = Regex.getLines(this.getProxyList());
            // get next line in list;
            String next = null;
            int i;
            for (i = 0; i < lines.length; i++) {
                if (!lines[i].trim().startsWith("#")) {
                    next = lines[i];
                    break;
                }
            }
            if (next == null) { return; }
            // resort proxylist;
            // current proxy to end of list.
            final StringBuilder sb = new StringBuilder();
            for (int x = i + 1; x < lines.length; x++) {
                sb.append(lines[x]);
                sb.append("\r\n");
            }
            for (int x = 0; x <= i; x++) {
                sb.append(lines[x]);
                sb.append("\r\n");
            }
            this.setProxyList(sb.toString());

            // Pasre next prpxy line NOW
            i = next.indexOf("://");
            if (i < 4) { throw new Exception("Proxylist Format error in " + next); }
            final String type = next.substring(0, i);
            String password = null;
            String username = null;
            String ip = null;
            String port = null;
            next = next.substring(i + 3);
            if (next.contains("@")) {
                i = next.indexOf("@");
                final String logins = next.substring(0, i);
                next = next.substring(i + 1);

                if (logins.contains(":")) {
                    final String[] splitted = logins.split(":");
                    username = splitted[0];
                    password = splitted[1];
                } else {
                    username = logins;
                }
            }
            final String[] splitted = next.split(":");
            ip = splitted[0];
            if (splitted.length > 1) {
                port = splitted[1];
            } else {
                port = "8080";
            }

            final JDProxy proxy = new JDProxy(type.equalsIgnoreCase("socks") ? JDProxy.Type.SOCKS : JDProxy.Type.HTTP, ip, Integer.parseInt(port));
            proxy.setUser(username);
            proxy.setPass(password);
            JDLogger.getLogger().info("Use Proxy: " + proxy);
            Browser.setGlobalProxy(proxy);
            // force the Reconnecter to verify IP again

        } catch (final Exception e) {
            e.printStackTrace();
            throw new ReconnectException(e);

        }
    }

    @Override
    public IP getExternalIP() {
        return IP_NA.IPCHECK_UNSUPPORTED;
    }

    @Override
    public JComponent getGUI() {
        // TODO: use table someday
        final JPanel p = new JPanel(new MigLayout("ins 15,wrap 1", "[grow,fill]", "[][][][][grow,fill]"));
        p.add(new JLabel(JDL.L("jd.controlling.reconnect.plugins.proxyRot.ProxyRotation.label.proxylist", "ProxyList")));
        p.add(new JLabel(JDL.L("jd.controlling.reconnect.plugins.proxyRot.ProxyRotation.label.formatexample", "Format examples:")));
        p.add(new JLabel(JDL.L("jd.controlling.reconnect.plugins.proxyRot.ProxyRotation.label.formatexample.http", "http://user:password@ip:port")),"gapleft 32");
        p.add(new JLabel(JDL.L("jd.controlling.reconnect.plugins.proxyRot.ProxyRotation.label.formatexample.socks", "socks://user:password@ip:port")),"gapleft 32");         
     
        this.txtList = new JTextPane();
        p.add(new JScrollPane(this.txtList));
        new TextComponentChangeListener(this.txtList) {
            @Override
            protected void onChanged(final DocumentEvent e) {

                ProxyRotation.this.setProxyList(ProxyRotation.this.txtList.getText());

            }

        };

        this.updateGUI();
        return p;
    }

    @Override
    public String getID() {
        return "ProxyRotation";
    }

    @Override
    public String getName() {
        return JDL.L("jd.controlling.reconnect.plugins.proxyRot.ProxyRotation.getName", "Proxy Rotation");
    }

    private String getProxyList() {

        return this.getStorage().get(ProxyRotation.PROXY_LIST, SubConfiguration.getConfig("PROXYROTATION").getStringProperty("PARAM_PROXYLIST", ""));
    }

    @Override
    public boolean isIPCheckEnabled() {
        return false;
    }

    @Override
    public boolean isReconnectionEnabled() {
        return true;
    }

    @Override
    public void setCanCheckIP(final boolean b) {
    }

    private void setProxyList(final String list) {
        this.getStorage().put(ProxyRotation.PROXY_LIST, list);
        this.updateGUI();
    }

    private void updateGUI() {
        new EDTRunner() {
            protected void runInEDT() {
                try {
                    ProxyRotation.this.txtList.setText(ProxyRotation.this.getProxyList());
                } catch (final java.lang.IllegalStateException e) {
                    // throws an java.lang.IllegalStateException if the caller

                }

            }

        };

    }

}
