/*
 * $Id: BasicStatusBarUI.java,v 1.18 2007/09/17 16:39:40 rbair Exp $
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

package org.jdesktop.swingx.plaf.basic;

import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXStatusBar.Constraint;
import org.jdesktop.swingx.plaf.StatusBarUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author rbair
 */
public class BasicStatusBarUI extends StatusBarUI {
    public static final String AUTO_ADD_SEPARATOR = new StringBuffer("auto-add-separator").toString();
    /**
     * Used to help reduce the amount of trash being generated
     */
    private static Insets TEMP_INSETS;
    /**
     * The one and only JXStatusBar for this UI delegate
     */
    private JXStatusBar statusBar;
    
    /** Creates a new instance of BasicStatusBarUI */
    public BasicStatusBarUI() {
    }
    
    /**
     * Returns an instance of the UI delegate for the specified component.
     * Each subclass must provide its own static <code>createUI</code>
     * method that returns an instance of that UI delegate subclass.
     * If the UI delegate subclass is stateless, it may return an instance
     * that is shared by multiple components.  If the UI delegate is
     * stateful, then it should return a new instance per component.
     * The default implementation of this method throws an error, as it
     * should never be invoked.
     */
    public static ComponentUI createUI(JComponent c) {
        return new BasicStatusBarUI();
    }
    
    /**
     * Configures the specified component appropriate for the look and feel.
     * This method is invoked when the <code>ComponentUI</code> instance is being installed
     * as the UI delegate on the specified component.  This method should
     * completely configure the component for the look and feel,
     * including the following:
     * <ol>
     * <li>Install any default property values for color, fonts, borders,
     *     icons, opacity, etc. on the component.  Whenever possible,
     *     property values initialized by the client program should <i>not</i>
     *     be overridden.
     * <li>Install a <code>LayoutManager</code> on the component if necessary.
     * <li>Create/add any required sub-components to the component.
     * <li>Create/install event listeners on the component.
     * <li>Create/install a <code>PropertyChangeListener</code> on the component in order
     *     to detect and respond to component property changes appropriately.
     * <li>Install keyboard UI (mnemonics, traversal, etc.) on the component.
     * <li>Initialize any appropriate instance data.
     * </ol>
     * @param c the component where this UI delegate is being installed
     *
     * @see #uninstallUI
     * @see javax.swing.JComponent#setUI
     * @see javax.swing.JComponent#updateUI
     */
    @Override
    public void installUI(JComponent c) {
        assert c instanceof JXStatusBar;
        statusBar = (JXStatusBar)c;
        
        installDefaults(statusBar);
        installListeners(statusBar);
        
        //only set the layout manager if the layout manager of the component is null.
        //do not replace custom layout managers. it is not necessary to replace this layout
        //manager.
        LayoutManager m = statusBar.getLayout();
        if (m == null) {
            statusBar.setLayout(createLayout());
        }
    }
    
    protected void installDefaults(JXStatusBar sb) {
        //only set the border if it is an instanceof UIResource
        //In other words, only replace the border if it has not been
        //set by the developer. UIResource is the flag we use to indicate whether
        //the value was set by the UIDelegate, or by the developer.
        Border b = statusBar.getBorder();
        if (b == null || b instanceof UIResource) {
            statusBar.setBorder(createBorder());
        }
    }
    
    protected void installListeners(JXStatusBar sb) { }
    
    /**
     * Reverses configuration which was done on the specified component during
     * <code>installUI</code>.  This method is invoked when this
     * <code>UIComponent</code> instance is being removed as the UI delegate
     * for the specified component.  This method should undo the
     * configuration performed in <code>installUI</code>, being careful to
     * leave the <code>JComponent</code> instance in a clean state (no
     * extraneous listeners, look-and-feel-specific property objects, etc.).
     * This should include the following:
     * <ol>
     * <li>Remove any UI-set borders from the component.
     * <li>Remove any UI-set layout managers on the component.
     * <li>Remove any UI-added sub-components from the component.
     * <li>Remove any UI-added event/property listeners from the component.
     * <li>Remove any UI-installed keyboard UI from the component.
     * <li>Nullify any allocated instance data objects to allow for GC.
     * </ol>
     * @param c the component from which this UI delegate is being removed;
     *          this argument is often ignored,
     *          but might be used if the UI object is stateless
     *          and shared by multiple components
     *
     * @see #installUI
     * @see javax.swing.JComponent#updateUI
     */
    @Override
    public void uninstallUI(JComponent c) {
        assert c instanceof JXStatusBar;

        uninstallDefaults(statusBar);
        uninstallListeners(statusBar);
        //TODO remove the border and layout if a UI resource?
    }
    
