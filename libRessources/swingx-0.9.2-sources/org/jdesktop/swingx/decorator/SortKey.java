/*
 * Created on 15.03.2006
 *
 */
package org.jdesktop.swingx.decorator;

import java.util.Comparator;
import java.util.List;

/**
 * A column and how its sorted.
 */
public class SortKey {
    private final SortOrder sortOrder;
    private final int column;
    private final Comparator comparator;
    
    /**
     * @param sortOrder one of {@link SortOrder#ASCENDING},
     *     {@link SortOrder#DESCENDING} or {@link SortOrder#UNSORTED}.
     * @param column a column in terms of <strong>model</strong> index.
     */
    public SortKey(SortOrder sortOrder, int column) {
        this(sortOrder, column, null);
    }

    /**
     * @param sortOrder one of {@link SortOrder#ASCENDING},
     *     {@link SortOrder#DESCENDING} or {@link SortOrder#UNSORTED}.
     * @param column a column in terms of <strong>model</strong> index.
     * @param comparator the comparator to use with this sort.
     */
    public SortKey(SortOrder sortOrder, int column, Comparator comparator) {
        if(sortOrder == null) throw new IllegalArgumentException();
        if(column < 0) throw new IllegalArgumentException();
        this.column = column;
        this.comparator = comparator;
        this.sortOrder = sortOrder;
    }

    /**
     * The sort order, ascending, descending or unsorted.
     */
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    /**
     * The sorting column in terms of <strong>model</strong> index.
     */
    public int getColumn() {
        return column;
    }

    /**
     * The comparator to use, might be null.
     */
    public Comparator getComparator() {
        return comparator;
    }
    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final SortKey sortKey = (SortKey) o;

        if (column != sortKey.column) return false;
        return !(sortOrder != null ? !sortOrder.equals(sortKey.sortOrder) : sortKey.sortOrder != null);

    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int result;
        result = (sortOrder != null ? sortOrder.hashCode() : 0);
        result = 29 * result + column;
        return result;
    }
    
//---------------------- static utility methods
    
    /**
     * Returns the first SortKey in the list which is sorted. 
     * If none is sorted, null is returned.
     * 
     * @param keys a list of SortKeys to search
     * @return the first SortKey which is sorted or null, if no
     *   is found.
     */
    public static SortKey getFirstSortingKey(List<? extends SortKey> keys) {
        for (SortKey key : keys) {
            if (key.getSortOrder().isSorted()) {
                return key;
            }
        }
        return null;
    }

    /**
     * Returns the first SortKey in the list for the given column, 
     * or null if the column has no SortKey. 
     * 
     * @param keys a list of SortKeys to search
     * @param modelColumn the column index in model coordinates
     * @return the first SortKey for the given column or null if none is
     *   found.
     */
    public static SortKey getFirstSortKeyForColumn(List<? extends SortKey> keys, int modelColumn) {
        for (SortKey key : keys) {
            if (key.getColumn() == modelColumn) {
                return key;
            }
        }
        return null;
    }

}
