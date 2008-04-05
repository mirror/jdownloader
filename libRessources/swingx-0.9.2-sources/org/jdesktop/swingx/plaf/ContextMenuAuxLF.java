/*
 * $Id: ContextMenuAuxLF.java,v 1.5 2006/03/30 10:19:12 kleopatra Exp $
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

import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;

/**
 * Support for context dependent popup menus.
 * 
 * It's meant to be used as a auxiliary LF on top of main LF: 
 * 
 *  <pre> 
 *  <code>
 *  UIManager.addAuxiliaryLookAndFeel(new ContextMenuAuxLF());
 *  </code>
 *  </pre>
 * 
 * There are core-issues involved, which might or might not
 * impair its usefulness, for details please see a thread in
 * the SwingLabs forum: <p>
 * 
 * <a href="http://forums.java.net/jive/thread.jspa?threadID=7713">
 *  Experimental: default context menus for textcomponents/scrollbars 
 * </a>
 * 
 * 
 * @author Jeanette Winzenburg
 */
public class ContextMenuAuxLF extends LookAndFeel {

    private UIDefaults myDefaults;

    public String getName() {
        return "ContextMenuAuxLF";
    }

    public String getID() {
        return getName();
    }

    public String getDescription() {

        return "Auxiliary LF to Support Context Dependent Popups";
    }

    public boolean isNativeLookAndFeel() {
        return false;
    }

    public boolean isSupportedLookAndFeel() {
        return true;
    }

    public UIDefaults getDefaults() {
        if (myDefaults == null) {
            initDefaults();
        }
        return myDefaults;
    }

    private void initDefaults() {
        myDefaults = new MyUIDefaults();
        Object[] mydefaults = { "TextFieldUI",
                "org.jdesktop.swingx.plaf.ContextMenuAuxTextUI",
                "EditorPaneUI",
                "org.jdesktop.swingx.plaf.ContextMenuAuxTextUI",
                "PasswordFieldUI",
                "org.jdesktop.swingx.plaf.ContextMenuAuxTextUI", "TextAreaUI",
                "org.jdesktop.swingx.plaf.ContextMenuAuxTextUI", "TextPaneUI",
                "org.jdesktop.swingx.plaf.ContextMenuAuxTextUI", "ScrollBarUI",
                "org.jdesktop.swingx.plaf.ContextMenuAuxScrollBarUI", };
        myDefaults.putDefaults(mydefaults);
    }

    /**
     * UIDefaults without error msg.
     * 
     */
    private static class MyUIDefaults extends UIDefaults {

        /**
         *  Overridden to do nothing. 
         *  There will be many errors because this is incomplete as 
         *  of component types by design
         * 
         */
        @Override
        protected void getUIError(String msg) {

        }
    }
}
