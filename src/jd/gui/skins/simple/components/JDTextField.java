package jd.gui.skins.simple.components;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;

public class JDTextField extends JTextField implements FocusListener {
    /**
     * 
     */
    private boolean autoselect = false;

    private static final long serialVersionUID = -4013847546677327448L;
    JUndoManager um = new JUndoManager(this);

    public JDTextField(String text) {
        super(text);
        addFocusListener(this);
    }

    public JDTextField() {
        super();
        addFocusListener(this);
    }

    public void setAutoSelect(boolean b) {
        autoselect = b;
    }

    public JUndoManager getUndoManager() {
        return um;
    }

    public void focusLost(FocusEvent fe) {
    }

    public void focusGained(FocusEvent fe) {
        if (autoselect) {
            setCaretPosition(0);
            if (getText() != null) {
                moveCaretPosition(getText().length());
            }
        }
    }
}