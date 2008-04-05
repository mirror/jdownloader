/*
 * $Id: UIManagerUtils.java,v 1.4 2005/10/26 11:44:30 kleopatra Exp $
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
package org.jdesktop.swingx.util;

import java.awt.Font;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.View;
import javax.swing.text.html.HTMLDocument;

/**
 * Utility for working with the UIManager
 * @author Richard Bair
 */
// PENDING: JW - is this still used? Yes, by BasicTipOfTheDayUI
public final class UIManagerUtils {
    
    private static final Logger LOG = Logger.getLogger(UIManagerUtils.class
            .getName());
        /**
	 * Hidden constructor
	 */
	private UIManagerUtils() {
	}
	
	/**
	 * Initializes the object in the UIDefaults denoted by 'key' to defaultObj <strong>only if</strong>
	 * the key is not already in the UIDefaults.
	 * @param key
	 * @param defaultObj
	 */
	public static void initDefault(String key, Object defaultObj) {
		Object obj = UIManager.get(key);
		if (obj == null) {
			UIManager.put(key, defaultObj);
		}
	}

	/**
	 * Initializes the object in the UIDefaults denoted by 'key' to either the property in the metal look and feel
	 * associated with defaultMetalObjName, or the defaultObj if all else fails.
	 * @param key
	 * @param defaultMetalObjName
	 * @param defaultObj
	 */
	public static void initDefault(String key, String defaultMetalObjName, Object defaultObj) {
		Object obj = UIManager.get(key);
		if (obj == null) {
			try {
                Method m = ((MetalLookAndFeel)UIManager.getLookAndFeel()).getClass().getMethod(defaultMetalObjName, defaultObj.getClass());
                UIManager.put(key, m.invoke(UIManager.getLookAndFeel(), defaultMetalObjName));
			} catch (Exception e) {
				UIManager.put(key, defaultObj);
			}
		}
	}
    
  /**
   * Forces the given component to use the given font for its html rendering.
   * Text must have been set before calling this method.
   * 
   * @param component
   * @param font
   */
  public static void htmlize(JComponent component, Font font) {    
    String stylesheet = "body { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0; font-family: "
      + font.getName()
      + "; font-size: "
      + font.getSize()
      + "pt;  }"
      + "a, p, li { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0; font-family: "
      + font.getName()
      + "; font-size: "
      + font.getSize()
      + "pt;  }";

    try {
      HTMLDocument doc = null;
      if (component instanceof JEditorPane) {
        if (((JEditorPane)component).getDocument() instanceof HTMLDocument) {
          doc = (HTMLDocument)((JEditorPane)component).getDocument();
        }
      } else {
        View v = (View)component
          .getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
        if (v != null && v.getDocument() instanceof HTMLDocument) {
          doc = (HTMLDocument)v.getDocument();
        }
      }
      if (doc != null) {
        doc.getStyleSheet().loadRules(new java.io.StringReader(stylesheet),
          null);
      } // end of if (doc != null)
    } catch (Exception e) {
        // TODO change to something meaningful JW - when do we expect this to happen?
        LOG.log(Level.WARNING, "", e);

    }
  }
}