    protected void uninstallDefaults(JXStatusBar sb) { }
    protected void uninstallListeners(JXStatusBar sb) { }
    
    @Override
    public void paint(Graphics g, JComponent c) {
        //paint the background if opaque
        if (statusBar.isOpaque()) {
            Graphics2D g2 = (Graphics2D)g;
            paintBackground(g2, statusBar);
        }
        
        if (includeSeparators()) {
            //now paint the separators
            TEMP_INSETS = getSeparatorInsets(TEMP_INSETS);
            for (int i=0; i<statusBar.getComponentCount()-1; i++) {
                Component comp = statusBar.getComponent(i);
                int x = comp.getX() + comp.getWidth() + TEMP_INSETS.left;
                int y = TEMP_INSETS.top;
                int w = getSeparatorWidth() - TEMP_INSETS.left - TEMP_INSETS.right;
                int h = c.getHeight() - TEMP_INSETS.top - TEMP_INSETS.bottom;

                paintSeparator((Graphics2D)g, statusBar, x, y, w, h);
            }
        }
    }
    
    //----------------------------------------------------- Extension Points
    protected void paintBackground(Graphics2D g, JXStatusBar bar) {
        if (bar.isOpaque()) {
            g.setColor(bar.getBackground());
            g.fillRect(0, 0, bar.getWidth(), bar.getHeight());
        }
    }
    
    protected void paintSeparator(Graphics2D g, JXStatusBar bar, int x, int y, int w, int h) {
        Color fg = UIManager.getColor("Separator.foreground");
        Color bg = UIManager.getColor("Separator.background");
        
        x += w / 2;
        g.setColor(fg);
        g.drawLine(x, y, x, h);
        
        g.setColor(bg);
        g.drawLine(x+1, y, x+1, h);
    }
    
    protected Insets getSeparatorInsets(Insets insets) {
        if (insets == null) {
            insets = new Insets(0, 0, 0, 0);
        }
        
        insets.top = 4;
        insets.left = 4;
        insets.bottom = 2;
        insets.right = 4;
        
        return insets;
    }
    
    protected int getSeparatorWidth() {
        return 10;
    }
    
    protected boolean includeSeparators() {
        Boolean b = (Boolean)statusBar.getClientProperty(AUTO_ADD_SEPARATOR);
        return b == null || b;
    }
    
    protected BorderUIResource createBorder() {
        return new BorderUIResource(BorderFactory.createEmptyBorder(4, 5, 4, 22));
    }
    
