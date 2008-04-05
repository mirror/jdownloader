/**
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
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
package org.jdesktop.swingx.calendar;

import java.util.Date;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jdesktop.swingx.event.DateSelectionEvent.EventType;
import org.jdesktop.swingx.util.Contract;

/**
 * DateSelectionModel which allows a single selection only. Takes dates as-is.
 * 
 * Temporary quick & dirty class to explore requirements as needed by
 * a DatePicker.
 * 
 * @author Jeanette Winzenburg
 */
public class SingleDaySelectionModel extends AbstractDateSelectionModel {

    private SortedSet<Date> selectedDates;
    private SortedSet<Date> unselectableDates;
    private Date upperBound;
    private Date lowerBound;

    /**
     * 
     */
    public SingleDaySelectionModel() {
        this(null);
    }

    /**
     * 
     */
    public SingleDaySelectionModel(Locale locale) {
        super(locale);
        this.selectedDates = new TreeSet<Date>();
        this.unselectableDates = new TreeSet<Date>();
    }

    /**
     * {@inheritDoc}
     */
    public SelectionMode getSelectionMode() {
        return SelectionMode.SINGLE_SELECTION;
    }

    /**
     * {@inheritDoc}<p>
     * 
     * Implemented to do nothing.
     * 
     */
    public void setSelectionMode(final SelectionMode selectionMode) {
    }


    //---------------------- selection ops    
    /**
     * {@inheritDoc} <p>
     * 
     * Implemented to call setSelectionInterval with startDate for both 
     * parameters.
     */
    public void addSelectionInterval(Date startDate, Date endDate) {
        setSelection(startDate);
    }

    /**
     * {@inheritDoc}<p>
     * 
     * PENDING JW: define what happens if we have a selection but the interval
     * isn't selectable. 
     */
    public void setSelectionInterval(Date startDate, Date endDate) {
        setSelection(startDate);
    }

    /**
     * {@inheritDoc}
     */
    public void removeSelectionInterval(Date startDate, Date endDate) {
        Contract.asNotNull(startDate, "date must not be null");
        if (isSelectionEmpty()) return;
        if (isSelectionInInterval(startDate, endDate)) {
            selectedDates.clear();
            fireValueChanged(EventType.DATES_REMOVED);
        }
    }
    
    /**
     * Checks and returns whether the selected date is contained in the interval
     * given by startDate/endDate. The selection must not be empty when 
     * calling this method. <p>
     * 
     * This implementation interprets the interval between the start of the day
     * of startDay to the end of the day of endDate. 
     * 
     * @param startDate the start of the interval, must not be null
     * @param endDate  the end of the interval, must not be null
     * @return true if the selected date is contained in the interval
     */
    protected boolean isSelectionInInterval(Date startDate, Date endDate) {
        if (selectedDates.first().before(startOfDay(startDate)) 
                || (selectedDates.first().after(endOfDay(endDate)))) return false;
        return true;
    }

    /**
     * Selects the given date if it is selectable and not yet selected. 
     * Does nothing otherwise.
     * If this operation changes the current selection, it will fire a 
     * DateSelectionEvent of type DATES_SET.
     * 
     * @param date the Date to select, must not be null. 
     */
    protected void setSelection(Date date) {
        Contract.asNotNull(date, "date must not be null");
        if (isSelectedStrict(date)) return;
        if (isSelectable(date)) {
            selectedDates.clear();
            // PENDING JW: use normalized
            selectedDates.add(date);
            fireValueChanged(EventType.DATES_SET);
        }
    }
    
