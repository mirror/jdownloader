package jd.gui.userio.dialog;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.controlling.JDLogger;
import jd.gui.skins.simple.components.JLinkButton;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class HtmlDialog extends AbstractDialog implements HyperlinkListener {

    private static final long serialVersionUID = 5106956546862704641L;

    private String message;

    public HtmlDialog(int flag, String title, String message) {
        super(flag, title, JDTheme.II("gui.images.config.tip"), null, null);
        this.message = message;
        init();
    }

    @Override
    public JComponent contentInit() {
        JPanel content = new JPanel(new MigLayout("ins 0"));

        JTextPane htmlArea = new JTextPane();
        htmlArea.setEditable(false);
        htmlArea.setContentType("text/html");
        htmlArea.setText(message);
        htmlArea.requestFocusInWindow();
        htmlArea.addHyperlinkListener(this);

        content.add(htmlArea);

        return content;
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                JLinkButton.openURL(e.getURL());
            } catch (Exception e1) {
                JDLogger.exception(e1);
            }
        }
    }

}
