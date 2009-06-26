package jd.gui.userio.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import jd.controlling.JDLogger;
import jd.gui.skins.simple.components.JLinkButton;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class HelpDialog extends AbstractDialog {

    private static final long serialVersionUID = 5106956546862704641L;

    private String message;

    private String helpMessage;

    private String url;

    public HelpDialog(int flag, String title, String message, String helpMessage, String url) {
        super(flag, title, JDTheme.II("gui.images.config.tip", 32, 32), null, null);
        this.message = message;
        this.helpMessage = helpMessage;
        this.url = url;
        init();
    }

    @Override
    public JComponent contentInit() {
        JPanel content = new JPanel(new MigLayout("ins 0", "[left]5[right]"));

        JTextPane htmlArea = new JTextPane();
        htmlArea.setEditable(false);
        htmlArea.setContentType("text/html");
        htmlArea.setText(message);
        htmlArea.setOpaque(false);

        JButton help = new JButton(helpMessage == null ? JDL.L("gui.btn_help", "Help") : helpMessage);
        help.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    JLinkButton.openURL(url);
                } catch (Exception ex) {
                    JDLogger.exception(ex);
                }
                setReturnValue(false);
                dispose();
            }

        });

        content.add(htmlArea);
        content.add(help);

        return content;
    }

}
