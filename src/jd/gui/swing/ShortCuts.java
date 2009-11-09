/**
 * Some functions (marked) taken from {@link java.awt.AWTKeyStroke}
 * 
 * 
 * @(#)AWTKeyStroke.java    1.28 06/02/06
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
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

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import javax.swing.KeyStroke;

import jd.utils.locale.JDL;

public class ShortCuts {
    /**
     * DO NOT MOVE THIS PREFIX. required by SrcParser in this java file
     */
    private static final String JDL_PREFIX = "jd.gui.swing.ShortCuts.";

    public static String getAcceleratorString(KeyStroke ks) {
        if (ks == null) return null;
        StringBuilder builder = new StringBuilder();
        builder.append(getModifiersText(ks.getModifiers()));
        if (builder.length() > 0) builder.append('+');
        if (ks.getKeyCode() == KeyEvent.VK_UNDEFINED) {
            return builder.append(ks.getKeyChar()).toString();
        } else {
            return builder.append(getVKText(ks.getKeyCode())).toString();
        }
    }

    /**
     * Taken from {@link java.awt.AWTKeyStroke}
     * 
     * 
     * @(#)AWTKeyStroke.java 1.28 06/02/06
     * 
     *                       Copyright 2006 Sun Microsystems, Inc. All rights
     *                       reserved. SUN PROPRIETARY/CONFIDENTIAL. Use is
     *                       subject to license terms.
     * 
     *                       Modified with translation code by JDTEam
     */
    public static String getModifiersText(int modifiers) {
        StringBuilder buf = new StringBuilder();

        if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(JDL.L(JDL_PREFIX + "key.ctrl", "ctrl"));
        }
        if ((modifiers & KeyEvent.META_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(JDL.L(JDL_PREFIX + "key.meta", "meta"));
        }
        if ((modifiers & KeyEvent.ALT_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(JDL.L(JDL_PREFIX + "key.alt", "alt"));
        }
        if ((modifiers & KeyEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(JDL.L(JDL_PREFIX + "key.altGr", "alt Gr"));
        }
        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(JDL.L(JDL_PREFIX + "key.shift", "shift"));
        }
        if ((modifiers & KeyEvent.BUTTON1_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(JDL.L(JDL_PREFIX + "key.button1", "button1"));
        }
        if ((modifiers & KeyEvent.BUTTON2_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(JDL.L(JDL_PREFIX + "key.button2", "button2"));
        }
        if ((modifiers & KeyEvent.BUTTON3_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(JDL.L(JDL_PREFIX + "key.button3", "button3"));
        }

        return buf.toString();
    }

    /**
     * Taken from {@link java.awt.AWTKeyStroke}
     * 
     * 
     * @(#)AWTKeyStroke.java 1.28 06/02/06
     * 
     *                       Copyright 2006 Sun Microsystems, Inc. All rights
     *                       reserved. SUN PROPRIETARY/CONFIDENTIAL. Use is
     *                       subject to license terms.
     * 
     * 
     *                       Modified with translation code by JDTEam
     *                       Translation uses dynamic keys. this is not
     *                       recommended, but there is not better solution
     * 
     */
    private static String getVKText(int keyCode) {
        int expected_modifiers = (Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        Field[] fields = KeyEvent.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                if (fields[i].getModifiers() == expected_modifiers && fields[i].getType() == Integer.TYPE && fields[i].getName().startsWith("VK_") && fields[i].getInt(KeyEvent.class) == keyCode) { return JDL.L(JDL_PREFIX + fields[i].getName(), fields[i].getName().substring(3)); }
            } catch (IllegalAccessException e) {
            }
        }
        return "UNKNOWN";
    }

}
