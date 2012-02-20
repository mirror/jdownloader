package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.ActionEvent;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.appwork.utils.StringUtils;
import org.jdownloader.actions.AppAction;

public class VariableAction extends AppAction {

    /**
	 * 
	 */
    private static final long serialVersionUID = -1614015840584960927L;
    private String            pattern;
    private JTextComponent    txtComp;

    public VariableAction(JTextComponent txtPackagename2, String name, String pattern) {
        super();
        setName(name);
        this.pattern = pattern;
        txtComp = txtPackagename2;
    }

    public void actionPerformed(ActionEvent e) {
        if (StringUtils.isEmpty(txtComp.getText())) {
            txtComp.setText(pattern);
        } else {
            int car = txtComp.getCaretPosition();

            try {
                txtComp.getDocument().insertString(car, pattern, null);
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }

    }

}
