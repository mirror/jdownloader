/*
 * $Id: JXList.java,v 1.66 2008/02/26 11:00:11 kleopatra Exp $
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
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.CompoundHighlighter;
import org.jdesktop.swingx.decorator.DefaultSelectionMapper;
import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.PipelineEvent;
import org.jdesktop.swingx.decorator.PipelineListener;
import org.jdesktop.swingx.decorator.SelectionMapper;
import org.jdesktop.swingx.decorator.SortController;
import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.jdesktop.swingx.renderer.DefaultListRenderer;
import org.jdesktop.swingx.renderer.StringValue;

/**
 * JXList.
 * 
 * Enabled Rollover/LinkModel handling. Enabled Highlighter support.
 * 
 * Added experimental support for filtering/sorting. This feature is disabled by
 * default because it has side-effects which might break "normal" expectations
 * when using a JList: if enabled all row coordinates (including those returned
 * by the selection) are in view coordinates. Furthermore, the model returned
 * from getModel() is a wrapper around the actual data.
 * 
 * 
 * 
 * @author Ramesh Gupta
 * @author Jeanette Winzenburg
 */
public class JXList extends JList {
    @SuppressWarnings("all")
    private static final Logger LOG = Logger.getLogger(JXList.class.getName());
    public static final String EXECUTE_BUTTON_ACTIONCOMMAND = "executeButtonAction";

    /** The pipeline holding the filters. */
    protected FilterPipeline filters;

    /**
     * The pipeline holding the highlighters.
     */
    protected CompoundHighlighter compoundHighlighter;

    /** listening to changeEvents from compoundHighlighter. */
    private ChangeListener highlighterChangeListener;

    /** The ComponentAdapter for model data access. */
    protected ComponentAdapter dataAdapter;

    /**
     * Mouse/Motion/Listener keeping track of mouse moved in cell coordinates.
     */
    private RolloverProducer rolloverProducer;

    /**
     * RolloverController: listens to cell over events and repaints
     * entered/exited rows.
     */
    private ListRolloverController linkController;

    /** A wrapper around the default renderer enabling decoration. */
    private DelegatingRenderer delegatingRenderer;

    private WrappingListModel wrappingModel;

    private PipelineListener pipelineListener;

    private boolean filterEnabled;

    private SelectionMapper selectionMapper;

    private Searchable searchable;

    private Comparator comparator;

    /**
    * Constructs a <code>JXList</code> with an empty model and filters disabled.
    *
    */                                           
    public JXList() {
        this(false);
    }

    /**
     * Constructs a <code>JXList</code> that displays the elements in the
     * specified, non-<code>null</code> model and filters disabled.
     *
     * @param dataModel   the data model for this list
     * @exception IllegalArgumentException   if <code>dataModel</code>
     *                                           is <code>null</code>
     */                                           
    public JXList(ListModel dataModel) {
        this(dataModel, false);
    }

    /**
     * Constructs a <code>JXList</code> that displays the elements in
     * the specified array and filters disabled.
     *
     * @param  listData  the array of Objects to be loaded into the data model
     * @throws IllegalArgumentException   if <code>listData</code>
     *                                          is <code>null</code>
     */
    public JXList(Object[] listData) {
        this(listData, false);
    }

    /**
     * Constructs a <code>JXList</code> that displays the elements in
     * the specified <code>Vector</code> and filtes disabled.
     *
     * @param  listData  the <code>Vector</code> to be loaded into the
     *          data model
     * @throws IllegalArgumentException   if <code>listData</code>
     *                                          is <code>null</code>
     */
    public JXList(Vector<?> listData) {
        this(listData, false);
    }


    /**
     * Constructs a <code>JXList</code> with an empty model and
     * filterEnabled property.
     * 
     * @param filterEnabled <code>boolean</code> to determine if 
     *  filtering/sorting is enabled
     */
    public JXList(boolean filterEnabled) {
        init(filterEnabled);
    }

    /**
     * Constructs a <code>JXList</code> with the specified model and
     * filterEnabled property.
     * 
     * @param dataModel   the data model for this list
     * @param filterEnabled <code>boolean</code> to determine if 
     *          filtering/sorting is enabled
     * @throws IllegalArgumentException   if <code>dataModel</code>
     *                                          is <code>null</code>
     */
    public JXList(ListModel dataModel, boolean filterEnabled) {
        super(dataModel);
        init(filterEnabled);
    }

    /**
     * Constructs a <code>JXList</code> that displays the elements in
     * the specified array and filterEnabled property.
     *
     * @param  listData  the array of Objects to be loaded into the data model
     * @param filterEnabled <code>boolean</code> to determine if filtering/sorting
     *   is enabled
     * @throws IllegalArgumentException   if <code>listData</code>
     *                                          is <code>null</code>
     */
    public JXList(Object[] listData, boolean filterEnabled) {
        super(listData);
        if (listData == null) 
           throw new IllegalArgumentException("listData must not be null");
        init(filterEnabled);
    }

