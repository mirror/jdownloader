/*
 * $Id: BasicHeaderUI.java,v 1.24 2008/02/29 08:35:12 rah003 Exp $
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

import org.jdesktop.swingx.JXHeader;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXHeader.IconPosition;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.plaf.HeaderUI;
import org.jdesktop.swingx.plaf.PainterUIResource;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import java.awt.*;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 *
 * @author rbair
 * @author rah003
 */
public class BasicHeaderUI extends HeaderUI {
	// Implementation detail. Neeeded to expose getMultiLineSupport() method to allow restoring view
	// lost after LAF switch 
    protected class DescriptionPane extends JXLabel {
            @Override
            public void paint(Graphics g) {
                // switch off jxlabel default antialiasing
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                super.paint(g);
            }
            
            public MultiLineSupport getMultiLineSupport() {
            	return super.getMultiLineSupport();
            }
	}

	protected JLabel titleLabel;
    protected DescriptionPane descriptionPane;
    protected JLabel imagePanel;
    private PropertyChangeListener propListener;
    private HierarchyBoundsListener boundsListener;
    private Color gradientLightColor;
    private Color gradientDarkColor;

    /** Creates a new instance of BasicHeaderUI */
    public BasicHeaderUI() {
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
        return new BasicHeaderUI();
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
        super.installUI(c);
        assert c instanceof JXHeader;
        JXHeader header = (JXHeader)c;

        titleLabel = new JLabel();
        descriptionPane = new DescriptionPane();
        descriptionPane.setLineWrap(true);
        descriptionPane.setOpaque(false);

        installDefaults(header);

        imagePanel = new JLabel();
        imagePanel.setIcon(header.getIcon() == null ? UIManager.getIcon("Header.defaultIcon") : header.getIcon());
        
        installComponents(header);
        installListeners(header);
    }

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
        assert c instanceof JXHeader;
        JXHeader header = (JXHeader)c;

        uninstallDefaults(header);
        uninstallListeners(header);
        uninstallComponents(header);

