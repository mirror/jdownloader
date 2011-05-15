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

import javax.swing.KeyStroke;

import org.jdownloader.gui.translate._GUI;

public class ShortCuts {
    /**
     * DO NOT MOVE THIS PREFIX. required by SrcParser in this java file
     */

    public static String getAcceleratorString(KeyStroke ks) {
        if (ks == null) return null;
        final StringBuilder builder = new StringBuilder();
        builder.append(getModifiersText(ks.getModifiers()));
        if (builder.length() > 0) builder.append('+');
        if (ks.getKeyCode() == KeyEvent.VK_UNDEFINED) {
            return builder.append(ks.getKeyChar()).toString();
        } else {
            return builder.append(KeyEvent.getKeyText(ks.getKeyCode())).toString();
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
    private static String getModifiersText(final int modifiers) {
        final StringBuilder buf = new StringBuilder();

        if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(_GUI._.jd_gui_swing_ShortCuts_key_ctrl());
        }
        if ((modifiers & KeyEvent.META_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(_GUI._.jd_gui_swing_ShortCuts_key_meta());
        }
        if ((modifiers & KeyEvent.ALT_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(_GUI._.jd_gui_swing_ShortCuts_key_alt());
        }
        if ((modifiers & KeyEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(_GUI._.jd_gui_swing_ShortCuts_key_altGr());
        }
        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(_GUI._.jd_gui_swing_ShortCuts_key_shift());
        }
        if ((modifiers & KeyEvent.BUTTON1_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(_GUI._.jd_gui_swing_ShortCuts_key_button1());
        }
        if ((modifiers & KeyEvent.BUTTON2_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(_GUI._.jd_gui_swing_ShortCuts_key_button2());
        }
        if ((modifiers & KeyEvent.BUTTON3_DOWN_MASK) != 0) {
            if (buf.length() > 0) buf.append('+');
            buf.append(_GUI._.jd_gui_swing_ShortCuts_key_button3());
        }

        return buf.toString();
    }

}