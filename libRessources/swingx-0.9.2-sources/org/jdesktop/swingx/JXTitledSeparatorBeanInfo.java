/*
 * $Id: JXTitledSeparatorBeanInfo.java,v 1.7 2007/12/14 16:25:58 stolis Exp $
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
package org.jdesktop.swingx;

import java.beans.BeanDescriptor;
import javax.swing.SwingConstants;

/**
 * BeanInfo class for JXTitledSeparator.
 *
 * @author rbair, Jan Stola
 */
public class JXTitledSeparatorBeanInfo extends BeanInfoSupport {
    
    public JXTitledSeparatorBeanInfo() {
        super(JXTitledSeparator.class);
    }

    protected void initialize() {
        String iconName = "resources/" + JXTitledSeparator.class.getSimpleName();
        setSmallMonoIconName(iconName + "16.png");
        setMonoIconName(iconName + "32.png");
        BeanDescriptor bd = getBeanDescriptor();
        bd.setValue("isContainer", Boolean.FALSE);
        setPreferred(true, "icon", "title", "horizontalAlignment", "horizontalTextPosition");
        setPreferred(false, "alpha", "inheritAlpha");
        setPreferred(false, "background", "backgroundPainter");
        setPreferred(false, "border", "opaque");
        
        setEnumerationValues(new EnumerationValue[] {
            new EnumerationValue("Center", SwingConstants.CENTER, "SwingConstants.CENTER"),
            new EnumerationValue("Leading", SwingConstants.LEADING, "SwingConstants.LEADING"),
            new EnumerationValue("Left", SwingConstants.LEFT, "SwingConstants.LEFT"),
            new EnumerationValue("Right", SwingConstants.RIGHT, "SwingConstants.RIGHT"),
            new EnumerationValue("Trailing", SwingConstants.TRAILING, "SwingConstants.TRAILING")
        }, "horizontalAlignment", "horizontalTextPosition");
    }
}