        titleLabel = null;
        descriptionPane = null;
        imagePanel = null;
    }

    protected void installDefaults(JXHeader h) {
        gradientLightColor = UIManager.getColor("JXHeader.startBackground");
        if (gradientLightColor == null) {
        	// fallback to white
        	gradientLightColor = Color.WHITE;
        }
        gradientDarkColor = UIManager.getColor("JXHeader.background");
        //for backwards compatibility (mostly for substance and synthetica,
        //I suspect) I'll fall back on the "control" color if JXHeader.background
        //isn't specified.
        if (gradientDarkColor == null) {
            gradientDarkColor = UIManager.getColor("control"); 
        }
        
        Painter p = h.getBackgroundPainter();
        if (p == null || p instanceof PainterUIResource) {
            h.setBackgroundPainter(createBackgroundPainter());
        }
 
        // title properties
        Font titleFont = h.getTitleFont();
        if (titleFont == null || titleFont instanceof FontUIResource) {
        	titleFont = UIManager.getFont("JXHeader.titleFont");
        	// fallback to label font
        	titleLabel.setFont(titleFont != null ? titleFont : UIManager.getFont("Label.font"));
        }
        
        Color titleForeground = h.getTitleForeground();
        if (titleForeground == null || titleForeground instanceof ColorUIResource) {
        	titleForeground = UIManager.getColor("JXHeader.titleForeground");
        	// fallback to label foreground
        	titleLabel.setForeground(titleForeground != null ? titleForeground : UIManager.getColor("Label.foreground"));
        }

        titleLabel.setText(h.getTitle());
        
        // description properties
        Font descFont = h.getDescriptionFont();
        if (descFont == null || descFont instanceof FontUIResource) {
        	descFont = UIManager.getFont("JXHeader.descriptionFont");
        	// fallback to label font
        	descriptionPane.setFont(descFont != null ? descFont : UIManager.getFont("Label.font"));
        }
            
        Color descForeground = h.getDescriptionForeground();
        if (descForeground == null || descForeground instanceof ColorUIResource) {
        	descForeground = UIManager.getColor("JXHeader.descriptionForeground");
        	// fallback to label foreground
        	descriptionPane.setForeground(descForeground != null ? descForeground : UIManager.getColor("Label.foreground"));
        }
        
        descriptionPane.setText(h.getDescription());
    }

    protected void uninstallDefaults(JXHeader h) {
    }

    protected void installListeners(final JXHeader header) {
        propListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                onPropertyChange(header, evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
            }
        };
        boundsListener = new HierarchyBoundsAdapter() {
            public void ancestorResized(HierarchyEvent e) {
                if (header == e.getComponent()) {
                    View v = (View) descriptionPane.getClientProperty(BasicHTML.propertyKey);
                    // view might get lost on LAF change ...
                    if (v == null) {
                    	descriptionPane.putClientProperty(BasicHTML.propertyKey, descriptionPane.getMultiLineSupport().createView(descriptionPane));
                    	v = (View) descriptionPane.getClientProperty(BasicHTML.propertyKey);
                    }
                    if (v != null) {
                        int h = Math.max(descriptionPane.getHeight(), header.getTopLevelAncestor().getHeight());
                        int w = Math.min(header.getTopLevelAncestor().getWidth(), header.getParent().getWidth());
                        // 35 = description pane insets, TODO: obtain dynamically
                        w -= 35 + header.getInsets().left + header.getInsets().right + descriptionPane.getInsets().left + descriptionPane.getInsets().right + imagePanel.getInsets().left + imagePanel.getInsets().right + imagePanel.getWidth() + descriptionPane.getBounds().x;
                    	v.setSize(w, h);
                    	descriptionPane.setSize(w, (int) Math.ceil(v.getPreferredSpan(View.Y_AXIS)));
                    }
                }
            }};
        header.addPropertyChangeListener(propListener);
        header.addHierarchyBoundsListener(boundsListener);
    }

    protected void uninstallListeners(JXHeader h) {
        h.removePropertyChangeListener(propListener);
        h.removeHierarchyBoundsListener(boundsListener);
    }

    protected void onPropertyChange(JXHeader h, String propertyName, Object oldValue, final Object newValue) {
        if ("title".equals(propertyName)) {
            titleLabel.setText(h.getTitle());
        } else if ("description".equals(propertyName)) {
            descriptionPane.setText(h.getDescription());
        } else if ("icon".equals(propertyName)) {
            imagePanel.setIcon(h.getIcon());
        } else if ("enabled".equals(propertyName)) {
            boolean enabled = h.isEnabled();
            titleLabel.setEnabled(enabled);
            descriptionPane.setEnabled(enabled);
            imagePanel.setEnabled(enabled);
        } else if ("titleFont".equals(propertyName)) {
            titleLabel.setFont((Font)newValue);
        } else if ("descriptionFont".equals(propertyName)) {
            descriptionPane.setFont((Font)newValue);
        } else if ("titleForeground".equals(propertyName)) {
            titleLabel.setForeground((Color)newValue);
        } else if ("descriptionForeground".equals(propertyName)) {
            descriptionPane.setForeground((Color)newValue);
        } else if ("iconPosition".equals(propertyName)) {
            resetLayout(h);
        }
    }

    protected void installComponents(JXHeader h) {
        h.setLayout(new GridBagLayout());
        resetLayout(h);
    }

    private void resetLayout(JXHeader h) {
    	h.remove(titleLabel);
    	h.remove(descriptionPane);
    	h.remove(imagePanel);
    	if (h.getIconPosition() == null || h.getIconPosition() == IconPosition.RIGHT) {
	        h.add(titleLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(12, 12, 0, 11), 0, 0));
	        h.add(descriptionPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 24, 12, 11), 0, 0));
	        h.add(imagePanel, new GridBagConstraints(1, 0, 1, 2, 0.0, 1.0, GridBagConstraints.FIRST_LINE_END, GridBagConstraints.NONE, new Insets(12, 0, 11, 11), 0, 0));
    	} else {
	        h.add(titleLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(12, 12, 0, 11), 0, 0));
	        h.add(descriptionPane, new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, new Insets(0, 24, 12, 11), 0, 0));
	        h.add(imagePanel, new GridBagConstraints(0, 0, 1, 2, 0.0, 1.0, GridBagConstraints.FIRST_LINE_END, GridBagConstraints.NONE, new Insets(12, 11, 0, 11), 0, 0));
    	}
	}

	protected void uninstallComponents(JXHeader h) {
        h.remove(titleLabel);
        h.remove(descriptionPane);
        h.remove(imagePanel);
    }

    protected Painter createBackgroundPainter() {
        MattePainter p = new MattePainter(new GradientPaint(0, 0, gradientLightColor, 1, 0, gradientDarkColor));
        p.setPaintStretched(true);
        return new PainterUIResource(p);
    }
}