    /**
     * Constructs a <code>JXList</code> that displays the elements in
     * the specified <code>Vector</code> and filtersEnabled property.
     *
     * @param  listData  the <code>Vector</code> to be loaded into the
     *          data model
     * @param filterEnabled <code>boolean</code> to determine if filtering/sorting
     *   is enabled
     * @throws IllegalArgumentException if <code>listData</code> is <code>null</code>
     */
    public JXList(Vector<?> listData, boolean filterEnabled) {
        super(listData);
        if (listData == null) 
           throw new IllegalArgumentException("listData must not be null");
        init(filterEnabled);
    }


    private void init(boolean filterEnabled) {
        setFilterEnabled(filterEnabled);
        
        Action findAction = createFindAction();
        getActionMap().put("find", findAction);
        
        KeyStroke findStroke = SearchFactory.getInstance().getSearchAccelerator();
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(findStroke, "find");
        
    }

    private Action createFindAction() {
        return new UIAction("find") {
            public void actionPerformed(ActionEvent e) {
                doFind();
            }
        };
    }

    protected void doFind() {
        SearchFactory.getInstance().showFindInput(this, getSearchable());
    }

    /**
     * 
     * @return a not-null Searchable for this editor.
     */
    public Searchable getSearchable() {
        if (searchable == null) {
            searchable = new ListSearchable();
        }
        return searchable;
    }

    /**
     * sets the Searchable for this editor. If null, a default 
     * searchable will be used.
     * 
     * @param searchable
     */
    public void setSearchable(Searchable searchable) {
        this.searchable = searchable;
    }
    

    public class ListSearchable extends AbstractSearchable {

        @Override
        protected void findMatchAndUpdateState(Pattern pattern, int startRow, boolean backwards) {
            SearchResult searchResult = null;
            if (backwards) {
                for (int index = startRow; index >= 0 && searchResult == null; index--) {
                    searchResult = findMatchAt(pattern, index);
                }
            } else {
                for (int index = startRow; index < getSize() && searchResult == null; index++) {
                    searchResult = findMatchAt(pattern, index);
                }
            }
            updateState(searchResult);
        }

        @Override
        protected SearchResult findExtendedMatch(Pattern pattern, int row) {
            
            return findMatchAt(pattern, row);
        }
        /**
         * Matches the cell content at row/col against the given Pattern.
         * Returns an appropriate SearchResult if matching or null if no
         * matching
         * 
         * @param pattern 
         * @param row a valid row index in view coordinates
         * @return <code>SearchResult</code> if matched otherwise null
         */
        protected SearchResult findMatchAt(Pattern pattern, int row) {
            Object value = getElementAt(row);
            if (value != null) {
                Matcher matcher = pattern.matcher(value.toString());
                if (matcher.find()) {
                    return createSearchResult(matcher, row, -1);
                }
            }
            return null;
        }
        
        @Override
        protected int getSize() {
            return getElementCount();
        }

        /**
         * @param result
         * @return {@code true} if the {@code result} contains a match;
         *         {@code false} otherwise
         */
        protected boolean hasMatch(SearchResult result) {
            return result.getFoundRow() >= 0;
        }
        
        @Override
        protected void moveMatchMarker() {
            // PENDING JW: #718-swingx - don't move selection on not found
            // complying here is accidental, defaultListSelectionModel doesn't
            // clear on -1 but silently does nothing
            // isn't doc'ed anywhere - so we back out
            if (!hasMatch(lastSearchResult)) {
                return;
            }
            setSelectedIndex(lastSearchResult.foundRow);
            ensureIndexIsVisible(lastSearchResult.foundRow);

        }

    }
    /**
     * Property to enable/disable rollover support. This can be enabled to show
     * "live" rollover behaviour, f.i. the cursor over LinkModel cells. Default
     * is disabled.
     * 
     * @param rolloverEnabled
     */
    public void setRolloverEnabled(boolean rolloverEnabled) {
        boolean old = isRolloverEnabled();
        if (rolloverEnabled == old)
            return;
        if (rolloverEnabled) {
            rolloverProducer = createRolloverProducer();
            addMouseListener(rolloverProducer);
            addMouseMotionListener(rolloverProducer);
            getLinkController().install(this);
        } else {
            removeMouseListener(rolloverProducer);
            removeMouseMotionListener(rolloverProducer);
            rolloverProducer = null;
            getLinkController().release();
        }
        firePropertyChange("rolloverEnabled", old, isRolloverEnabled());
    }

    
    protected ListRolloverController getLinkController() {
        if (linkController == null) {
            linkController = createLinkController();
        }
        return linkController;
    }

    protected ListRolloverController createLinkController() {
        return new ListRolloverController();
    }


    /**
     * creates and returns the RolloverProducer to use with this tree.
     * 
     * @return <code>RolloverProducer</code> to use with this tree
     */
    protected RolloverProducer createRolloverProducer() {
        return new RolloverProducer() {
            @Override
            protected void updateRolloverPoint(JComponent component,
                    Point mousePoint) {
                JList list = (JList) component;
                int row = list.locationToIndex(mousePoint);
                if (row >= 0) {
                    Rectangle cellBounds = list.getCellBounds(row, row);
                    if (!cellBounds.contains(mousePoint)) {
                        row = -1;
                    }
                }
                int col = row < 0 ? -1 : 0;
                rollover.x = col;
                rollover.y = row;
            }

        };
    }
    /**
     * returns the rolloverEnabled property.
     *
     * TODO: why doesn't this just return rolloverEnabled???
     *
     * @return true if rollover is enabled
     */
    public boolean isRolloverEnabled() {
        return rolloverProducer != null;
    }

