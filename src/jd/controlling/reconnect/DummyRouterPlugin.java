package jd.controlling.reconnect;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.os.CrossSystem;

public class DummyRouterPlugin extends RouterPlugin {

    private static final DummyRouterPlugin INSTANCE = new DummyRouterPlugin();

    public static RouterPlugin getInstance() {
        return DummyRouterPlugin.INSTANCE;
    }

    private DummyRouterPlugin() {

    }

    @Override
    protected void performReconnect() throws ReconnectException {

    }

    @Override
    public String getExternalIP() {
        return null;
    }

    @Override
    public JComponent getGUI() {
        final JPanel p = new JPanel(new MigLayout("ins 15,wrap 1", "[grow,fill]", "[][grow,fill]"));
        final JTextPane text = new JTextPane();
        text.setContentType("text/html");
        text.addHyperlinkListener(new HyperlinkListener() {

            public void hyperlinkUpdate(final HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    CrossSystem.openURL(e.getURL());
                }
            }

        });
        text.setEditable(false);
        text.setBackground(null);
        text.setOpaque(false);
        text.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        text.setText(JDL.L("jd.controlling.reconnect.plugins.DummyRouterPlugin.getGUI", "<b><u>No Reconnect selected</u></b><br/><p>Reconnection is an advanced approach for skipping long waits that some hosts impose on free users. <br>It is not helpful while using a premium account.</p><p>Read more about Reconnect <a href='http://board.jdownloader.org/showthread.php?t=16450'>here</a></p>"));
        p.add(text);
        return p;
    }

    @Override
    public String getID() {
        return "DummyRouterPlugin";
    }

    @Override
    public String getName() {
        return JDL.L("jd.controlling.reconnect.plugins.DummyRouterPlugin.getName", "No Reconnect");
    }

    @Override
    public boolean isIPCheckEnabled() {
        return false;
    }

    @Override
    public boolean isReconnectionEnabled() {
        return false;
    }

    @Override
    public void setCanCheckIP(final boolean b) {
    }

}
