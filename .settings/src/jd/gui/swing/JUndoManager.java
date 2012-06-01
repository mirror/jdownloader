//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import jd.controlling.JDLogger;

public class JUndoManager {

    /**
     * Adds Undo/Redo capabilities to the passed in JTextComponent, it also
     * binds the JTextComponent with the Ctrl-z and Ctrl-y key strokes.
     * 
     * @param area
     */
    public static void addUndoRedo(final JTextComponent area) {

        final UndoManager undo = new UndoManager();

        // Listen for undo and redo events
        area.getDocument().addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(final UndoableEditEvent evt) {
                undo.addEdit(evt.getEdit());
            }
        });

        // action for the undo command
        final AbstractAction undo_action = new AbstractAction() {

            private static final long serialVersionUID = -1151050746658519934L;

            public void actionPerformed(final ActionEvent evt) {
                try {
                    if (undo.canUndo()) {
                        undo.undo();
                    }
                } catch (CannotUndoException e) {
                    JDLogger.exception(e);
                }
            }

        };

        // action for the redo command
        final AbstractAction redo_action = new AbstractAction() {

            private static final long serialVersionUID = 7373087464871959970L;

            public void actionPerformed(final ActionEvent evt) {
                try {
                    if (undo.canRedo()) {
                        undo.redo();
                    }
                } catch (CannotRedoException e) {
                    JDLogger.exception(e);
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
