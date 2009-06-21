package jd.gui.userio.dialog;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import jd.gui.skins.simple.components.JLinkButton;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class HtmlDialog extends AbstractDialog {

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
        htmlArea.addHyperlinkListener(JLinkButton.getHyperlinkListener());

        content.add(htmlArea);

        return content;
    }

}
