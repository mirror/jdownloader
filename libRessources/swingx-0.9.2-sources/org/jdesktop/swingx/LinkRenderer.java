/*
 * $Id: LinkRenderer.java,v 1.27 2008/02/18 15:35:05 kleopatra Exp $
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

import org.jdesktop.swingx.action.LinkAction;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A Renderer/Editor for "Links". <p>
 * 
 * The renderer is configured with a LinkAction<T>. 
 * It's mostly up to the developer to guarantee that the all
 * values which are passed into the getXXRendererComponent(...) are
 * compatible with T: she can provide a runtime class to check against.
 * If it isn't the renderer will configure the
 * action with a null target. <p>
 * 
 * It's recommended to not use the given Action anywhere else in code,
 * as it is updated on each getXXRendererComponent() call which might
 * lead to undesirable side-effects. <p>
 * 
 * Internally uses JXHyperlink for both CellRenderer and CellEditor
 * It's recommended to not reuse the same instance for both functions. <p>
 * 
 * PENDING: make renderer respect selected cell state.
 * 
 * PENDING: TreeCellRenderer has several issues
 *   - no icons
 *   - usual background highlighter issues
 * 
 * @author Jeanette Winzenburg
 * 
 * @deprecated as renderer (the editor part is not yet handled), 
 *    use the SwingX DefaultXXRenderer configured with a 
 *    {@link org.jdesktop.swingx.renderer.HyperlinkProvider} instead
 */
@Deprecated
public class LinkRenderer extends AbstractCellEditor implements
        TableCellRenderer, TableCellEditor, ListCellRenderer, 
        TreeCellRenderer, RolloverRenderer {

    private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    private JXHyperlink linkButton;

    private LinkAction<Object> linkAction;
    protected Class<?> targetClass;

    /**
     * Instantiate a LinkRenderer with null LinkAction and null
     * targetClass.
     *
     */
    public LinkRenderer() {
        this(null, null);
    }

    /**
     * Instantiate a LinkRenderer with the LinkAction to use with
     * target values. 
     * 
     * @param linkAction the action that acts on values.
     */
    public LinkRenderer(LinkAction linkAction) {
        this(linkAction, null);
    }
    
    /**
     * Instantiate a LinkRenderer with a LinkAction to use with
     * target values and the type of values the action can cope with. <p>
     * 
     * It's up to developers to take care of matching types.
     * 
     * @param linkAction the action that acts on values.
     * @param targetClass the type of values the action can handle.
     */
    public LinkRenderer(LinkAction linkAction, Class targetClass) {
        linkButton = createHyperlink();
        linkButton.addActionListener(createEditorActionListener());
        setLinkAction(linkAction, targetClass);
    }
    
    /**
     * Sets the class the action is supposed to handle. <p>
     * 
     * PENDING: make sense to set independently of LinkAction?
     * 
     * @param targetClass the type of values the action can handle.
     */
    public void setTargetClass(Class targetClass) {
        this.targetClass = targetClass;
    }

    /**
     * Sets the LinkAction for handling the values. <p>
     * 
     * The action is assumed to be able to cope with any type, that is
     * this method is equivalent to setLinkAction(linkAction, null).
     * 
     * @param linkAction
     */
    public void setLinkAction(LinkAction linkAction) {
        setLinkAction(linkAction, null);
    }
    
    /**
     * Sets the LinkAction for handling the values and the 
     * class the action can handle. <p>
     * 
     * PENDING: in the general case this is not independent of the
     * targetClass. Need api to set them combined?
     * 
     * @param linkAction
     */
    @SuppressWarnings("unchecked")
    public void setLinkAction(LinkAction linkAction, Class targetClass) {
        if (linkAction == null) {
            linkAction = createDefaultLinkAction();
        }
        setTargetClass(targetClass); 
        this.linkAction = linkAction;
        linkButton.setAction(linkAction);
        
    }
    /**
     * decides if the given target is acceptable for setTarget.
     * <p>
     *  
     *  target == null is acceptable for all types.
     *  targetClass == null is the same as Object.class
     *  
     * @param target the target to set.
     * @return true if setTarget can cope with the object, 
     *  false otherwise.
     * 
     */
    public  boolean isTargetable(Object target) {
        // we accept everything
        if (targetClass == null) return true;
        if (target == null) return true;
        return targetClass.isAssignableFrom(target.getClass());
    }


    /**
     * creates and returns the hyperlink component used for rendering
     * the value and activating the action on the target value.
     * 
     * @return the hyperlink renderer component.
     */
    protected JXHyperlink createHyperlink() {
        return new JXHyperlink() {

            @Override
            public void updateUI() {
                super.updateUI();
                setBorderPainted(true);
                setOpaque(true);
            }
            
        };
    }

    /** 
     * default action - does nothing... except showing the target.
     * 
     * @return a default LinkAction for showing the target.
     */
    protected LinkAction createDefaultLinkAction() {
        return new LinkAction<Object>(null) {

            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                
            }
            
        };
    }

