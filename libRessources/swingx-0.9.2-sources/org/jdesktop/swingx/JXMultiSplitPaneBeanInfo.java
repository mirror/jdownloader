/*
 * $Id: JXMultiSplitPaneBeanInfo.java,v 1.2 2007/08/18 11:44:11 luano Exp $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
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
package org.jdesktop.swingx;

/**
 * Bean info for {@link org.jdesktop.swingx.JXMultiSplitPane} component.
 *
 * @author Hans Muller <Hans.Muller@Sun.COM>
 */
public class JXMultiSplitPaneBeanInfo extends BeanInfoSupport {
    public JXMultiSplitPaneBeanInfo() {
        super(JXMultiSplitPane.class);
    }

    // model, dividerSize, continuousLayout, dividerPainter
    protected void initialize() {
        setCategory("MultiSplitPane Layout, Appearance", "model", "dividerSize", 
                    "continuousLayout", "dividerPainter");
    }
}
