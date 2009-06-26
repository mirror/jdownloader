package jd.gui.userio.dialog;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.UserIO;
import jd.gui.skins.simple.components.JDTextArea;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class TextAreaDialog extends AbstractDialog {

    private static final long serialVersionUID = 5129590048597691591L;

    private String message;

    private String def;

    private JDTextArea txtArea;

    public TextAreaDialog(String title, String message, String def) {
        super(UserIO.NO_COUNTDOWN, title, JDTheme.II("gui.images.config.tip", 32, 32), null, null);
        this.message = message;
        this.def = def;
        init();
    }

    @Override
    public JComponent contentInit() {
        JPanel panel = new JPanel(new MigLayout("ins 0, wrap 1", "[grow, fill]", "[]5[]"));
        panel.add(new JLabel(message));
        panel.add(txtArea = new JDTextArea(def), "h 100!");
        return panel;
    }

    public String getResult() {
        return txtArea.getText();
    }

}