    protected LayoutManager createLayout() {
        //This is in the UI delegate because the layout
        //manager takes into account spacing for the separators between components
        return new LayoutManager2() {
            private Map<Component,Constraint> constraints = new HashMap<Component,Constraint>();
            
            public void addLayoutComponent(String name, Component comp) {addLayoutComponent(comp, null);}
            public void removeLayoutComponent(Component comp) {constraints.remove(comp);}
            public Dimension minimumLayoutSize(Container parent) {return preferredLayoutSize(parent);}
            public Dimension maximumLayoutSize(Container target) {return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);}
            public float getLayoutAlignmentX(Container target) {return .5f;}
            public float getLayoutAlignmentY(Container target) {return .5f;}
            public void invalidateLayout(Container target) {}
            
            public void addLayoutComponent(Component comp, Object constraint) {
                //we accept an Insets, a ResizeBehavior, or a Constraint.
                if (constraint instanceof Insets) {
                    constraint = new Constraint((Insets)constraint);
                } else if (constraint instanceof Constraint.ResizeBehavior) {
                    constraint = new Constraint((Constraint.ResizeBehavior)constraint);
                }
                
                constraints.put(comp, (Constraint)constraint);
            }
            
            public Dimension preferredLayoutSize(Container parent) {
                Dimension prefSize = new Dimension();
                int count = 0;
                for (Component comp : constraints.keySet()) {
                    Constraint c = constraints.get(comp);
                    Dimension d = comp.getPreferredSize();
                    int prefWidth = 0;
                    if (c != null) {
                        Insets i = c.getInsets();
                        d.width += i.left + i.right;
                        d.height += i.top + i.bottom;
                        prefWidth = c.getFixedWidth();
                    }
                    prefSize.height = Math.max(prefSize.height, d.height);
                    prefSize.width += Math.max(d.width, prefWidth);
                    
                    //If this is not the last component, add extra space between each
                    //component (for the separator).
                    count++;
                    if (includeSeparators() && constraints.size() < count) {
                        prefSize.width += getSeparatorWidth();
                    }
                }
                
                Insets insets = parent.getInsets();
                prefSize.height += insets.top + insets.bottom;
                prefSize.width += insets.left + insets.right;
                return prefSize;
            }
            
            public void layoutContainer(Container parent) {
                /*
                 * Layout algorithm:
                 *      If the parent width is less than the sum of the preferred
                 *      widths of the components (including separators), where
                 *      preferred width means either the component preferred width + 
                 *      constraint insets, or fixed width + constraint insets, then
                 *      simply layout the container from left to right and let the
                 *      right hand components flow off the parent.
                 *
                 *      Otherwise, lay out each component according to its preferred
                 *      width except for components with a FILL constraint. For these,
                 *      resize them evenly for each FILL constraint.
                 */
                
                //the insets of the parent component.
                Insets parentInsets = parent.getInsets();
                //the available width for putting components.
                int availableWidth = parent.getWidth() - parentInsets.left - parentInsets.right;
                if (includeSeparators()) {
                    //remove from availableWidth the amount of space the separators will take
                    availableWidth -= (parent.getComponentCount() - 1) * getSeparatorWidth();
                }
                
                //the preferred widths of all of the components -- where preferred
                //width mean the preferred width after calculating fixed widths and
                //constraint insets
                int[] preferredWidths = new int[parent.getComponentCount()];
                int sumPreferredWidths = 0;
                for (int i=0; i<preferredWidths.length; i++) {
                    preferredWidths[i] = getPreferredWidth(parent.getComponent(i));
                    sumPreferredWidths += preferredWidths[i];
                }
                
                //if the availableWidth is greater than the sum of preferred
                //sizes, then adjust the preferred width of each component that
                //has a FILL constraint, to evenly use up the extra space.
                if (availableWidth > sumPreferredWidths) {
                    //the number of components with a fill constraint
                    int numFilledComponents = 0;
                    for (Component comp : parent.getComponents()) {
                        Constraint c = constraints.get(comp);
                        if (c != null && c.getResizeBehavior() == Constraint.ResizeBehavior.FILL) {
                            numFilledComponents++;
                        }
                    }
                    
                    if (numFilledComponents > 0) {
                        //calculate the share of free space each FILL component will take
                        availableWidth -= sumPreferredWidths;
                        double weight = 1.0 / (double)numFilledComponents;
                        int share = (int)(availableWidth * weight);
                        int remaining = numFilledComponents;
                        for (int i=0; i<parent.getComponentCount(); i++) {
                            Component comp = parent.getComponent(i);
                            Constraint c = constraints.get(comp);
                            if (c != null && c.getResizeBehavior() == Constraint.ResizeBehavior.FILL) {
                                if (remaining > 1) {
                                    preferredWidths[i] += share;
                                    availableWidth -= share;
                                } else {
                                    preferredWidths[i] += availableWidth;
                                }
                                remaining--;
                            }
                        }
                    }
                }
                
                //now lay out the components
                int nextX = parentInsets.left;
                int height = parent.getHeight() - parentInsets.top - parentInsets.bottom;
                for (int i=0; i<parent.getComponentCount(); i++) {
                    Component comp = parent.getComponent(i);
                    Constraint c = constraints.get(comp);
                    Insets insets = c == null ? new Insets(0,0,0,0) : c.getInsets();
                    int width = preferredWidths[i] - (insets.left + insets.right);
                    int x = nextX + insets.left;
                    int y = parentInsets.top + insets.top;
                    comp.setSize(width, height);
                    comp.setLocation(x, y);
                    nextX = x + width + insets.right;
                    //If this is not the last component, add extra space
                    //for the separator
                    if (includeSeparators() && i < parent.getComponentCount() - 1) {
                        nextX += getSeparatorWidth();
                    }
                }
            }
            
            /**
             * @return the "preferred" width, where that means either 
             *         comp.getPreferredSize().width + constraintInsets, or
             *         constraint.fixedWidth + constraintInsets.
             */
            private int getPreferredWidth(Component comp) {
                Constraint c = constraints.get(comp);
                if (c == null) {
                    return comp.getPreferredSize().width;
                } else {
                    Insets insets = c.getInsets();
                    assert insets != null;
                    if (c.getFixedWidth() <= 0) {
                        return comp.getPreferredSize().width + insets.left + insets.right;
                    } else {
                        return c.getFixedWidth() + insets.left + insets.right;
                    }
                }
            }
            
        };
    }
}
