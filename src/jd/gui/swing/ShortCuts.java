/**
 * 
 * 
 * 
 * Some functions (marked) Taken from java.awt.AWTKeyStroke.java
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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.swing.KeyStroke;

import jd.utils.locale.JDL;

public class ShortCuts {
    /**
     * DO NOT MOVE THIS PREFIX. required by SrcParser in this java file
     */
    private static final String JDL_PREFIX = "jd.gui.swing.ShortCuts.";

    public static String getAcceleratorString(KeyStroke ks) {

        if (ks == null) return null;
        if (ks.getKeyCode() == KeyEvent.VK_UNDEFINED) {
            return getModifiersText(ks.getModifiers()) + "+" + ks.getKeyChar();
        } else {
            return getModifiersText(ks.getModifiers()) + "+" + getVKText(ks.getKeyCode());
        }

    }

    /**
     * Taken from java.awt.AWTKeyStroke.java
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
        StringBuffer buf = new StringBuffer();

        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            buf.append(JDL.L(JDL_PREFIX + "key.shift", "shift") + "");
        }
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            buf.append(JDL.L(JDL_PREFIX + "key.ctrl", "ctrl") + "");
        }
        if ((modifiers & InputEvent.META_DOWN_MASK) != 0) {
            buf.append(JDL.L(JDL_PREFIX + "key.meta", "meta") + "");
        }
        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
            buf.append(JDL.L(JDL_PREFIX + "key.alt", "alt") + "");
        }
        if ((modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            buf.append(JDL.L(JDL_PREFIX + "key.altGr", "alt Gr") + "");
        }
        if ((modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0) {
            buf.append(JDL.L(JDL_PREFIX + "key.button1", "button1") + "");
        }
        if ((modifiers & InputEvent.BUTTON2_DOWN_MASK) != 0) {
            buf.append(JDL.L(JDL_PREFIX + "key.button2", "button2") + "");
        }
        if ((modifiers & InputEvent.BUTTON3_DOWN_MASK) != 0) {
            buf.append(JDL.L(JDL_PREFIX + "key.button3", "button3") + "");
        }

        return buf.toString();
    }

    /**
     * Taken from java.awt.AWTKeyStroke.java
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
     *                       Translation uses dynamic keys. this is not recommended, but there is not better solution
     *                       
     */
    private static String getVKText(int keyCode) {
       
        Integer key = Integer.valueOf(keyCode);

        int expected_modifiers = (Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        Field[] fields = KeyEvent.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                if (fields[i].getModifiers() == expected_modifiers && fields[i].getType() == Integer.TYPE && fields[i].getName().startsWith("VK_") && fields[i].getInt(KeyEvent.class) == keyCode) {
                
                    return JDL.L(JDL_PREFIX+fields[i].getName(),fields[i].getName().substring(3));
                }
            } catch (IllegalAccessException e) {
                assert (false);
            }
        }
        return "UNKNOWN";
    }

    /**
     * Taken from java.awt.AWTKeyStroke.java
     * 
     * 
     * @(#)AWTKeyStroke.java 1.28 06/02/06
     * 
     *                       Copyright 2006 Sun Microsystems, Inc. All rights
     *                       reserved. SUN PROPRIETARY/CONFIDENTIAL. Use is
     *                       subject to license terms.
     */
    static class VKCollection {
        Map code2name;
        Map name2code;

        public VKCollection() {
            code2name = new HashMap();
            name2code = new HashMap();
        }

        public synchronized void put(String name, Integer code) {
            assert ((name != null) && (code != null));
            assert (findName(code) == null);
            assert (findCode(name) == null);
            code2name.put(code, name);
            name2code.put(name, code);
        }

        public synchronized Integer findCode(String name) {
            assert (name != null);
            return (Integer) name2code.get(name);
        }

        public synchronized String findName(Integer code) {
            assert (code != null);
            return (String) code2name.get(code);
        }
    }
}
