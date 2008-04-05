/*
 * $Id: JXHeaderBeanInfo.java,v 1.3 2007/12/08 13:43:58 stolis Exp $
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
import org.jdesktop.swingx.editors.EnumPropertyEditor;

/**
 * BeanInfo class for JXHeader.
 *
 * @author rbair, Jan Stola
 */
public class JXHeaderBeanInfo extends BeanInfoSupport {
    
    public JXHeaderBeanInfo() {
        super(JXHeader.class);
    }

    protected void initialize() {
        BeanDescriptor bd = getBeanDescriptor();
        bd.setValue("isContainer", Boolean.FALSE);
        setPreferred(true, "title", "titleFont", "titleForeground");
        setPreferred(true, "description", "descriptionFont", "descriptionForeground");
        setPreferred(true, "icon", "iconPosition");
        setPreferred(false, "alpha", "background", "backgroundPainter", "border", "inheritAlpha", "opaque", "font");
        setPropertyEditor(IconPositionPropertyEditor.class, "iconPosition");
    }

    public static final class IconPositionPropertyEditor extends EnumPropertyEditor<JXHeader.IconPosition> {
        public IconPositionPropertyEditor() {
            super(JXHeader.IconPosition.class);
        }
    }

}
