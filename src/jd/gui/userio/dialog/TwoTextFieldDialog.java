package jd.gui.userio.dialog;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.UserIO;
import jd.gui.skins.simple.components.JDTextField;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class TwoTextFieldDialog extends AbstractDialog {

    private static final long serialVersionUID = -7426399217833694784L;

    private String messageOne;

    private String defOne;

    private String messageTwo;

    private String defTwo;

    private JDTextField txtFieldOne;

    private JDTextField txtFieldTwo;

    public TwoTextFieldDialog(String title, String messageOne, String defOne, String messageTwo, String defTwo) {
        super(UserIO.NO_COUNTDOWN, title, JDTheme.II("gui.images.config.tip", 32, 32), null, null);
        this.messageOne = messageOne;
        this.defOne = defOne;
        this.messageTwo = messageTwo;
        this.defTwo = defTwo;
        init();
    }

    @Override
    public JComponent contentInit() {
        JPanel panel = new JPanel(new MigLayout("ins 0, wrap 1", "[grow, fill]", "[]5[]10[]5[]"));
        panel.add(new JLabel(messageOne));
        panel.add(txtFieldOne = new JDTextField(defOne));
        panel.add(new JLabel(messageTwo));
        panel.add(txtFieldTwo = new JDTextField(defTwo));
        return panel;
    }

    public String[] getResult() {
        return new String[] { txtFieldOne.getText(), txtFieldTwo.getText() };
    }

}
