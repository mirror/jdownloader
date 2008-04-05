/*
 * $Id: Filter.java,v 1.16 2008/02/18 13:52:42 kleopatra Exp $
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

package org.jdesktop.swingx.decorator;

/**
 * <p>A <b><code>Filter</code></b> is used to filter the data presented in a
 * data-aware component such as a {@link org.jdesktop.swingx.JXList} or a
 * {@link org.jdesktop.swingx.JXTable}. Filtering involves interposing one or
 * more filters in a {@link org.jdesktop.swingx.decorator.FilterPipeline} between
 * a data model and a view to change the apparent order and/or number of records
 * in the data model.</p>
 *
 * @author Ramesh Gupta
 * @see org.jdesktop.swingx.decorator.FilterPipeline
 * @see org.jdesktop.swingx.JXTable
 */
public abstract class Filter {
    /** the column the filter is bound to. JW: no need to make it final. */
    private /** final */ int column;		// in model coordinates
    protected FilterPipeline		pipeline = null;
    protected ComponentAdapter	adapter = null;	/** TODO: make private */
    protected int[]             fromPrevious = new int[0];
    // TODO: JW... magic number!
    int order = -1;	// package private

    /**
     * Constructs a new filter for the first column of a data model (in model coordinates).
     */
    public Filter() {
        this(0);
    }

    /**
     * Constructs a new filter for the specified column of a data model (in model coordinates).
     *
     * @param col column index in model coordinates
     */
    public Filter(int col) {
        column = col;
        init();
    }

    
//----------------- public methods meant for end-client access
    
    /**
     * Refreshes the internal state of the filter, performs the {@link #filter() filter}
     * operation and regenerates row mappings from the previous filter. If this
     * filter is bound to a filter pipeline (as most filters are), it also triggers a
     * {@link org.jdesktop.swingx.decorator.FilterPipeline#filterChanged(org.jdesktop.swingx.decorator.Filter) filterChanged}
     * notification.
     */
    public void refresh() {
        refresh(true);
    }

    /**
     * Returns the model index of the column that this filter has been bound to.
     *
     * @return the model index of the column that this filter has been bound to
     */
    public int getColumnIndex() {
        return column;  // model coordinates
    }

    /**
     * TODO: PENDING: not tested!
     * 
     * @param modelColumn
     */
    public void setColumnIndex(int modelColumn) {
        if (getColumnIndex() == modelColumn) return;
        this.column = modelColumn;
        refresh();
        
    }
    public String getColumnName() {
        if (adapter == null) {
            return "Column " + column;  // in model coordinates :-(
        }
        else {
            return adapter.getColumnName(getColumnIndex());
        }
    }

    /**
     * Convert row index from view coordinates to model coordinates
     * accounting for the presence of sorters and filters.
     *
     * PRE: 0 <= row < getSize()
     *  
     * @param row row index in this filters "view" coordinates - 
     * @return row index in model coordinates
     */
    public int convertRowIndexToModel(int row) {
        int mappedRow = mapTowardModel(row);
        Filter filter = getMappingFilter();
        if (filter != null) {
            mappedRow = filter.convertRowIndexToModel(mappedRow);
        }
        return mappedRow;
    }


    /**
     * Convert row index from model coordinates to view coordinates accounting
     * for the presence of sorters and filters.
     * 
     * @param row row index in model coordinates
     * @return row index in this filter's "view" coordinates
     */
    public int convertRowIndexToView(int row) {
        int mappedRow = row;
        Filter filter = getMappingFilter();
        if (filter != null) {
            mappedRow = filter.convertRowIndexToView(mappedRow);
        }
        return mapTowardView(mappedRow);
    }

    /**
     * Returns the value at the specified row and column.
     * 
     * PRE: 0 <= row < getSize()
     * 
     * @param row row index in this filter's "view" coordinates
     * @param column column index in model coordinates
     * @return the value at the specified row and column
     */
    public Object getValueAt(int row, int column) {
        int mappedRow = mapTowardModel(row);
        Filter filter = getMappingFilter();
        if (filter != null) {
            return filter.getValueAt(mappedRow, column);
        }
        return adapter.getValueAt(mappedRow, column);
    }

    /**
     * Sets the specified value as the new value for the cell identified by the
     * specified row and column index.
     *
     * PRE: 0 <= row < getSize()
     * 
     * @param aValue new value for the specified cell
     * @param row row index in this filter's "view" coordinates
     * @param column column index in model coordinates
     */
    public void setValueAt(Object aValue, int row, int column) {
        int mappedRow = mapTowardModel(row);
        Filter filter = getMappingFilter();
        if (filter != null) {
            filter.setValueAt(aValue, mappedRow, column);
            return; // make sure you return from here!
        }
        adapter.setValueAt(aValue, mappedRow, column);
    }

    /**
     * returns editability of the cell identified by the specified row
     * and column index.
     * 
     * PRE: 0 &lt;= row &lt; <code>getSize()</code>
     * 
     * @param row row index in this filter's "view" coordinates
     * @param column
     * @return true if the cell at the specified row/col is editable
     */
    public boolean isCellEditable(int row, int column) {
        int mappedRow = mapTowardModel(row);
        Filter filter = getMappingFilter();
        if (filter != null) {
            return filter.isCellEditable(mappedRow, column);
        }
        return adapter.isCellEditable(mappedRow, column);
    }