//----------------------- Implement RolloverRenderer
    
    public boolean isEnabled() {
        return true;
    }

    public void doClick() {
        linkButton.doClick();
    }
    
//---------------------- Implement ListCellRenderer
    
    public Component getListCellRendererComponent(JList list, Object value, 
            int index, boolean isSelected, boolean cellHasFocus) {
        if ((value != null) && !isTargetable(value)) {
            value = null;
        }
        linkAction.setTarget(value);
        if (list != null) {
            Point p = (Point) list
                .getClientProperty(RolloverProducer.ROLLOVER_KEY);
            if (/*cellHasFocus ||*/ (p != null && (p.y >= 0) && (p.y == index))) {
                 linkButton.getModel().setRollover(true);
            } else {
                 linkButton.getModel().setRollover(false);
            }
            updateSelectionColors(list, isSelected);
            updateFocusBorder(cellHasFocus);
        }
        return linkButton;
    }
    

    private void updateSelectionColors(JList table, boolean isSelected) {
        if (isSelected) {
            // linkButton.setForeground(table.getSelectionForeground());
            linkButton.setBackground(table.getSelectionBackground());
        } else {
            // linkButton.setForeground(table.getForeground());
            linkButton.setBackground(table.getBackground());
        }

    }

//------------------------ TableCellRenderer
    
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if ((value != null) && !isTargetable(value)) {
            value = null;
        }
        linkAction.setTarget(value);
        if (table !=  null) {
            Point p = (Point) table
                    .getClientProperty(RolloverProducer.ROLLOVER_KEY);
            if (/*hasFocus || */(p != null && (p.x >= 0) && (p.x == column) && (p.y == row))) {
                 linkButton.getModel().setRollover(true);
            } else {
                 linkButton.getModel().setRollover(false);
            }
            updateSelectionColors(table, isSelected);
            updateFocusBorder(hasFocus);
        }
        return linkButton;
    }

    private void updateSelectionColors(JTable table, boolean isSelected) {
            if (isSelected) {
//                linkButton.setForeground(table.getSelectionForeground());
                linkButton.setBackground(table.getSelectionBackground());
            }
            else {
//                linkButton.setForeground(table.getForeground());
                linkButton.setBackground(table.getBackground());
            }
    
    }

    private void updateFocusBorder(boolean hasFocus) {
        if (hasFocus) {
            linkButton.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
        } else {
            linkButton.setBorder(noFocusBorder);
        }

        
    }

//-------------------------- TableCellEditor
    
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        linkAction.setTarget(value);
        linkButton.getModel().setRollover(true); 
        updateSelectionColors(table, isSelected);
        return linkButton;
    }

    public Object getCellEditorValue() {
        return linkAction.getTarget();
    }

    
 
    @Override
    protected void fireEditingStopped() {
        fireEditingCanceled();
    }

    private ActionListener createEditorActionListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelCellEditing();
            }
        };
    }

//----------------------- treeCellRenderer
    
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if ((value != null) && !isTargetable(value)) {
            value = null;
        }
        linkAction.setTarget(value);
        if (tree != null) {
            Point p = (Point) tree
                .getClientProperty(RolloverProducer.ROLLOVER_KEY);
            if (/*cellHasFocus ||*/ (p != null && (p.y >= 0) && (p.y == row))) {
                 linkButton.getModel().setRollover(true);
            } else {
                 linkButton.getModel().setRollover(false);
            }
            updateSelectionColors(tree, isSelected);
            updateFocusBorder(hasFocus);
        }
        return linkButton;
        
    }

    private void updateSelectionColors(JTree tree, boolean isSelected) {
        if (isSelected) {
//          linkButton.setForeground(table.getSelectionForeground());
          linkButton.setBackground(UIManager.getColor("Tree.selectionBackground"));
      }
      else {
//          linkButton.setForeground(table.getForeground());
          linkButton.setBackground(tree.getBackground());
      }
        
    }

}
