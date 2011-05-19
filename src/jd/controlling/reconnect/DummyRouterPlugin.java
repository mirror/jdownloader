package jd.controlling.reconnect;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class DummyRouterPlugin extends RouterPlugin {

    private static final DummyRouterPlugin INSTANCE = new DummyRouterPlugin();

    public static RouterPlugin getInstance() {
        return DummyRouterPlugin.INSTANCE;
    }

    private ImageIcon icon;

    private DummyRouterPlugin() {
        icon = NewTheme.I().getIcon("delete", 16);
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
        text.setText(_JDT._.jd_controlling_reconnect_plugins_DummyRouterPlugin_getGUI2());
        p.add(text);
        return p;
    }

    @Override
    public String getID() {
        return "DummyRouterPlugin";
    }

    @Override
    public String getName() {
        return _JDT._.jd_controlling_reconnect_plugins_DummyRouterPlugin_getName();
    }

    @Override
    public boolean isReconnectionEnabled() {
        return false;
    }

    @Override
    protected void performReconnect() throws ReconnectException {

    }

    @Override
    public ImageIcon getIcon16() {
        return icon;
    }
}