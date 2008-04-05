/*
 * Created on 08.01.2007
 *
 */
package org.jdesktop.swingx.renderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;

import org.jdesktop.swingx.JXPanel;

/**
 * Compound component for usage in tree renderer.
 */
public class WrappingIconPanel extends JXPanel {
    JComponent delegate;
    JLabel iconLabel;
    String labelPosition = BorderLayout.CENTER; //2;
    int iconLabelGap;
    private Border ltorBorder;
    private Border rtolBorder;
    
    
    public WrappingIconPanel() {
        setOpaque(false);
        iconLabel = new JRendererLabel();
        iconLabelGap = iconLabel.getIconTextGap();
        iconLabel.setOpaque(false);
        updateIconBorder();
        setBorder(null);
        setLayout(new BorderLayout());
        add(iconLabel, BorderLayout.LINE_START);
    }
    
    
    @Override
    public void setComponentOrientation(ComponentOrientation o) {
        super.setComponentOrientation(o);
        updateIconBorder();
    }


    private void updateIconBorder() {
        if (ltorBorder == null) {
            ltorBorder = BorderFactory.createEmptyBorder(0, 0, 0, iconLabelGap);
            rtolBorder = BorderFactory.createEmptyBorder(0, iconLabelGap, 0, 0);
        } 
        if (getComponentOrientation().isLeftToRight()) {
            iconLabel.setBorder(ltorBorder);
        } else {
            iconLabel.setBorder(rtolBorder);
        }
    }


    public void setIcon(Icon action) {
        iconLabel.setIcon(action);
        iconLabel.setText(null);
    }
    
    public void setComponent(JComponent comp) {
        if (delegate != null) {
            remove(delegate);
        }
        delegate = comp;
        add(delegate, labelPosition);
        validate();
    }


    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (iconLabel != null) {
            iconLabel.setBackground(bg);
        }
        if (delegate != null) {
            delegate.setBackground(bg);
        }
    }

    @Override
    public void setForeground(Color bg) {
        super.setForeground(bg);
        if (iconLabel != null) {
            iconLabel.setForeground(bg);
        }
        if (delegate != null) {
            delegate.setForeground(bg);
        }
    }


    /**
     * Returns the icon used in this panel, may be null.
     * 
     * @return the icon used in this panel, may be null.
     */
    public Icon getIcon() {
        return iconLabel.getIcon();
    }



    /**
     * 
     * Returns the bounds of the delegate component or null if the delegate is null.
     * 
     * PENDING JW: where do we use it? Maybe it was for testing only?
     * 
     * @return the bounds of the delegate, or null if the delegate is null.
     */
    public Rectangle getDelegateBounds() {
        if (delegate == null) return null;
        return delegate.getBounds();
    }
    
    
}