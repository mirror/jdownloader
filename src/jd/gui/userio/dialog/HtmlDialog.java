package jd.gui.userio.dialog;

import javax.swing.JComponent;
import javax.swing.JTextPane;

import jd.gui.skins.simple.components.JLinkButton;
import jd.utils.JDTheme;

public class HtmlDialog extends AbstractDialog {

    private static final long serialVersionUID = 5106956546862704641L;

    private String message;

    public HtmlDialog(int flag, String title, String message) {
        super(flag, title, JDTheme.II("gui.images.config.tip", 32, 32), null, null);
        this.message = message;
        init();
    }

    @Override
    public JComponent contentInit() {
        JTextPane htmlArea = new JTextPane();
        htmlArea.setEditable(false);
        htmlArea.setContentType("text/html");
        htmlArea.setText(message);
        htmlArea.setOpaque(false);
        htmlArea.requestFocusInWindow();
        htmlArea.addHyperlinkListener(JLinkButton.getHyperlinkListener());

        return htmlArea;
    }

}