    /**
     * listens to rollover properties. Repaints effected component regions.
     * Updates link cursor.
     * 
     * @author Jeanette Winzenburg
     */
    public static class ListRolloverController<T extends JList> extends
            RolloverController<T> {

        private Cursor oldCursor;

        // --------------------------------- JList rollover

        @Override
        protected void rollover(Point oldLocation, Point newLocation) {
            // PENDING JW - track down the -1 in location.y
            if (oldLocation != null) {
                Rectangle r = component.getCellBounds(oldLocation.y, oldLocation.y);
                // LOG.info("old index/cellbounds: " + index + "/" + r);
                if (r != null) {
                    component.repaint(r);
                }
            }
            if (newLocation != null) {
                Rectangle r = component.getCellBounds(newLocation.y, newLocation.y);
                // LOG.info("new index/cellbounds: " + index + "/" + r);
                if (r != null) {
                    component.repaint(r);
                }
            }
            setRolloverCursor(newLocation);
        }

        /**
         * something weird: cursor in JList behaves different from JTable?
         * Hmm .. no: using the table code snippets seems to fix #503-swingx
         * @param location
         */
        private void setRolloverCursor(Point location) {
            if (hasRollover(location)) {
                if (oldCursor == null) {
                    oldCursor = component.getCursor();
                    component.setCursor(Cursor
                            .getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            } else {
                if (oldCursor != null) {
                    component.setCursor(oldCursor);
                    oldCursor = null;
                }
            }
//            if (hasRollover(location)) {
//                oldCursor = component.getCursor();
//                component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//            } else {
//                component.setCursor(oldCursor);
//                oldCursor = null;
//            }

        }

        @Override
        protected RolloverRenderer getRolloverRenderer(Point location,
                boolean prepare) {
            ListCellRenderer renderer = component.getCellRenderer();
            RolloverRenderer rollover = renderer instanceof RolloverRenderer 
                ? (RolloverRenderer) renderer : null;
            if ((rollover != null) && !rollover.isEnabled()) {
                rollover = null;
            }
            if ((rollover != null) && prepare) {
                Object element = component.getModel().getElementAt(location.y);
                renderer.getListCellRendererComponent(component, element,
                        location.y, false, true);
            }
            return rollover;
        }

        @Override
        protected Point getFocusedCell() {
            int leadRow = component.getLeadSelectionIndex();
            if (leadRow < 0)
                return null;
            return new Point(0, leadRow);
        }

    }

//--------------------- public sort api
//    /** 
//     * Returns the sortable property.
//     * Here: same as filterEnabled.
//     * @return true if the table is sortable. 
//     */
//    public boolean isSortable() {
//        return isFilterEnabled();
//    }
    /**
     * Removes the interactive sorter.
     * 
     */
    public void resetSortOrder() {
        SortController controller = getSortController();
        if (controller != null) {
            controller.setSortKeys(null);
        }
    }

    /**
     * 
     * Toggles the sort order of the items.
     * <p>
     * The exact behaviour is defined by the SortController's
     * toggleSortOrder implementation. Typically a unsorted 
     * column is sorted in ascending order, a sorted column's
     * order is reversed. 
     * <p>
     * PENDING: where to get the comparator from?
     * <p>
     * 
     * 
     */
    public void toggleSortOrder() {
        SortController controller = getSortController();
        if (controller != null) {
            controller.toggleSortOrder(0, getComparator());
        }
    }

    /**
     * Sorts the list using SortOrder. 
     * 
     * 
     * Respects the JXList's sortable and comparator 
     * properties: routes the comparator to the SortController
     * and does nothing if !isFilterEnabled(). 
     * <p>
     * 
     * @param sortOrder the sort order to use. If null or SortOrder.UNSORTED, 
     *   this method has the same effect as resetSortOrder();
     *    
     */
    public void setSortOrder(SortOrder sortOrder) {
        if ((sortOrder == null) || !sortOrder.isSorted()) {
            resetSortOrder();
            return;
        }
        SortController sortController = getSortController();
        if (sortController != null) {
            SortKey sortKey = new SortKey(sortOrder, 
                    0, getComparator());    
            sortController.setSortKeys(Collections.singletonList(sortKey));
        }
    }


    /**
     * Returns the SortOrder. 
     * 
     * @return the interactive sorter's SortOrder  
     *  or SortOrder.UNSORTED 
     */
    public SortOrder getSortOrder() {
        SortController sortController = getSortController();
        if (sortController == null) return SortOrder.UNSORTED;
        SortKey sortKey = SortKey.getFirstSortKeyForColumn(sortController.getSortKeys(), 
                0);
        return sortKey != null ? sortKey.getSortOrder() : SortOrder.UNSORTED;
    }

    /**
     * 
     * @return the comparator used.
     * @see #setComparator(Comparator)
     */
    public Comparator getComparator() {
        return comparator;
    }
    
    /**
     * Sets the comparator used. As a side-effect, the 
     * current sort might be updated. The exact behaviour
     * is defined in #updateSortAfterComparatorChange. 
     * 
     * @param comparator the comparator to use.
     */
    public void setComparator(Comparator comparator) {
        Comparator old = getComparator();
        this.comparator = comparator;
        updateSortAfterComparatorChange();
        firePropertyChange("comparator", old, getComparator());
    }
    
    /**
     * Updates sort after comparator has changed. 
     * Here: sets the current sortOrder with the new comparator.
     *
     */
    protected void updateSortAfterComparatorChange() {
        setSortOrder(getSortOrder());
        
    }

    /**
     * returns the currently active SortController. Will be null if
     * !isFilterEnabled().
     * @return the currently active <code>SortController</code> may be null
     */
    protected SortController getSortController() {
//      // this check is for the sake of the very first call after instantiation
        // doesn't apply for JXList? need to test for filterEnabled?
        //if (filters == null) return null;
        if (!isFilterEnabled()) return null;
        return getFilters().getSortController();
    }
    
    
    // ---------------------------- filters

    /**
     * returns the element at the given index. The index is in view coordinates
     * which might differ from model coordinates if filtering is enabled and
     * filters/sorters are active.
     * 
     * @param viewIndex the index in view coordinates
     * @return the element at the index
     * @throws IndexOutOfBoundsException if viewIndex < 0 or viewIndex >=
     *         getElementCount()
     */
    public Object getElementAt(int viewIndex) {
        return getModel().getElementAt(viewIndex);
    }

    /**
     * Returns the number of elements in this list in view 
     * coordinates. If filters are active this number might be
     * less than the number of elements in the underlying model.
     * 
     * @return number of elements in this list in view coordinates
     */
    public int getElementCount() {
        return getModel().getSize();
    }

    /**
     * Convert row index from view coordinates to model coordinates accounting
     * for the presence of sorters and filters.
     * 
     * @param viewIndex index in view coordinates
     * @return index in model coordinates
     * @throws IndexOutOfBoundsException if viewIndex < 0 or viewIndex >= getElementCount() 
     */
    public int convertIndexToModel(int viewIndex) {
        return isFilterEnabled() ? getFilters().convertRowIndexToModel(
                viewIndex) : viewIndex;
    }

    /**
     * Convert index from model coordinates to view coordinates accounting
     * for the presence of sorters and filters.
     * 
     * PENDING Filter guards against out of range - should not? 
     * 
     * @param modelIndex index in model coordinates
     * @return index in view coordinates if the model index maps to a view coordinate
     *          or -1 if not contained in the view.
     * 
     */
    public int convertIndexToView(int modelIndex) {
        return isFilterEnabled() ? getFilters().convertRowIndexToView(
                modelIndex) : modelIndex;
    }

    /**
     * returns the underlying model. If !isFilterEnabled this will be the same
     * as getModel().
     * 
     * @return the underlying model
     */
    public ListModel getWrappedModel() {
        return isFilterEnabled() ? wrappingModel.getModel() : getModel();
    }

    /**
     * Enables/disables filtering support. If enabled all row indices -
     * including the selection - are in view coordinates and getModel returns a
     * wrapper around the underlying model.
     * 
     * Note: as an implementation side-effect calling this method clears the
     * selection (done in super.setModel).
     * 
     * PENDING: cleanup state transitions!! - currently this can be safely
     * applied once only to enable. Internal state is inconsistent if trying to
     * disable again. As a temporary emergency measure, this will throw a 
     * IllegalStateException. 
     * 
     * see Issue #2-swinglabs.
     * 
     * @param enabled
     * @throws IllegalStateException if trying to disable again.
     */
    public void setFilterEnabled(boolean enabled) {
        boolean old = isFilterEnabled();
        if (old == enabled)
            return;
        if (old) 
            throw new IllegalStateException("must not reset filterEnabled");
        // JW: filterEnabled must be set before calling super.setModel!
        filterEnabled = enabled;
        wrappingModel = new WrappingListModel(getModel());
        super.setModel(wrappingModel);
    }

    /**
     * 
     * @return a <boolean> indicating if filtering is enabled.
     * @see #setFilterEnabled(boolean)
     */
    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    /**
     * Overridden to update selectionMapper
     */
    @Override 
    public void setSelectionModel(ListSelectionModel newModel) {
        super.setSelectionModel(newModel);
        getSelectionMapper().setViewSelectionModel(getSelectionModel());
    }

    /**
     * 
     * Sets the underlying data model. Note that if isFilterEnabled you must
     * call getWrappedModel to access the model given here. In this case
     * getModel returns a wrapper around the data!
     * 
     * @param model the data model for this list.
     * 
     */
    @Override
    public void setModel(ListModel model) {
        if (isFilterEnabled()) {
            wrappingModel.setModel(model);
        } else {
            super.setModel(model);
        }
    }

    /**
     * widened access for testing...
     * @return the selection mapper
     */
    protected SelectionMapper getSelectionMapper() {
        if (selectionMapper == null) {
            selectionMapper = new DefaultSelectionMapper(filters, getSelectionModel());
        }
        return selectionMapper;
    }

    /**
     * 
     * @return the <code>FilterPipeline</code> assigned to this list, or
     *   null if !isFiltersEnabled().
     */
    public FilterPipeline getFilters() {
        if ((filters == null) && isFilterEnabled()) {
            setFilters(null);
        }
        return filters;
    }

    /** Sets the FilterPipeline for filtering the items of this list, maybe null
     *  to remove all previously applied filters. 
     *  
     *  Note: the current "interactive" sortState is preserved (by 
     *  internally copying the old sortKeys to the new pipeline, if any). 
     *  
     *  PRE: isFilterEnabled()
     * 
     * @param pipeline the <code>FilterPipeline</code> to use, null removes
     *   all filters.
     * @throws IllegalStateException if !isFilterEnabled()
     */
    public void setFilters(FilterPipeline pipeline) {
        if (!isFilterEnabled()) throw
            new IllegalStateException("filters not enabled - not allowed to set filters");

        FilterPipeline old = filters;
        List<? extends SortKey> sortKeys = null;
        if (old != null) {
            old.removePipelineListener(pipelineListener);
            sortKeys = old.getSortController().getSortKeys();
        }
        if (pipeline == null) {
            pipeline = new FilterPipeline();
        }
        filters = pipeline;
        filters.getSortController().setSortKeys(sortKeys);
        // JW: first assign to prevent (short?) illegal internal state
        // #173-swingx
        use(filters);
        getSelectionMapper().setFilters(filters);

    }

    /**
     * setModel() and setFilters() may be called in either order.
     * 
     * @param pipeline
     */
    private void use(FilterPipeline pipeline) {
        if (pipeline != null) {
            // check JW: adding listener multiple times (after setModel)?
            if (initialUse(pipeline)) {
                pipeline.addPipelineListener(getFilterPipelineListener());
                pipeline.assign(getComponentAdapter());
            } else {
                pipeline.flush();
            }
        }
    }

    /**
     * @return true is not yet used in this JXTable, false otherwise
     */
    private boolean initialUse(FilterPipeline pipeline) {
        if (pipelineListener == null)
            return true;
        PipelineListener[] l = pipeline.getPipelineListeners();
        for (int i = 0; i < l.length; i++) {
            if (pipelineListener.equals(l[i]))
                return false;
        }
        return true;
    }

    /** returns the listener for changes in filters. */
    protected PipelineListener getFilterPipelineListener() {
        if (pipelineListener == null) {
            pipelineListener = createPipelineListener();
        }
        return pipelineListener;
    }

    /** creates the listener for changes in filters. */
    protected PipelineListener createPipelineListener() {
        return new PipelineListener() {
            public void contentsChanged(PipelineEvent e) {
                updateOnFilterContentChanged();
            }
        };
    }

    /**
     * method called on change notification from filterpipeline.
     */
    protected void updateOnFilterContentChanged() {
        // make the wrapper listen to the pipeline?
        if (wrappingModel != null) {
            wrappingModel.updateOnFilterContentChanged();
        }
        revalidate();
        repaint();
    }

    private class WrappingListModel extends AbstractListModel {

        private ListModel delegate;

        private ListDataListener listDataListener;
        private Point OUTSIDE = new Point(-1, -1);
        protected boolean ignoreFilterContentChanged;

        public WrappingListModel(ListModel model) {
            setModel(model);
        }

        public void updateOnFilterContentChanged() {
            if (ignoreFilterContentChanged) return;
            fireContentsChanged(this, -1, -1);

        }

        public void setModel(ListModel model) {
            ListModel old = this.getModel();
            if (old != null) {
                old.removeListDataListener(listDataListener);
            }
            this.delegate = model;
            delegate.addListDataListener(getListDataListener());
            // sequence of method calls? 
            // fire contentsChanged after internal cleanup?
            fireContentsChanged(this, -1, -1);
            // fix #477-swingx
            getSelectionMapper().clearModelSelection();
            getFilters().flush();
        }

        private ListDataListener getListDataListener() {
            if (listDataListener == null) {
                listDataListener = createListDataListener();
            }
            return listDataListener;
        }

        private ListDataListener createListDataListener() {
            return new ListDataListener() {
                public void intervalAdded(ListDataEvent e) {
                    boolean wasEnabled = getSelectionMapper().isEnabled();
                    getSelectionMapper().setEnabled(false);
                    try {
                        updateSelection(e);
                        refireIntervalAdded(e);
                    } finally {
                        getSelectionMapper().setEnabled(wasEnabled);
                    }
                    ignoreFilterContentChanged = true;
                    getFilters().flush();
                    ignoreFilterContentChanged = false;
                }

                public void intervalRemoved(ListDataEvent e) {
                    boolean wasEnabled = getSelectionMapper().isEnabled();
                    getSelectionMapper().setEnabled(false);
                    try {
                        updateSelection(e);
                        refireIntervalRemoved(e);
                    } finally {
                        getSelectionMapper().setEnabled(wasEnabled);
                    }
                    ignoreFilterContentChanged = true;
                    getFilters().flush();
                    ignoreFilterContentChanged = false;
                }

                public void contentsChanged(ListDataEvent e) {
                    updateInternals(e);
                    refireContentsChanged(e);
                }
            };
        }

        /**
         * Refires the received event. Tries its best to map to the new
         * coordinates. At this point, the internals (selection, filter) are
         * updated, so it's safe to use the conversion methods.
         * 
         * @param e the ListDataEvent received from the wrapped model.
         */
        private void refireContentsChanged(ListDataEvent e) {
            // quick check for single item removal
            if ((e.getIndex0() >= 0) 
                && (e.getIndex0() == e.getIndex1())) {
                // single outside - no notification
                int viewIndex = convertIndexToView(e.getIndex0());
                if (viewIndex == -1) return;
                fireContentsChanged(this, viewIndex, viewIndex);
            } else if (e.getIndex0() >= 0) {
                // PENDING JW: narrow the interval bounds
                fireContentsChanged(this, 0, getSize());
            } else {
                fireContentsChanged(this, -1, -1);
            }
        }

        /**
         * Refires the received event. Tries its best to map to the new
         * coordinates. 
         * 
         * @param e the ListDataEvent received from the wrapped model.
         */
        private void refireIntervalRemoved(ListDataEvent e) {
            // quick check for single item removal
            if ((e.getIndex0() != - 1) 
                && (e.getIndex0() == e.getIndex1())) {
                // single outside - no notification
                int viewIndex = convertIndexToView(e.getIndex0());
                if (viewIndex == -1) return;
                fireIntervalRemoved(this, viewIndex, viewIndex);
                return;
            }
            Point mappedRange = getContinousMappedRange(e);
            if (mappedRange == null) {
             // cant help - no support for discontiouns interval remove notification
                fireContentsChanged(this, -1, -1); 
            } else if (OUTSIDE == mappedRange) {
                // do nothing, everything is outside
            } else {
                // we could map the continous range in model coordinates to
                // a continous range in view coordinates
                fireIntervalRemoved(this, mappedRange.x, mappedRange.y);
            } 
        }
        
        
        protected Point getContinousMappedRange(ListDataEvent e) {
            List<Integer> mapped = new ArrayList<Integer>();
            for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                int viewIndex = convertIndexToView(i);
                if (viewIndex >= 0) {
                    mapped.add(viewIndex);
                }
            }
            if (mapped.size() == 0) return OUTSIDE;
            if (mapped.size() == 1) return new Point(mapped.get(0), mapped.get(0));
            Collections.sort(mapped);
            for (int i = 0; i < mapped.size() - 2; i++) {
                if (mapped.get(i+1) - mapped.get(i) != 1) return null;
            }
            return new Point(mapped.get(0), mapped.get(mapped.size() - 1));
        }

        /**
         * Refires the received event. Tries its best to map to the new
         * coordinates. At this point, the internals (selection, filter) are
         * updated, so it's safe to use the conversion methods.
         * 
         * @param e the ListDataEvent received from the wrapped model.
         */
        private void refireIntervalAdded(ListDataEvent e) {
            // quick check for single item removal
            if ((e.getIndex0() != - 1) 
                && (e.getIndex0() == e.getIndex1())) {
                // single outside - no notification
                int viewIndex = convertIndexToView(e.getIndex0());
                if (viewIndex == -1) return;
                fireIntervalAdded(this, viewIndex, viewIndex);
                return;
            }
            Point mappedRange = getContinousMappedRange(e);
            if (mappedRange == null) {
             // cant help - no support for discontiouns interval remove notification
                fireContentsChanged(this, -1, -1); 
            } else if (OUTSIDE == mappedRange) {
                // do nothing, everything is outside
            } else {
                // we could map the continous range in model coordinates to
                // a continous range in view coordinates
                fireIntervalAdded(this, mappedRange.x, mappedRange.y);
            } 
        }
        
        private void updateInternals(ListDataEvent e) {
            boolean wasEnabled = getSelectionMapper().isEnabled();
            getSelectionMapper().setEnabled(false);
            try {
                updateSelection(e);
            } finally {
                getSelectionMapper().setEnabled(wasEnabled);
            }
            ignoreFilterContentChanged = true;
            getFilters().flush();
            ignoreFilterContentChanged = false;
        }

        protected void updateSelection(ListDataEvent e) {
            if (e.getType() == ListDataEvent.INTERVAL_REMOVED) {
                getSelectionMapper()
                        .removeIndexInterval(e.getIndex0(), e.getIndex1());
            } else if (e.getType() == ListDataEvent.INTERVAL_ADDED) {

                int minIndex = Math.min(e.getIndex0(), e.getIndex1());
                int maxIndex = Math.max(e.getIndex0(), e.getIndex1());
                int length = maxIndex - minIndex + 1;
                getSelectionMapper().insertIndexInterval(minIndex, length, true);
            } else if (e.getIndex0() == -1) {
                getSelectionMapper().clearModelSelection();
            }

        }

        public ListModel getModel() {
            return delegate;
        }

        public int getSize() {
            return getFilters().getOutputSize();
        }

        public Object getElementAt(int index) {
            return getFilters().getValueAt(index, 0);
        }



    }

    // ---------------------------- uniform data model

    /**
     * @return the unconfigured ComponentAdapter.
     */
    protected ComponentAdapter getComponentAdapter() {
        if (dataAdapter == null) {
            dataAdapter = new ListAdapter(this);
        }
        return dataAdapter;
    }

    /**
     * Convenience to access a configured ComponentAdapter.
     * Note: the column index of the configured adapter is always 0.
     * 
     * @param index the row index in view coordinates, must be valid.
     * @return the configured ComponentAdapter.
     */
    protected ComponentAdapter getComponentAdapter(int index) {
        ComponentAdapter adapter = getComponentAdapter();
        adapter.column = 0;
        adapter.row = index;
        return adapter;
    }
    
    /**
     * A component adapter targeted at a JXList.
     */
    protected static class ListAdapter extends ComponentAdapter {
        private final JXList list;

        /**
         * Constructs a <code>ListAdapter</code> for the specified target
         * JXList.
         * 
         * @param component  the target list.
         */
        public ListAdapter(JXList component) {
            super(component);
            list = component;
        }

        /**
         * Typesafe accessor for the target component.
         * 
         * @return the target component as a {@link org.jdesktop.swingx.JXList}
         */
        public JXList getList() {
            return list;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasFocus() {
            /** TODO: Think through printing implications */
            return list.isFocusOwner() && (row == list.getLeadSelectionIndex());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return list.getWrappedModel().getSize();
        }

        /**
         * {@inheritDoc} <p>
         * Overridden to return value at implicit view coordinates.
         */
        @Override
        public Object getValue() {
            return list.getElementAt(row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(int row, int column) {
            return list.getWrappedModel().getElementAt(row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getFilteredValueAt(int row, int column) {
            return list.getElementAt(row);
        }

        
        /**
         * {@inheritDoc}
         */
        @Override
        public String getFilteredStringAt(int row, int column) {
            return list.getStringAt(row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getString() {
            return list.getStringAt(row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValueAt(Object aValue, int row, int column) {
            throw new UnsupportedOperationException(
                    "The model is immutable.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEditable() {
            return false;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSelected() {
            /** TODO: Think through printing implications */
            return list.isSelectedIndex(row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(int columnIndex) {
            return "Column_" + columnIndex;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnIdentifier(int columnIndex) {
            return null;
        }

    }

    // ------------------------------ renderers

    /**
     * Returns the CompoundHighlighter assigned to the list, null if none.
     * PENDING: open up for subclasses again?.
     * 
     * @return the CompoundHighlighter assigned to the table.
     * @see #setCompoundHighlighter(CompoundHighlighter)
     */
    private CompoundHighlighter getCompoundHighlighter() {
        return compoundHighlighter;
    }

    /**
     * Assigns a CompoundHighlighter to the list, maybe null to remove all
     * Highlighters.<p>
     * 
     * The default value is <code>null</code>. <p>
     * 
     * PENDING: open up for subclasses again?.
     * @param pipeline the CompoundHighlighter to use for renderer decoration. 
     * @see #getCompoundHighlighter()
     * @see #addHighlighter(Highlighter)
     * @see #removeHighlighter(Highlighter)
     */
    private void setCompoundHighlighter(CompoundHighlighter pipeline) {
        CompoundHighlighter old = getCompoundHighlighter();
        if (old != null) {
            old.removeChangeListener(getHighlighterChangeListener());
        }
        compoundHighlighter = pipeline;
        if (compoundHighlighter != null) {
            compoundHighlighter.addChangeListener(getHighlighterChangeListener());
        }
        firePropertyChange("highlighters", old, getCompoundHighlighter());
    }

    /**
     * Sets the <code>Highlighter</code>s to the list, replacing any old settings.
     * None of the given Highlighters must be null.<p>
     * 
     * Note: the implementation is lenient with a single null highighter
     * to ease the api change from previous versions.
     * 
     * PENDING: property change? 
     * 
     * @param highlighters zero or more not null highlighters to use for renderer decoration.
     * 
     * @see #getHighlighters()
     * @see #addHighlighter(Highlighter)
     * @see #removeHighlighter(Highlighter)
     * 
     */
    public void setHighlighters(Highlighter... highlighters) {
        CompoundHighlighter pipeline = null;
        if ((highlighters != null) && (highlighters.length > 0) && 
            (highlighters[0] != null)) {    
           pipeline = new CompoundHighlighter(highlighters);
        }
        setCompoundHighlighter(pipeline);
    }

    /**
     * Returns the <code>Highlighter</code>s used by this list.
     * Maybe empty, but guarantees to be never null.
     * @return the Highlighters used by this list, guaranteed to never null.
     * @see #setHighlighters(Highlighter[])
     */
    public Highlighter[] getHighlighters() {
        return getCompoundHighlighter() != null ? 
                getCompoundHighlighter().getHighlighters() : 
                    CompoundHighlighter.EMPTY_HIGHLIGHTERS;
    }
    
    /**
     * Adds a Highlighter. Appends to the end of the list of used
     * Highlighters.
     * <p>
     * 
     * @param highlighter the <code>Highlighter</code> to add.
     * @throws NullPointerException if <code>Highlighter</code> is null.
     * 
     * @see #removeHighlighter(Highlighter)
     * @see #setHighlighters(Highlighter[])
     */
    public void addHighlighter(Highlighter highlighter) {
        CompoundHighlighter pipeline = getCompoundHighlighter();
        if (pipeline == null) {
           setCompoundHighlighter(new CompoundHighlighter(highlighter)); 
        } else {
            pipeline.addHighlighter(highlighter);
        }
    }

    /**
     * Removes the given Highlighter. <p>
     * 
     * Does nothing if the Highlighter is not contained.
     * 
     * @param highlighter the Highlighter to remove.
     * @see #addHighlighter(Highlighter)
     * @see #setHighlighters(Highlighter...)
     */
    public void removeHighlighter(Highlighter highlighter) {
        if ((getCompoundHighlighter() == null)) return;
        getCompoundHighlighter().removeHighlighter(highlighter);
    }
    

    /**
     * returns the ChangeListener to use with highlighters. Creates one if
     * necessary.
     * 
     * @return != null
     */
    protected ChangeListener getHighlighterChangeListener() {
        if (highlighterChangeListener == null) {
            highlighterChangeListener = new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    repaint();
                }

            };
        }
        return highlighterChangeListener;
    }

    
    /**
     * Returns the string representation of the cell value at the given position. 
     * 
     * @param row the row index of the cell in view coordinates
     * @return the string representation of the cell value as it will appear in the 
     *   table. 
     */
    public String getStringAt(int row) {
        ListCellRenderer renderer = getDelegatingRenderer().getDelegateRenderer();
        if (renderer instanceof StringValue) {
            return ((StringValue) renderer).getString(getElementAt(row));
        }
        return StringValue.TO_STRING.getString(getElementAt(row));
    }

    private DelegatingRenderer getDelegatingRenderer() {
        if (delegatingRenderer == null) {
            // only called once... to get hold of the default?
            delegatingRenderer = new DelegatingRenderer(createDefaultCellRenderer());
        }
        return delegatingRenderer;
    }

    /**
     * Creates and returns the default cell renderer to use. Subclasses
     * may override to use a different type. Here: returns a <code>DefaultListRenderer</code>.
     * 
     * @return the default cell renderer to use with this list.
     */
    protected ListCellRenderer createDefaultCellRenderer() {
        return new DefaultListRenderer();
    }

    @Override
    public ListCellRenderer getCellRenderer() {
        return getDelegatingRenderer();
    }

    @Override
    public void setCellRenderer(ListCellRenderer renderer) {
        // JW: Pending - probably fires propertyChangeEvent with wrong newValue?
        // how about fixedCellWidths?
        // need to test!!
        getDelegatingRenderer().setDelegateRenderer(renderer);
        super.setCellRenderer(delegatingRenderer);
    }

    public class DelegatingRenderer implements ListCellRenderer, RolloverRenderer {

        private ListCellRenderer delegateRenderer;

        public DelegatingRenderer(ListCellRenderer delegate) {
            setDelegateRenderer(delegate);
        }

        public void setDelegateRenderer(ListCellRenderer delegate) {
            if (delegate == null) {
                delegate = new DefaultListCellRenderer();
            }
            delegateRenderer = delegate;
        }

        
        public ListCellRenderer getDelegateRenderer() {
            return delegateRenderer;
        }

        public boolean isEnabled() {
            return (delegateRenderer instanceof RolloverRenderer) && 
               ((RolloverRenderer) delegateRenderer).isEnabled();
        }
        
        public void doClick() {
            if (isEnabled()) {
                ((RolloverRenderer) delegateRenderer).doClick();
            }
        }
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            Component comp = delegateRenderer.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
            if ((compoundHighlighter != null) && (index >= 0) && (index < getElementCount())) {
                comp = compoundHighlighter.highlight(comp, getComponentAdapter(index));
            }
            return comp;
        }


        public void updateUI() {
            updateRendererUI(delegateRenderer);
        }

        private void updateRendererUI(ListCellRenderer renderer) {
            if (renderer instanceof JComponent) {
                ((JComponent) renderer).updateUI();
            } else if (renderer != null) {
                Component comp = renderer.getListCellRendererComponent(
                        JXList.this, null, -1, false, false);
                if (comp instanceof JComponent) {
                    ((JComponent) comp).updateUI();
                }
            }

        }

    }

    // --------------------------- updateUI

    @Override
    public void updateUI() {
        super.updateUI();
        updateRendererUI();
    }

    private void updateRendererUI() {
        if (delegatingRenderer != null) {
            delegatingRenderer.updateUI();
        } else {
            ListCellRenderer renderer = getCellRenderer();
            if (renderer instanceof JComponent) {
                ((JComponent) renderer).updateUI();
            }
        }
    }

}