/*
 * $Id: JXTableHeader.java,v 1.31 2008/02/28 14:58:23 kleopatra Exp $
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
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.io.Serializable;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.UIResource;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jdesktop.swingx.event.TableColumnModelExtListener;
import org.jdesktop.swingx.table.ColumnHeaderRenderer;
import org.jdesktop.swingx.table.TableColumnExt;

/**
 * TableHeader with extended functionality if associated Table is of
 * type JXTable.<p>
 * 
 * The enhancements:
 * <ul>
 * <li> supports pluggable handler to control user interaction for sorting. 
 * The default handler toggles sort order on mouseClicked on the header
 * of the column to sort. On shift-mouseClicked, it resets any column sorting. 
 * Both are done by invoking the corresponding methods of JXTable, 
 * <code> toggleSortOrder(int) </code> and <code> resetSortOrder() </code>
 * <li> uses ColumnHeaderRenderer which can show the sort icon
 * <li> triggers column pack (== auto-resize to exactly fit the contents)
 *  on double-click in resize region.
 *  <li> auto-scrolls if column is dragged outside visible rectangle. This feature
 *  is enabled if the autoscrolls property is true. The default is false 
 *  (because of Issue #788-swingx which still isn't fixed for jdk1.6).
 *  <li> listens to TableColumn propertyChanges to update itself accordingly.
 * </ul>
 * 
 * 
 * @author Jeanette Winzenburg
 * 
 * @see ColumnHeaderRenderer
 * @see JXTable#toggleSortOrder(int)
 * @see JXTable#resetSortOrder()
 */
