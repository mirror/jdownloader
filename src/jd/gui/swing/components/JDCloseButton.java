package jd.gui.swing.components;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;

import org.appwork.utils.Application;

public abstract class JDCloseButton extends JButton implements ActionListener {

    private static final long serialVersionUID = -8939894934856422310L;

    private JDCloseAction     closeAction;

    public JDCloseButton() {
        super();

        init();
    }

    private void init() {
        closeAction = new JDCloseAction(this);
        setAction(closeAction);
        setBorder(false);
        addMouseListener(new JDMouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBorder(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBorder(false);
            }
        });
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setBorder(false);
            }
        });
        setPreferredSize(new Dimension(closeAction.getWidth(), closeAction.getHeight()));
        if (Application.getJavaVersion() >= 16000000) setHideActionText(true);
    }

    private final void setBorder(boolean state) {
        setContentAreaFilled(state);
        setBorderPainted(state);
    }

    public int getIconHeight() {
        return closeAction.getHeight();
    }

    public int getIconWidth() {
        return closeAction.getWidth();
    }

}