    /**
     * Checks and returns whether the given date is contained in the selection.
     * This differs from isSelected in that it tests for the exact (normalized)
     * Date instead of for the same day.
     * 
     * @param date the Date to test.
     * @return true if the given date is contained in the selection, 
     *    false otherwise
     * 
     */
    private boolean isSelectedStrict(Date date) {
        if (!isSelectionEmpty()) {
            // PENDING JW: use normalized
            return selectedDates.first().equals(date);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Date getFirstSelectionDate() {
        return isSelectionEmpty() ? null : selectedDates.first();
    }

    /**
     * {@inheritDoc}
     */
    public Date getLastSelectionDate() {
        return isSelectionEmpty() ? null : selectedDates.last();
    }

    /**
     * Returns a boolean indicating whether the given date is selectable.
     * 
     * @param date the date to check for selectable, must not be null.
     * @return true if the given date is selectable, false if not.
     */
    public  boolean isSelectable(Date date) {
        if (outOfBounds(date)) return false;
        return !inUnselectables(date);
    }

    /**
     * @param date
     * @return
     */
    private boolean inUnselectables(Date date) {
        for (Date unselectable : unselectableDates) {
            if (isSameDay(unselectable, date)) return true;
        }
        return false;
    }

    /**
     * Returns a boolean indication whether the given date is below
     * the lower or above the upper bound. 
     * 
     * @param date
     * @return
     */
    private boolean outOfBounds(Date date) {
        if (belowLowerBound(date)) return true;
        if (aboveUpperBound(date)) return true;
        return false;
    }

    /**
     * @param date
     * @return
     */
    private boolean aboveUpperBound(Date date) {
        if (upperBound != null) {
            return endOfDay(upperBound).before(date);
        }
        return false;
    }

    /**
     * @param date
     * @return
     */
    private boolean belowLowerBound(Date date) {
        if (lowerBound != null) {
           return startOfDay(lowerBound).after(date);
        }
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public void clearSelection() {
        if (isSelectionEmpty()) return;
        selectedDates.clear();
        fireValueChanged(EventType.SELECTION_CLEARED);
    }


    /**
     * {@inheritDoc}
     */
    public SortedSet<Date> getSelection() {
        return new TreeSet<Date>(selectedDates);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSelected(Date date) {
        if (isSelectionEmpty()) return false;
        return isSameDay(selectedDates.first(), date);
    }

    

    /**
     * {@inheritDoc}<p>
     * 
     * Implemented to return the date itself.
     */
    public Date getNormalizedDate(Date date) {
        return new Date(date.getTime());
    }


    /**
     * {@inheritDoc}
     */
    public boolean isSelectionEmpty() {
        return selectedDates.isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    public SortedSet<Date> getUnselectableDates() {
        return new TreeSet<Date>(unselectableDates);
    }

    /**
     * {@inheritDoc}
     */
    public void setUnselectableDates(SortedSet<Date> unselectables) {
        Contract.asNotNull(unselectables, "unselectable dates must not be null");
        this.unselectableDates.clear();
        for (Date unselectableDate : unselectables) {
            removeSelectionInterval(unselectableDate, unselectableDate);
            unselectableDates.add(unselectableDate);
        }
        fireValueChanged(EventType.UNSELECTED_DATES_CHANGED);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUnselectableDate(Date date) {
        return !isSelectable(date);
    }

    /**
     * {@inheritDoc}
     */
    public Date getUpperBound() {
        return upperBound;
    }

    /**
     * {@inheritDoc}
     */
    public void setUpperBound(Date upperBound) {
        if ((upperBound != null && !upperBound.equals(this.upperBound)) ||
                (upperBound == null && upperBound != this.upperBound)) {
            this.upperBound = upperBound;
            if (!isSelectionEmpty() && selectedDates.last().after(this.upperBound)) {
                if (this.upperBound != null) {
                    // Remove anything above the upper bound
                    long justAboveUpperBoundMs = this.upperBound.getTime() + 1;
                    if (!selectedDates.isEmpty() && selectedDates.last().before(this.upperBound))
                        removeSelectionInterval(this.upperBound, new Date(justAboveUpperBoundMs));
                }
            }
            fireValueChanged(EventType.UPPER_BOUND_CHANGED);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Date getLowerBound() {
        return lowerBound;
    }

    /**
     * {@inheritDoc}
     */
    public void setLowerBound(Date lowerBound) {
        if ((lowerBound != null && !lowerBound.equals(this.lowerBound)) ||
                (lowerBound == null && lowerBound != this.lowerBound)) {
            this.lowerBound = lowerBound;
            if (this.lowerBound != null) {
                // Remove anything below the lower bound
                long justBelowLowerBoundMs = this.lowerBound.getTime() - 1;
                if (!isSelectionEmpty() && selectedDates.first().before(this.lowerBound)) {
                    removeSelectionInterval(selectedDates.first(), new Date(justBelowLowerBoundMs));
                }
            }
            fireValueChanged(EventType.LOWER_BOUND_CHANGED);
        }
    }




}