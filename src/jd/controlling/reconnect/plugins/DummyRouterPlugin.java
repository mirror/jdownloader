package jd.controlling.reconnect.plugins;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.controlling.ProgressController;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.os.CrossSystem;

public class DummyRouterPlugin extends RouterPlugin {

    private static final DummyRouterPlugin INSTANCE = new DummyRouterPlugin();

    public static RouterPlugin getInstance() {
        // TODO Auto-generated method stub
        return DummyRouterPlugin.INSTANCE;
    }

    @Override
    public void doReconnect(final ProgressController progress) throws ReconnectException {
        // TODO Auto-generated method stub

    }

    @Override
    public String getExternalIP() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JComponent getGUI() {
        final JPanel p = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]"));
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
        // TODO Auto-generated method stub
        return "DummyRouterPlugin";
    }

    @Override
    public String getName() {

        return JDL.L("jd.controlling.reconnect.plugins.DummyRouterPlugin.getName", "No Reconnect");
    }

    @Override
    public boolean isIPCheckEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isReconnectionEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setCanCheckIP(final boolean b) {
        // TODO Auto-generated method stub

    }

}
