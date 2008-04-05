/*
 * $Id: HighlightPredicate.java,v 1.19 2008/02/29 14:24:47 kleopatra Exp $
 *
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
 *
 */
package org.jdesktop.swingx.decorator;

import java.awt.Component;
import java.awt.Point;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jdesktop.swingx.RolloverProducer;
import org.jdesktop.swingx.util.Contract;

/**
 * A controller which decides whether or not a visual decoration should
 * be applied to the given Component in the given ComponentAdapter state. 
 * This is a on/off <b>decision</b> only, the actual decoration is
 * left to the AbstractHighlighter which typically respects this predicate. <p>
 * 
 * @author Jeanette Winzenburg
 * 
 * @see AbstractHighlighter
 */
public interface HighlightPredicate {

    /**
     * Returns a boolean to indicate whether the component should be 
     * highlighted.<p>
     * 
     * Note: both parameters should be considered strictly read-only!
     * 
    * @param renderer the cell renderer component that is to be decorated,
    *    must not be null
    * @param adapter the ComponentAdapter for this decorate operation,
    *    most not be null
    * @return a boolean to indicate whether the component should be highlighted.
     */
    boolean isHighlighted(Component renderer, ComponentAdapter adapter);

    
//--------------------- implemented Constants    
    /**
     * Unconditional true.
     */
    public static final HighlightPredicate ALWAYS = new HighlightPredicate() {

        /**
         * {@inheritDoc} <p>
         * 
         * Implemented to return true always.
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return true;
        }
        
    };

    /**
     * Unconditional false.
     */
    public static final HighlightPredicate NEVER = new HighlightPredicate() {

        /**
         * {@inheritDoc} <p>
         * 
         * Implemented to return false always.
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return false;
        }
        
    };
    
    /**
     * Rollover  Row.
     */
    public static final HighlightPredicate ROLLOVER_ROW = new HighlightPredicate() {
        
        /**
         * @inheritDoc
         * Implemented to return true if the adapter's component is enabled and
         * the row of its rollover property equals the adapter's row, returns
         * false otherwise.
         * 
         * @see org.jdesktop.swingx.RolloverProducer
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            if (!adapter.getComponent().isEnabled()) return false;
            // JW: where to put constants? 
            // this is a back-reference into swingx simply to access
            // a string constant. Hmmm...
            Point p = (Point) adapter.getComponent().getClientProperty(
                    RolloverProducer.ROLLOVER_KEY);
            return p != null &&  p.y == adapter.row;
        }
        
    };
    
    /**
     * Is editable.
     */
    public static final HighlightPredicate EDITABLE = new HighlightPredicate() {
        /**
         * {@inheritDoc} <p>
         * 
         * Implemented to return true is the given adapter isEditable, false otherwise.
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return adapter.isEditable();
        }
    };
    
    /**
     * Convenience for read-only (same as !editable).
     */
    public static final HighlightPredicate READ_ONLY = new HighlightPredicate() {
        /**
         * {@inheritDoc} <p>
         * 
         * Implemented to return false is the given adapter isEditable, true otherwise.
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return !adapter.isEditable();
        }
    };
    
    /**
     * Leaf predicate.
     */
    public static final HighlightPredicate IS_LEAF = new HighlightPredicate() {
        /**
         * {@inheritDoc} <p>
         * 
         * Implemented to return true if the given adapter isLeaf, false otherwise.
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return adapter.isLeaf();
        }
    };
    
    /**
     * Folder predicate - convenience: same as !IS_LEAF.
     */
    public static final HighlightPredicate IS_FOLDER = new HighlightPredicate() {
        /**
         * {@inheritDoc} <p>
         * 
         * Implemented to return false if the given adapter isLeaf, true otherwise.
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return !adapter.isLeaf();
        }
    };
    
    /**
     * Even rows.
     * 
     * PENDING: this is zero based (that is "really" even 0, 2, 4 ..), differing 
     * from the old AlternateRowHighlighter.
     * 
     */
    public static final HighlightPredicate EVEN = new HighlightPredicate() {

        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return adapter.row % 2 == 0;
        }
        
    };
    
    /**
     * Odd rows.
     * 
     * PENDING: this is zero based (that is 1, 3, 4 ..), differs from 
     * the old implementation which was one based?
     * 
     */
    public static final HighlightPredicate ODD = new HighlightPredicate() {

        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return !EVEN.isHighlighted(renderer, adapter);
        }
        
    };
    
    /**
     * Negative BigDecimals.
     */
     public static final HighlightPredicate BIG_DECIMAL_NEGATIVE = new HighlightPredicate() {

        public boolean isHighlighted(Component renderer,
                ComponentAdapter adapter) {
            return (adapter.getValue() instanceof BigDecimal) 
               && ((BigDecimal) adapter.getValue()).compareTo(BigDecimal.ZERO) < 0;
        }
        
    };

    /**
     * Negative Number.
     */
     public static final HighlightPredicate INTEGER_NEGATIVE = new HighlightPredicate() {

        public boolean isHighlighted(Component renderer,
                ComponentAdapter adapter) {
            return (adapter.getValue() instanceof Number) 
               && ((Number) adapter.getValue()).intValue() < 0;
        }
        
    };
    
//----------------- logical implementations amongst HighlightPredicates
    
    /**
     * Negation of a HighlightPredicate.
     */
    public static class NotHighlightPredicate implements HighlightPredicate {
        
        private HighlightPredicate predicate;
        
        /**
         * Instantiates a not against the given predicate.
         * @param predicate the predicate to negate, must not be null.
         * @throws NullPointerException if the predicate is null
         */
        public NotHighlightPredicate(HighlightPredicate predicate) {
            if (predicate == null) 
                throw new NullPointerException("predicate must not be null");
            this.predicate = predicate;
        }
        
        /**
         * {@inheritDoc}
         * Implemented to return the negation of the given predicate.
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return !predicate.isHighlighted(renderer, adapter);
        }
        
    }
    
    /**
     * Ands a list of predicates.
     */
    public static class AndHighlightPredicate implements HighlightPredicate {
        
        private List<HighlightPredicate> predicate;
        
        /**
         * Instantiates a predicate which ands all given predicates.
         * @param predicate zero or more not null predicates to and
         * @throws NullPointerException if the predicate is null
         */
        public AndHighlightPredicate(HighlightPredicate... predicate) {
            this.predicate = Arrays.asList(Contract.asNotNull(predicate, "predicate must not be null"));
        }
        
        /**
         * Instantiates a predicate which ANDs all contained predicates.
         * @param list a collection with zero or more not null predicates to AND
         * @throws NullPointerException if the collection is null
         */
        public AndHighlightPredicate(Collection<HighlightPredicate> list) {
            this.predicate = new ArrayList<HighlightPredicate>(Contract.asNotNull(list, "predicate list must not be null"));
        }

        /**
         * {@inheritDoc}
         * Implemented to return false if any of the contained predicates is
         * false or if there are no predicates.
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            for (HighlightPredicate hp : predicate) {
                if (!hp.isHighlighted(renderer, adapter)) return false;
            }
            return !predicate.isEmpty();
        }
        
    }
    
    /**
     * Or's a list of predicates.
     */
    public static class OrHighlightPredicate implements HighlightPredicate {
        
        private List<HighlightPredicate> predicate;
        
        /**
         * Instantiates a predicate which ORs all given predicates.
         * @param predicate zero or more not null predicates to OR
         * @throws NullPointerException if the predicate is null
         */
        public OrHighlightPredicate(HighlightPredicate... predicate) {
            this.predicate = Arrays.asList(Contract.asNotNull(predicate, "predicate must not be null"));
        }
        
        /**
         * Instantiates a predicate which ORs all contained predicates.
         * @param list a collection with zero or more not null predicates to OR
         * @throws NullPointerException if the collection is null
         */
        public OrHighlightPredicate(Collection<HighlightPredicate> list) {
            this.predicate = new ArrayList<HighlightPredicate>(Contract.asNotNull(list, "predicate list must not be null"));
        }

        /**
         * {@inheritDoc}
         * Implemented to return true if any of the contained predicates is
         * true.
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            for (HighlightPredicate hp : predicate) {
                if (hp.isHighlighted(renderer, adapter)) return true;
            }
            return false;
        }
        
    }
    
//------------------------ coordinates
    
    public static class RowGroupHighlightPredicate implements HighlightPredicate {

        private int linesPerGroup;

        /**
         * Instantiates a predicate with the given grouping.
         * 
         * @param linesPerGroup number of lines constituting a group, must
         *    be > 0
         * @throws IllegalArgumentException if linesPerGroup < 1   
         */
        public RowGroupHighlightPredicate(int linesPerGroup) {
            if (linesPerGroup < 1) 
                throw new IllegalArgumentException("a group contain at least 1 row, was: " + linesPerGroup);
            this.linesPerGroup = linesPerGroup;
        }
        
        /**
         * {@inheritDoc}
         * Implemented to return true if the adapter's row falls into a 
         * odd group number.
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return (adapter.row / linesPerGroup) % 2 == 1;
        }
        
    }
    
    /**
     * A HighlightPredicate based on column index.
     * 
     */
    public static class ColumnHighlightPredicate implements HighlightPredicate {
        List<Integer> columnList;
        
        /**
         * Instantiates a predicate which returns true for the
         * given columns in model coodinates.
         * 
         * @param columns the columns to highlight in model coordinates.
         */
        public ColumnHighlightPredicate(int... columns) {
            columnList = new ArrayList<Integer>();
            for (int i = 0; i < columns.length; i++) {
                columnList.add(columns[i]);
            }
        }
        
        /**
         * {@inheritDoc}
         * 
         * This implementation returns true if the adapter's column
         * is contained in this predicates list.
         * 
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            int modelIndex = adapter.viewToModel(adapter.column);
            return columnList.contains(modelIndex);
        }
        
    }
    
    /**
     * A {@code HighlightPredicate} based on adapter depth.
     * 
     * @author Karl Schaefer
     */
    public static class DepthHighlightPredicate implements HighlightPredicate {
    	private List<Integer> depthList;
    	
        /**
         * Instantiates a predicate which returns true for the
         * given depths.
         * 
         * @param depths the depths to highlight
         */
    	public DepthHighlightPredicate(int... depths) {
    		depthList = new ArrayList<Integer>();
            for (int i = 0; i < depths.length; i++) {
                depthList.add(depths[i]);
            }
    	}
    	
        /**
         * {@inheritDoc}
         * 
         * This implementation returns true if the adapter's depth
         * is contained in this predicates list.
         * 
         */
		public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            int depth = adapter.getDepth();
            return depthList.contains(depth);
		}
    	
    }
    
    //--------------------- value testing
    
    /**
     * Predicate testing the componentAdapter value against a fixed
     * Object. 
     */
    public static class EqualsHighlightPredicate implements HighlightPredicate {

        private Object compareValue;
        
        /**
         * Instantitates a predicate with null compare value.
         *
         */
        public EqualsHighlightPredicate() {
            this(null);
        }
        /**
         * Instantitates a predicate with the given compare value.
         * @param compareValue the fixed value to compare the 
         *   adapter against.
         */
        public EqualsHighlightPredicate(Object compareValue) {
            this.compareValue = compareValue;
        }
        
        /**
         * @inheritDoc
         * 
         * Implemented to return true if the adapter value equals the 
         * this predicate's compare value.
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            if (compareValue == null) return adapter.getValue() == null;
            return compareValue.equals(adapter.getValue());
        }
        
    }

    /**
     * Predicate testing the componentAdapter value type against a given
     * Clazz. 
     */
    public static class TypeHighlightPredicate implements HighlightPredicate {

        private Class clazz;
        
        /**
         * Instantitates a predicate with Object.clazz. This is essentially the
         * same as testing against null.
         *
         */
        public TypeHighlightPredicate() {
            this(Object.class);
        }
        /**
         * Instantitates a predicate with the given compare class.
         * @param compareValue the fixed class to compare the 
         *   adapter value against.
         */
        public TypeHighlightPredicate(Class compareValue) {
            this.clazz = compareValue;
        }
        
        /**
         * @inheritDoc
         * 
         * Implemented to return true if the adapter value is an instance
         * of this predicate's class type.
         */
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return adapter.getValue() != null ? 
                    clazz.isAssignableFrom(adapter.getValue().getClass()) : false;
        }
        
    }

    /**
     * Predicate testing the componentAdapter column type against a given
     * Clazz. Would be nice-to-have - but can't because the ComponentAdapter doesn't
     * expose the columnClass. Should it?
     */
    // @KEEP
//    public static class ColumnTypeHighlightPredicate implements HighlightPredicate {
//
//        private Class clazz;
//        
//        /**
//         * Instantitates a predicate with Object.clazz. This is essentially the
//         * same as testing against null.
//         *
//         */
//        public ColumnTypeHighlightPredicate() {
//            this(Object.class);
//        }
//        /**
//         * Instantitates a predicate with the given compare class.
//         * @param compareValue the fixed class to compare the 
//         *   adapter value against.
//         */
//        public ColumnTypeHighlightPredicate(Class compareValue) {
//            this.clazz = compareValue;
//        }
//        
//        /**
//         * @inheritDoc
//         * 
//         * Implemented to return true if the adapter value is an instance
//         * of this predicate's class type.
//         */
//        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
//            int modelColumn = adapter.viewToModel(columnIndex)
//            return clazz.isAssignableFrom(adapter.getColumnClass(columnIndex));
//        }
//        
//    }
}
