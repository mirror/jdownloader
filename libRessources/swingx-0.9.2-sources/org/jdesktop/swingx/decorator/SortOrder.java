/*
 * Created on 10.03.2006
 *
 */
package org.jdesktop.swingx.decorator;

/**
 * Encasulates sort state. 
 * There are several conenience methods to simplify usage of the three possible states 
 *  (unsorted, ascending sorted, descending sorted).
 *  PENDING: incomplete.
 * 
 * 
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public final class SortOrder {
    public static final SortOrder ASCENDING = new SortOrder("ascending");
    public static final SortOrder DESCENDING = new SortOrder("descending");
    public static final SortOrder UNSORTED = new SortOrder("unsorted");

    private final String name;
    private SortOrder(String name) {
        this.name = name;
    }
    
    /**
     * Convenience to check if the order is sorted.
     * @return false if unsorted, true for ascending/descending.
     */
    public boolean isSorted() {
        return this != UNSORTED;
    }
    
    public boolean isSorted(boolean ascending) {
        return isSorted() && (ascending == isAscending());
    }
    
    /**
     * Convenience to check for ascending sort order.
     * PENDING: is this helpful at all?
     * 
     * @return true if ascendingly sorted, false for unsorted/descending.
     */
    public boolean isAscending() {
        return this == ASCENDING;
    }
    
    @Override
    public String toString() {
        return name;
    }

}
