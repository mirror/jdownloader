package jd.gui.skins.simple.components;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextArea;

public class JDTextArea extends JTextArea implements FocusListener {
    /**
     * 
     */
    private boolean autoselect = false;

    private static final long serialVersionUID = -4013847546677327448L;
//    JUndoManager um = new JUndoManager(this);

    public JDTextArea(String text) {
        super(text);
        addFocusAndUndo();
    }

    public JDTextArea(int a, int b) {
        super(a, b);
        addFocusAndUndo();
    }

    private void addFocusAndUndo() {
        addFocusListener(this);
        JDTextField.addUndoRedo(this);
    }

    public JDTextArea() {
        this(null);
    }

    public void setAutoSelect(boolean b) {
        autoselect = b;
    }

//    public JUndoManager getUndoManager() {
//        return um;
//    }

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
