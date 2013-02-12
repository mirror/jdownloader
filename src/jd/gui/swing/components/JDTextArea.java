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

package jd.gui.swing.components;

import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextArea;

import jd.gui.swing.JUndoManager;

public class JDTextArea extends JTextArea implements FocusListener {

    private boolean           autoselect       = false;

    private static final long serialVersionUID = -4013847546677327448L;

    public JDTextArea(String text) {
        super(text);
        addFocusAndUndo();
    }

    public JDTextArea(int a, int b) {
        super(a, b);
        addFocusAndUndo();
    }

    public Dimension getPreferredScrollableViewportSize() {

        return new Dimension(100, super.getPreferredScrollableViewportSize().height);
    }

    private void addFocusAndUndo() {
        addFocusListener(this);
        JUndoManager.addUndoRedo(this);
    }

    public JDTextArea() {
        this(null);

        // setLineWrap(true);
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
}
