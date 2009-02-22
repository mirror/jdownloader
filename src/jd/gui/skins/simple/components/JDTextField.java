package jd.gui.skins.simple.components;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public class JDTextField extends JTextField implements FocusListener {
    /**
     * 
     */
    private boolean autoselect = false;

    private static final long serialVersionUID = -4013847546677327448L;

    public JDTextField(String text) {
        super(text);
        addFocusListener(this);
        addUndoRedo(this);
    }

    public JDTextField() {
        this(null);
    }

    public void setAutoSelect(boolean b) {
        autoselect = b;
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

    /**
        Adds Undo/Redo capabilities to the passed in JTextArea, it also
        binds the JTextArea with the Ctrl-z and Ctrl-y key strockes.
     * @param undo 
     */
    static void addUndoRedo(JTextComponent area) {

        final UndoManager undo = new UndoManager();
        Document doc = area.getDocument();

        // Listen for undo and redo events
        doc.addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent evt) {
                undo.addEdit(evt.getEdit());
            }
        });

        //action for the undo command
        AbstractAction undo_action = new AbstractAction() {
            /**
             * 
             */
            private static final long serialVersionUID = -1151050746658519934L;

            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canUndo()) {
                        undo.undo();
                    }
                } catch (CannotUndoException e) {
                    e.printStackTrace();
                }
            }
        };
        //action for the redo command
        AbstractAction redo_action = new AbstractAction() {
            /**
             * 
             */
            private static final long serialVersionUID = 7373087464871959970L;

            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canRedo()) {
                        undo.redo();
                    }
                } catch (CannotRedoException e) {
                    e.printStackTrace();
                }
            }
        };
        
        // Create an undo action and add it to the text component
        area.getActionMap().put("Undo", undo_action);

        // Bind the undo action to ctl-Z
        area.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK), "Undo");

        // Create a redo action and add it to the text component
        area.getActionMap().put("Redo", redo_action);

        // Bind the redo action to ctl-Y
        area.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK), "Redo");        
    }

}