    /**
     * Returns the number of records that remain in this filter's "view"
     * after the input records have been filtered.
     *
     * @return the number of records that remain in this filter's "view"
     * after the input records have been filtered
     */
    public abstract int getSize();


//---------------------------------- for subclasses

    /**
     * Returns the number of records that are processed by this filter.
     *
     * @return the number of records that are processed by this filter
     */
    protected int getInputSize() {
        return pipeline == null ? adapter == null ?
                0 : adapter.getRowCount() : pipeline.getInputSize(this);
    }

    /**
     * Returns the value of the cell at the specified row and column (in model coordinates).
     *
     * @param row in the coordinates of what is the filter's "view" of the model
     * @param column in model coordinates
     * @return the value of the cell at the specified row and column (in model coordinates)
     */
    protected Object getInputValue(int row, int column) {
        Filter filter = getMappingFilter();
        if (filter != null) {
            return filter.getValueAt(row, column);
        }
        if (adapter != null) {
            return adapter.getValueAt(row, column);
        }

        return null;
    }

    /**
     * Provides filter-specific initialization. Called from the <code>Filter</code>
     * constructor.
     */
    protected abstract void init();

    /**
     * Resets the internal row mappings from this filter to the previous filter.
     */
    protected abstract void reset();

    /**
     * Performs the filter operation defined by this filter.
     */
    protected abstract void filter();


    /**
     * PRE: 0 &lt;= row &lt; <code>getSize();</code>
     * 
     * @param row
     * @return TODO:
     */
    protected abstract int mapTowardModel(int row);

    /**
     * PRE: 0 &lt;= row &lt; <code>getInputSize();</code>
     * 
     * @param row
     * @return TODO:
     */
    protected int mapTowardView(int row) {
        // WARNING: Not all model indices map to view when view is filtered!
        // JW - TODO: cleanup and clarify preconditions in all mapping methods
        // in all towardView the row must be < getInputSize
        // in add towardModel the row must be < getSize
        return row < 0 || row >= fromPrevious.length ? - 1 : fromPrevious[row];
    }

    /**
     * Returns the filter to use for accessing input.
     * That's the previous (model is first) filter if this is 
     * part of a pipeline or null if this is standalone or the first
     * in the pipeline.
     * 
     * @return filter to use for accessing input
     */
    protected Filter getMappingFilter() {
        Filter filter = null;
        if (pipeline != null) {
            filter = pipeline.previous(this);
        }
        return filter;
    }
    
    /**
     * Refreshes the internal state of the filter, optionally resetting the
     * cache of existing row mappings from this filter to the previous filter.
     * Always performs the {@link #filter() filter} operation and regenerates
     * row mappings from the previous filter. If this filter is bound to a filter
     * pipeline (as most filters are), it also triggers a
     * {@link org.jdesktop.swingx.decorator.FilterPipeline#filterChanged(org.jdesktop.swingx.decorator.Filter) filterChanged}
     * notification.
     *
     * @param reset true if existing row mappings from this filter to the previous
     * filter should be reset; false, if the existing row mappings should be preserved.
     */
    protected void refresh(boolean reset) {
        if (reset) {
            reset();
        }
        filter();
        fireFilterChanged();
    }

    /**
     * Notifies interested parties that this filter has changed.
     * 
     */
    protected void fireFilterChanged() {
        // trigger direct notification; will cascade to next in pipeline, if any
        if (pipeline != null) {
            if (pipeline.contains(this)) {
                pipeline.filterChanged(this);
                return;
            }
        }
        if (adapter != null) {
            adapter.refresh();
        }
    }

    /**
     * Binds this filter to the specified <code>ComponentAdapter</code>.
     * Called by {@link FilterPipeline#assign(ComponentAdapter)}
     *
     * @param adapter adapter that this filter is bound to
     */
    protected void assign(ComponentAdapter adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("null adapter");
        }

        if (this.adapter == null) {
            this.adapter = adapter;
        }
        else if (this.adapter != adapter){
            throw new IllegalStateException("Already bound to another adapter");
        }
    }

    /**
     * Binds this filter to the specified filter pipeline.
     *
     * @param pipeline the filter pipeline that this filter is bound to
     */
    final void assign(FilterPipeline pipeline) {
        /** NOTE: JXTable.resetSorter may pass in null for filter pipeline!
        if (pipeline == null) {
            throw new IllegalArgumentException("null pipeline");
        }
		*/

        if ((this.pipeline == null) || (pipeline == null)) {
            this.pipeline = pipeline;
        }
        else if (this.pipeline != pipeline) {
            throw new IllegalStateException("Already bound to another pipeline");
        }
    }

    /**
     * Called by {@link FilterPipeline#assignFilters()}
     */
    void assign(FilterPipeline pipeline, int i) {
        if (order >= 0) {
            throw new IllegalArgumentException("Element " + i +
            " is part of another pipeline.");
        }
        this.order = i;
        assign(pipeline);
    }

    protected FilterPipeline getPipeline() {
        return pipeline;
    }
}