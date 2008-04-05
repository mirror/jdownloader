/*
 * $Id: AbstractSearchable.java,v 1.4 2007/04/04 07:48:53 kleopatra Exp $
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

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An abstract implementation of Searchable supporting
 * incremental search.
 * 
 * Keeps internal state to represent the previous search result.
 * For all methods taking a string as parameter: compiles the String 
 * to a Pattern as-is and routes to the central method taking a Pattern.
 * 
 * 
 * @author Jeanette Winzenburg
 */
public abstract class AbstractSearchable implements Searchable {
    /**
     * a constant representing not-found state.
     */
    public static final SearchResult NO_MATCH = new SearchResult();

    /**
     * stores the result of the previous search.
     */
    protected SearchResult lastSearchResult = new SearchResult();
    

    /** key for client property to use SearchHighlighter as match marker. */
    public static final String MATCH_HIGHLIGHTER = "match.highlighter";

    /**
     * Performs a forward search starting at the beginning 
     * across the Searchable using String that represents a
     * regex pattern; {@link java.util.regex.Pattern}. 
     * @param searchString <code>String</code> that we will try to locate
     * @return the position of the match in appropriate coordinates or -1 if
     *   no match found.
     */
    public int search(String searchString) {
        return search(searchString, -1);
    }

    /**
     * Performs a forward search starting at the given startIndex
     * using String that represents a regex
     * pattern; {@link java.util.regex.Pattern}. 
     * @param searchString <code>String</code> that we will try to locate
     * @param startIndex position in the document in the appropriate coordinates
     * from which we will start search or -1 to start from the beginning
     * @return the position of the match in appropriate coordinates or -1 if
     *   no match found.
     */
    public int search(String searchString, int startIndex) {
        return search(searchString, startIndex, false);
    }

    /**
     * Performs a  search starting at the given startIndex
     * using String that represents a regex
     * pattern; {@link java.util.regex.Pattern}. The search direction 
     * depends on the boolean parameter: forward/backward if false/true, respectively.
     * @param searchString <code>String</code> that we will try to locate
     * @param startIndex position in the document in the appropriate coordinates
     * from which we will start search or -1 to start from the beginning
     * @param backward <code>true</code> if we should perform search towards the beginning
     * @return the position of the match in appropriate coordinates or -1 if
     *   no match found.
     */
    public int search(String searchString, int startIndex, boolean backward) {
        Pattern pattern = null;
        if (!isEmpty(searchString)) {
            pattern = Pattern.compile(searchString, 0);
        }
        return search(pattern, startIndex, backward);
    }

    /**
     * Performs a forward search starting at the beginning 
     * across the Searchable using the pattern; {@link java.util.regex.Pattern}. 
     * @param pattern <code>Pattern</code> that we will try to locate
     * @return the position of the match in appropriate coordinates or -1 if
     *   no match found.
     */
    public int search(Pattern pattern) {
        return search(pattern, -1);
    }

    /**
     * Performs a forward search starting at the given startIndex
     * using the Pattern; {@link java.util.regex.Pattern}. 
     *
     * @param pattern <code>Pattern</code> that we will try to locate
     * @param startIndex position in the document in the appropriate coordinates
     * from which we will start search or -1 to start from the beginning
     * @return the position of the match in appropriate coordinates or -1 if
     *   no match found.
     */
    public int search(Pattern pattern, int startIndex) {
        return search(pattern, startIndex, false);
    }

    /**
     * Performs a  search starting at the given startIndex
     * using the pattern; {@link java.util.regex.Pattern}. 
     * The search direction depends on the boolean parameter: 
     * forward/backward if false/true, respectively.
     * 
     * Updates visible and internal search state.
     * 
     * @param pattern <code>Pattern</code> that we will try to locate
     * @param startIndex position in the document in the appropriate coordinates
     * from which we will start search or -1 to start from the beginning
     * @param backwards <code>true</code> if we should perform search towards the beginning
     * @return the position of the match in appropriate coordinates or -1 if
     *   no match found.
     */
    public int search(Pattern pattern, int startIndex, boolean backwards) {
        int matchingRow = doSearch(pattern, startIndex, backwards);
        moveMatchMarker();
        return matchingRow;
    }

