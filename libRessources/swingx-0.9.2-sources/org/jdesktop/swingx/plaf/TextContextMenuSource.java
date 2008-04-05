/*
 * $Id: TextContextMenuSource.java,v 1.5 2006/05/14 08:19:45 dmouse Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.plaf;

import java.util.Map;

import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

/**
 * @author Jeanette Winzenburg
  */
public class TextContextMenuSource extends ContextMenuSource{

    String UNDO = "Undo";
    String CUT = "Cut";
    String COPY = "Copy";
    String PASTE = "Paste";
    String DELETE = "Delete";
    String SELECT_ALL = "Select All";
  
    String[] keys = { DefaultEditorKit.cutAction, DefaultEditorKit.copyAction,
            DefaultEditorKit.pasteAction, DefaultEditorKit.deleteNextCharAction, 
            null, // separator
            DefaultEditorKit.selectAllAction };

    String[] defaultValues = { CUT, COPY, PASTE, DELETE, null, SELECT_ALL,
    };

    public String[] getKeys() {
        return keys;
    }

    public void updateActionEnabled(JComponent component, ActionMap map) {
        if (!(component instanceof JTextComponent)) return;
        JTextComponent textComponent = (JTextComponent) component;
        boolean selectedText = textComponent.getSelectionEnd()
                - textComponent.getSelectionStart() > 0;
        boolean containsText = textComponent.getDocument().getLength() > 0;
        boolean editable = textComponent.isEditable();
        boolean copyProtected = (textComponent instanceof JPasswordField);
        boolean dataOnClipboard = textComponent.getToolkit()
                .getSystemClipboard().getContents(null) != null;
        map.get(DefaultEditorKit.cutAction).setEnabled(
                !copyProtected && editable && selectedText);
        map.get(DefaultEditorKit.copyAction).setEnabled(
                !copyProtected && selectedText);
        map.get(DefaultEditorKit.pasteAction).setEnabled(
                editable && dataOnClipboard);
        map.get(DefaultEditorKit.deleteNextCharAction).setEnabled(
                editable && selectedText);
        map.get(DefaultEditorKit.selectAllAction).setEnabled(containsText);

    }


    protected void initNames(Map<String, String> names) {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                names.put(keys[i],  getValue(keys[i], defaultValues[i]));
            }
        }
    }


    /**
     * @return resource prefix
     */
    protected String getResourcePrefix() {
        return "DefaultEditorKit.";
    }

}