public class JXTableHeader extends JTableHeader 
    implements TableColumnModelExtListener {

    private SortGestureRecognizer sortGestureRecognizer;

    /**
     *  Constructs a <code>JTableHeader</code> with a default 
     *  <code>TableColumnModel</code>.
     *
     * @see #createDefaultColumnModel
     */
    public JXTableHeader() {
        super();
    }

    /**
     * Constructs a <code>JTableHeader</code> which is initialized with
     * <code>cm</code> as the column model. If <code>cm</code> is
     * <code>null</code> this method will initialize the table header with a
     * default <code>TableColumnModel</code>.
     * 
     * @param columnModel the column model for the table
     * @see #createDefaultColumnModel
     */
    public JXTableHeader(TableColumnModel columnModel) {
        super(columnModel);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 
     * Overridden to initialize autoscrolls property to true.
     * <p>
     * 
     * N
     * 
     * @see #setDraggedDistance(int)
     */
//    @Override
//    protected void initializeLocalVars() {
//        super.initializeLocalVars();
//        // enabled again - after fix of #788-swingx
//        // no ... it's not fixed, something changed in 1.5
//        setAutoscrolls(false);
//    }

    /**
     * Sets the associated JTable. Enables enhanced header
     * features if table is of type JXTable.<p>
     * 
     * PENDING: who is responsible for synching the columnModel?
     */
    @Override
    public void setTable(JTable table) {
        super.setTable(table);
//        setColumnModel(table.getColumnModel());
        // the additional listening option makes sense only if the table
        // actually is a JXTable
        if (getXTable() != null) {
            installHeaderListener();
        } else {
            uninstallHeaderListener();
        }
    }

    /**
     * Implementing TableColumnModelExt: listening to column property changes.
     * Here: triggers a resizeAndRepaint on every propertyChange which
     * doesn't already fire a "normal" columnModelEvent.
     * 
     * @param event change notification from a contained TableColumn.
     * @see #isColumnEvent(PropertyChangeEvent)
     * 
     */
    public void columnPropertyChange(PropertyChangeEvent event) {
       if (isColumnEvent(event)) return;
       resizeAndRepaint(); 
    }
    
    
    /**
     * @param event the PropertyChangeEvent received as TableColumnModelExtListener.
     * @return a boolean to decide whether the same event triggers a
     *   base columnModelEvent.
     */
    protected boolean isColumnEvent(PropertyChangeEvent event) {
        return "width".equals(event.getPropertyName()) || 
            "preferredWidth".equals(event.getPropertyName())
            || "visible".equals(event.getPropertyName());
    }

    /**
     * overridden to respect the column tooltip, if available. 
     * 
     * @return the column tooltip of the column at the mouse position 
     *   if not null or super if not available.
     */
    @Override
    public String getToolTipText(MouseEvent event) {
        String columnToolTipText = getColumnToolTipText(event);
        return columnToolTipText != null ? columnToolTipText : super.getToolTipText(event);
    }

    /**
     * 
     * @param event the mouseEvent representing the mouse location.
     * @return the column tooltip of the column below the mouse location,
     *   or null if not available.
     */
    protected String getColumnToolTipText(MouseEvent event) {
        if (getXTable() == null) return null;
        int column = columnAtPoint(event.getPoint());
        if (column < 0) return null;
        TableColumnExt columnExt = getXTable().getColumnExt(column);
        return columnExt != null ? columnExt.getToolTipText() : null;
    }
    
    public JXTable getXTable() {
        if (!(getTable() instanceof JXTable))
            return null;
        return (JXTable) getTable();
    }

    /**
     * Returns the TableCellRenderer used for rendering the headerCell
     * of the column at columnIndex.
     * 
     * @param columnIndex the index of the column
     * @return the renderer.
     */
    public TableCellRenderer getCellRenderer(int columnIndex) {
        TableCellRenderer renderer = getColumnModel().getColumn(columnIndex).getHeaderRenderer();
        return renderer != null ? renderer : getDefaultRenderer();
    }
    
    /**
     * Overridden to adjust for a minimum height as returned by
     * #getMinimumHeight.
     * 
     * @inheritDoc
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension pref = super.getPreferredSize();
        pref = getPreferredSize(pref);
        pref.height = getMinimumHeight(pref.height);
        return pref;
    }
    
    /**
     * Hack around #334-swingx: super doesnt measure all headerRenderers
     * for prefSize. This hack does and adjusts the height of the 
     * given Dimension to be equal to the max fo all renderers.
     * 
     * @param pref the adjusted preferred size respecting all renderers
     *   size requirements.
     */
    protected Dimension getPreferredSize(Dimension pref) {
        int height = pref.height;
        for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
            TableCellRenderer renderer = getCellRenderer(i);
            Component comp = renderer.getTableCellRendererComponent(table, 
                    getColumnModel().getColumn(i).getHeaderValue(), false, false, -1, i);
            height = Math.max(height, comp.getPreferredSize().height);
        }
        pref.height = height;
        return pref;
        
    }

    /**
     * Allows to enforce a minimum heigth in the 
     * getXXSize methods.
     * 
     * Here: jumps in if the input height is 0, then measures the
     * cell renderer component with a dummy value.
     * 
     * @param height the prefHeigth as calcualated by super.
     * @return a minimum height for the preferredSize.
     */
    protected int getMinimumHeight(int height) {
        if ((height == 0)) {
//                && (getXTable() != null) 
//                && getXTable().isColumnControlVisible()){
            TableCellRenderer renderer = getDefaultRenderer();
            Component comp = renderer.getTableCellRendererComponent(getTable(), 
                        "dummy", false, false, -1, -1);
            height = comp.getPreferredSize().height;
        }
        return height;
    }
    
    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to update the default renderer. 
     * 
     * @see #preUpdateRendererUI()
     * @see #postUpdateRendererUI(TableCellRenderer)
     * @see ColumnHeaderRenderer
     */
    @Override
    public void updateUI() {
        TableCellRenderer oldRenderer = preUpdateRendererUI();
        super.updateUI();
        postUpdateRendererUI(oldRenderer);
    }

    /**
     * Prepares the default renderer and internal state for updateUI. 
     * Returns the default renderer set when entering this method.
     * Called from updateUI before calling super.updateUI to 
     * allow UIDelegate to cleanup, if necessary. This implementation
     * does so by restoring the header's default renderer to the 
     * <code>ColumnHeaderRenderer</code>'s delegate.
     *  
     * @return the current default renderer 
     * @see #updateUI()
     */
    protected TableCellRenderer preUpdateRendererUI() {
        TableCellRenderer oldRenderer = getDefaultRenderer();
        // reset the default to the original to give S
        if (oldRenderer instanceof ColumnHeaderRenderer) {
            setDefaultRenderer(((ColumnHeaderRenderer)oldRenderer).getDelegateRenderer());
        }
        return oldRenderer;
    }

    /**
     * Cleans up after the UIDelegate has updated the default renderer.
     * Called from <code>updateUI</code> after calling <code>super.updateUI</code>.
     * This implementation wraps a <code>UIResource</code> default renderer into a 
     * <code>ColumnHeaderRenderer</code>.
     * 
     * @param oldRenderer the default renderer before updateUI
     * 
     * @see #updateUI()
     * 
     * 
     */
    protected void postUpdateRendererUI(TableCellRenderer oldRenderer) {
        TableCellRenderer current = getDefaultRenderer();
        if (!(current instanceof ColumnHeaderRenderer) && (current instanceof UIResource)) {
            ColumnHeaderRenderer renderer;
            if (oldRenderer instanceof ColumnHeaderRenderer) {
                renderer = (ColumnHeaderRenderer) oldRenderer;
                renderer.updateUI(this);
            } else {
                renderer = new ColumnHeaderRenderer(this);
            }
            setDefaultRenderer(renderer);
        }
    }
    
    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to scroll the table to keep the dragged column visible.
     * This side-effect is enabled only if the header's autoscroll property is
     * <code>true</code> and the associated table is of type JXTable.<p>
     * 
     * 
     */
    @Override
    public void setDraggedDistance(int distance) {
        super.setDraggedDistance(distance);
        if (!getAutoscrolls() || (getXTable() == null)) return;
        TableColumn column = getDraggedColumn();
        // fix for #788-swingx: don't try to scroll if we have no dragged column
        // as doing will confuse the horizontalScrollEnabled on the JXTable.
        if (column != null) {
            getXTable().scrollColumnToVisible(getViewIndexForColumn(column));
        }
    }
    
    /**
     * Returns the the dragged column if and only if, a drag is in process and
     * the column is visible, otherwise returns <code>null</code>.
     * 
     * @return the dragged column, if a drag is in process and the column is
     *         visible, otherwise returns <code>null</code>
     * @see #getDraggedDistance
     */
    @Override
    public TableColumn getDraggedColumn() {
        return isVisible(draggedColumn) ? draggedColumn : null; 
    }

    /**
     * Checks and returns the column's visibility. 
     * 
     * @param column the <code>TableColumn</code> to check
     * @return a boolean indicating if the column is visible
     */
    private boolean isVisible(TableColumn column) {
        return getViewIndexForColumn(column) >= 0;
    }

    /**
     * Returns the (visible) view index for the given column
     * or -1 if not visible or not contained in this header's
     * columnModel.
     * 
     * 
     * @param aColumn
     * @return
     */
    private int getViewIndexForColumn(TableColumn aColumn) {
        if (aColumn == null)
            return -1;
        TableColumnModel cm = getColumnModel();
        for (int column = 0; column < cm.getColumnCount(); column++) {
            if (cm.getColumn(column) == aColumn) {
                return column;
            }
        }
        return -1;
    }

//    protected TableCellRenderer createDefaultRenderer() {
//        return ColumnHeaderRenderer.createColumnHeaderRenderer();
//    }

    /**
     * Lazily creates and returns the SortGestureRecognizer.
     * 
     * @return the SortGestureRecognizer used in Headerlistener.
     */
    public SortGestureRecognizer getSortGestureRecognizer() {
        if (sortGestureRecognizer == null) {
            sortGestureRecognizer = createSortGestureRecognizer();
        }
        return sortGestureRecognizer;
        
    }
    
    /**
     * Set the SortGestureRecognizer for use in the HeaderListener.
     * 
     * @param recognizer the recognizer to use in HeaderListener.
     */
    public void setSortGestureRecognizer(SortGestureRecognizer recognizer) {
        this.sortGestureRecognizer = recognizer;
    }
    
    /**
     * creates and returns the default SortGestureRecognizer.
     * @return the SortGestureRecognizer used in Headerlistener.
     * 
     */
    protected SortGestureRecognizer createSortGestureRecognizer() {
        return new SortGestureRecognizer();
    }

    protected void installHeaderListener() {
        if (headerListener == null) {
            headerListener = new HeaderListener();
            addMouseListener(headerListener);
            addMouseMotionListener(headerListener);

        }
    }

    protected void uninstallHeaderListener() {
        if (headerListener != null) {
            removeMouseListener(headerListener);
            removeMouseMotionListener(headerListener);
            headerListener = null;
        }
    }

    private MouseInputListener headerListener;

    private class HeaderListener implements MouseInputListener, Serializable {
        private TableColumn cachedResizingColumn;

        public void mouseClicked(MouseEvent e) {
            if (shouldIgnore(e)) {
                return;
            }
            if (isInResizeRegion(e)) {
                doResize(e);
            } else {
                doSort(e);
            }
        }

        private boolean shouldIgnore(MouseEvent e) {
            return !SwingUtilities.isLeftMouseButton(e)
              || !table.isEnabled();
        }

        private void doSort(MouseEvent e) {
            JXTable table = getXTable();
            if (!table.isSortable())
                return;
            if (getSortGestureRecognizer().isResetSortOrderGesture(e)) {
                table.resetSortOrder();
                repaint();
            } else if (getSortGestureRecognizer().isToggleSortOrderGesture(e)){
                int column = columnAtPoint(e.getPoint());
                if (column >= 0) {
                    table.toggleSortOrder(column);
                }
                uncacheResizingColumn();
                repaint();
            }

        }

        private void doResize(MouseEvent e) {
            if (e.getClickCount() != 2)
                return;
            int column = getViewIndexForColumn(cachedResizingColumn);
            if (column >= 0) {
                (getXTable()).packColumn(column, 5);
            }
            uncacheResizingColumn();

        }


        public void mouseReleased(MouseEvent e) {
            cacheResizingColumn(e);
        }

        public void mousePressed(MouseEvent e) {
            cacheResizingColumn(e);
        }

        private void cacheResizingColumn(MouseEvent e) {
            if (!getSortGestureRecognizer().isSortOrderGesture(e))
                return;
            TableColumn column = getResizingColumn();
            if (column != null) {
                cachedResizingColumn = column;
            }
        }

        private void uncacheResizingColumn() {
            cachedResizingColumn = null;
        }

        private boolean isInResizeRegion(MouseEvent e) {
            return cachedResizingColumn != null; // inResize;
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
            uncacheResizingColumn();
        }

        public void mouseDragged(MouseEvent e) {
            uncacheResizingColumn();
        }

        public void mouseMoved(MouseEvent e) {
        }
    }

    /**
     * Encapsulates decision about which MouseEvents should
     * trigger sort/unsort events.
     * 
     * Here: a single left click for toggling sort order, a
     * single SHIFT-left click for unsorting.
     * 
     */
    public static class SortGestureRecognizer {
        public boolean isResetSortOrderGesture(MouseEvent e) {
            return isSortOrderGesture(e) && isResetModifier(e);
        }

        protected boolean isResetModifier(MouseEvent e) {
            return ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK);
        }
        
        public boolean isToggleSortOrderGesture(MouseEvent e) {
            return isSortOrderGesture(e) && !isResetModifier(e);
        }
        
        public boolean isSortOrderGesture(MouseEvent e) {
            return e.getClickCount() == 1;
        }
    }


}
