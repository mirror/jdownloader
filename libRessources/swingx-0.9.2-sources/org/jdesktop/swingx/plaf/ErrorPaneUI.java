/*
 * $Id: ErrorPaneUI.java,v 1.3 2008/02/12 06:25:13 rah003 Exp $
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

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.plaf.PanelUI;

/**
 *
 * @author rbair
 */
public abstract class ErrorPaneUI extends PanelUI {
    public abstract JFrame getErrorFrame(Component owner);
    public abstract JDialog getErrorDialog(Component owner);
    public abstract JInternalFrame getErrorInternalFrame(Component owner);
    public abstract Dimension calculatePreferredSize();
}