    /**
     * Performs a  search starting at the given startIndex
     * using the pattern; {@link java.util.regex.Pattern}. 
     * The search direction depends on the boolean parameter: 
     * forward/backward if false/true, respectively.
     * 
     * Updates internal search state.
     * 
     * @param pattern <code>Pattern</code> that we will try to locate
     * @param startIndex position in the document in the appropriate coordinates
     * from which we will start search or -1 to start from the beginning
     * @param backwards <code>true</code> if we should perform search towards the beginning
     * @return the position of the match in appropriate coordinates or -1 if
     *   no match found.
     */
    protected int doSearch(Pattern pattern, final int startIndex, boolean backwards) {
        if (isTrivialNoMatch(pattern, startIndex)) {
            updateState(null);
            return lastSearchResult.foundRow;
        }
        
        int startRow;
        if (isEqualStartIndex(startIndex)) { // implies: the last found coordinates are valid
            if (!isEqualPattern(pattern)) {
               SearchResult searchResult = findExtendedMatch(pattern, startIndex);
               if (searchResult != null) {
                   updateState(searchResult);
                   return lastSearchResult.foundRow;
               }

            }
            // didn't find a match, make sure to move the startPosition
            // for looking for the next/previous match
            startRow = moveStartPosition(startIndex, backwards);
            
        } else { 
            // startIndex is different from last search, reset the column to -1
            // and make sure a -1 startIndex is mapped to first/last row, respectively.
            startRow = adjustStartPosition(startIndex, backwards); 
        }
        findMatchAndUpdateState(pattern, startRow, backwards);
        return lastSearchResult.foundRow;
    }

    /**
     * Loops through the searchable until a match is found or the 
     * end is reached. Updates internal search state.
     *
     * @param pattern <code>Pattern</code> that we will try to locate
     * @param startRow position in the document in the appropriate coordinates
     * from which we will start search or -1 to start from the beginning
     * @param backwards <code>true</code> if we should perform search towards the beginning
     */
    protected abstract void findMatchAndUpdateState(Pattern pattern, int startRow, boolean backwards);

    /**
     * Checks and returns if it can be trivially decided to not match.
     * Here: pattern is null or startIndex exceeds the upper size limit.
     * 
     * @param pattern <code>Pattern</code> that we will try to locate
     * @param startIndex position in the document in the appropriate coordinates
     * from which we will start search or -1 to start from the beginning
     * @return true if we can say ahead that no match will be found with given search criteria
     */
    protected boolean isTrivialNoMatch(Pattern pattern, final int startIndex) {
        return (pattern == null) || (startIndex >= getSize());
    }

    /**
     * Called if <code>startIndex</code> is different from last search
     * and make sure a backwards/forwards search starts at last/first row,
     * respectively.
     * @param startIndex position in the document in the appropriate coordinates
     * from which we will start search or -1 to start from the beginning
     * @param backwards <code>true</code> if we should perform search from towards the beginning
     * @return adjusted <code>startIndex</code>
     */
    protected int adjustStartPosition(int startIndex, boolean backwards) {
        if (startIndex < 0) {
            if (backwards) {
                return getSize() - 1;
            } else {
                return 0;
            }
        }
        return startIndex;
    }

    /**
     * Moves the internal start position for matching as appropriate and returns
     * the new startIndex to use.
     * Called if search was messaged with the same startIndex as previously.
     * 
     * @param startIndex position in the document in the appropriate coordinates
     * from which we will start search or -1 to start from the beginning
     * @param backwards <code>true</code> if we should perform search towards the beginning
     * @return adjusted <code>startIndex</code>
     */
    protected int moveStartPosition(int startIndex, boolean backwards) {
        if (backwards) {
                   startIndex--;
           } else {
                   startIndex++;
           }
        return startIndex;
    }
    

