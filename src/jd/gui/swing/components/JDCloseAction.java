package jd.gui.swing.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class JDCloseAction extends AbstractAction {

    private static final long serialVersionUID = -771203720364300914L;
    private static final String JDL_PREFIX = "jd.gui.swing.components.JDCloseAction.";
    private int height;
    private int width;
    private ActionListener listener;

    /**
     * Returns the default close icon.
     */
    public static Icon getCloseIcon() {
        return JDTheme.II("gui.images.cancel", 16, 16);
    }

    public JDCloseAction(ActionListener listener) {
        this.listener = listener;

        Icon ic = getCloseIcon();
        this.height = ic.getIconHeight();
        this.width = ic.getIconWidth();
        this.putValue(AbstractAction.SMALL_ICON, ic);
        this.putValue(AbstractAction.SHORT_DESCRIPTION, JDL.L(JDL_PREFIX + "closeTab", "Close Tab"));
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public void actionPerformed(ActionEvent e) {
        if (listener != null) listener.actionPerformed(e);
    }

}
