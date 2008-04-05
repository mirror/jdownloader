/*
 * $Id: VerticalLayout.java,v 1.3 2005/10/10 18:01:47 rbair Exp $
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * Organizes components in a vertical layout.
 * 
 * @author fred
 */
public class VerticalLayout implements LayoutManager {

  private int gap = 0;

  public VerticalLayout() {}

  public VerticalLayout(int gap) {
    this.gap = gap;
  }

  public int getGap() {
    return gap;
  }

  public void setGap(int gap) {
    this.gap = gap;
  }

  public void addLayoutComponent(String name, Component c) {}

  public void layoutContainer(Container parent) {
    Insets insets = parent.getInsets();
    Dimension size = parent.getSize();
    int width = size.width - insets.left - insets.right;
    int height = insets.top;

    for (int i = 0, c = parent.getComponentCount(); i < c; i++) {
      Component m = parent.getComponent(i);
      if (m.isVisible()) {
        m.setBounds(insets.left, height, width, m.getPreferredSize().height);
        height += m.getSize().height + gap;
      }
    }
  }

  public Dimension minimumLayoutSize(Container parent) {
    return preferredLayoutSize(parent);
  }

  public Dimension preferredLayoutSize(Container parent) {
    Insets insets = parent.getInsets();
    Dimension pref = new Dimension(0, 0);

    for (int i = 0, c = parent.getComponentCount(); i < c; i++) {
      Component m = parent.getComponent(i);
      if (m.isVisible()) {
        Dimension componentPreferredSize =
          parent.getComponent(i).getPreferredSize(); 
        pref.height += componentPreferredSize.height + gap;
        pref.width = Math.max(pref.width, componentPreferredSize.width);
      }
    }

    pref.width += insets.left + insets.right;
    pref.height += insets.top + insets.bottom;

    return pref;
  }

  public void removeLayoutComponent(Component c) {}

}
