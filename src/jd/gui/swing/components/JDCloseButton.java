package jd.gui.swing.components;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.utils.JDUtilities;

public abstract class JDCloseButton extends JButton implements ActionListener {

    private static final long serialVersionUID = -8939894934856422310L;

    private JDCloseAction closeAction;

    public JDCloseButton() {
        super();

        init();
    }

    private void init() {
        closeAction = new JDCloseAction(this);
        setAction(closeAction);
        setContentAreaFilled(false);
        setBorderPainted(false);
        addMouseListener(new JDMouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                setContentAreaFilled(true);
                setBorderPainted(true);
            }

            public void mouseExited(MouseEvent e) {
                setContentAreaFilled(false);
                setBorderPainted(false);
            }
        });
        setPreferredSize(new Dimension(closeAction.getWidth(), closeAction.getHeight()));
        if (JDUtilities.getJavaVersion() < 1.6) setHideActionText(true);
    }

    public int getIconHeight() {
        return closeAction.getHeight();
    }

    public int getIconWidth() {
        return closeAction.getWidth();
    }

}
