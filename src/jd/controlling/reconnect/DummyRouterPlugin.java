package jd.controlling.reconnect;

import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.translate._JDT;

public class DummyRouterPlugin extends RouterPlugin {
    private static final DummyRouterPlugin INSTANCE = new DummyRouterPlugin();

    public static RouterPlugin getInstance() {
        return DummyRouterPlugin.INSTANCE;
    }

    private Icon icon;

    private DummyRouterPlugin() {
        icon = new AbstractIcon(IconKey.ICON_DELETE, 16);
    }

    @Override
    public JComponent getGUI() {
        final JPanel p = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]"));
        p.setOpaque(false);
        final JTextPane text = new JTextPane();
        final Font font = text.getFont();
        text.setContentType("text/html");
        text.setFont(font);
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
        text.setText(_JDT.T.jd_controlling_reconnect_plugins_DummyRouterPlugin_getGUI2());
        p.add(text);
        return p;
    }

    @Override
    public String getID() {
        return "DummyRouterPlugin";
    }

    @Override
    public String getName() {
        return _JDT.T.jd_controlling_reconnect_plugins_DummyRouterPlugin_getName();
    }

    @Override
    public Icon getIcon16() {
        return icon;
    }

    @Override
    public ReconnectInvoker getReconnectInvoker() {
        return null;
    }
}