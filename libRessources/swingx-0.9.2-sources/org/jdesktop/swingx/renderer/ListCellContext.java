package org.jdesktop.swingx.renderer;

import java.awt.Color;

import javax.swing.JList;

/**
 * List specific cellContext.
 */
public class ListCellContext extends CellContext<JList> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Color getSelectionBackground() {
        return getComponent() != null ? getComponent().getSelectionBackground() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Color getSelectionForeground() {
        return getComponent() != null ? getComponent().getSelectionForeground() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getUIPrefix() {
        return "List.";
    }
    
    
    
}