    /**
     * Checks if the given Pattern should be considered as the same as 
     * in a previous search.
     * 
     * Here: compares the patterns' regex.
     * 
     * @param pattern <code>Pattern</code> that we will compare with last request
     * @return if provided <code>Pattern</code> is the same as the stored from 
     * the previous search attempt
     */
    protected boolean isEqualPattern(Pattern pattern) {
        return pattern.pattern().equals(lastSearchResult.getRegEx());
    }

    /**
     * Checks if the startIndex should be considered as the same as in
     * the previous search.
     * 
     * @param startIndex <code>startIndex</code> that we will compare with the index
     * stored by the previous search request
     * @return true if the startIndex should be re-matched, false if not.
     */
    protected boolean isEqualStartIndex(final int startIndex) {
        return isValidIndex(startIndex) && (startIndex == lastSearchResult.foundRow);
    }
    
    /**
     * checks if the searchString should be interpreted as empty.
     * here: returns true if string is null or has zero length.
     * 
     * @param searchString <code>String</code> that we should evaluate
     * @return true if the provided <code>String</code> should be interpreted as empty
     */
    protected boolean isEmpty(String searchString) {
        return (searchString == null) || searchString.length() == 0;
    }


    /**
     * called if sameRowIndex && !hasEqualRegEx.
     * Matches the cell at row/lastFoundColumn against the pattern.
     * PRE: lastFoundColumn valid.
     * 
     * @param pattern <code>Pattern</code> that we will try to match
     * @param row position at which we will get the value to match with the provided <code>Pattern</code>
     * @return result of the match; {@link SearchResult}
     */
    protected abstract SearchResult findExtendedMatch(Pattern pattern, int row);
 
    /**
     * Factory method to create a SearchResult from the given parameters.
     * 
     * @param matcher the matcher after a successful find. Must not be null.
     * @param row the found index
     * @param column the found column
     * @return newly created <code>SearchResult</code>
     */
    protected SearchResult createSearchResult(Matcher matcher, int row, int column) {
        return new SearchResult(matcher.pattern(), 
                matcher.toMatchResult(), row, column);
    }

   /** 
    * checks if index is in range: 0 <= index < getSize().
    * @param index possible start position that we will check for validity
    * @return <code>true</code> if given parameter is valid index
    */ 
   protected boolean isValidIndex(int index) {
        return index >= 0 && index < getSize();
    }

   /**
    * returns the size of this searchable.
    * 
    * @return size of this searchable
    */
   protected abstract int getSize();
   
    /**
     * Update inner searchable state based on provided search result
     *
     * @param searchResult <code>SearchResult</code> that represents the new state 
     *  of this <code>AbstractSearchable</code>
     */
    protected void updateState(SearchResult searchResult) {
        lastSearchResult.updateFrom(searchResult);
    }

    /**
     * Moves the match marker according to current found state.
     */
    protected abstract void moveMatchMarker();

    /**
     * A convenience class to hold search state.
     * NOTE: this is still in-flow, probably will take more responsibility/
     * or even change altogether on further factoring
     */
    public static class SearchResult {
        int foundRow;
        int foundColumn;
        MatchResult matchResult;
        Pattern pattern;

        public SearchResult() {
            reset();
        }
        
        public SearchResult(Pattern ex, MatchResult result, int row, int column) {
            pattern = ex;
            matchResult = result;
            foundRow = row;
            foundColumn = column;
        }
        
        public void updateFrom(SearchResult searchResult) {
            if (searchResult == null) {
                reset();
                return;
            }
            foundRow = searchResult.foundRow;
            foundColumn = searchResult.foundColumn;
            matchResult = searchResult.matchResult;
            pattern = searchResult.pattern;
        }

        public String getRegEx() {
            return pattern != null ? pattern.pattern() : null;
        }
      
        public void reset() {
            foundRow= -1;
            foundColumn = -1;
            matchResult = null;
            pattern = null;
        } 
        
        public int getFoundColumn() {
            return foundColumn;
        }
        
        public int getFoundRow() {
            return foundRow;
        }
        
        public MatchResult getMatchResult() {
            return matchResult;
        }
        
        public Pattern getPattern() {
            return pattern;
        }
    }
}
