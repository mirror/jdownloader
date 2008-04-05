/*
 * $Id: JXSearchPanel.java,v 1.15 2008/02/18 13:28:23 kleopatra Exp $
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.jdesktop.swingx.decorator.PatternFilter;
import org.jdesktop.swingx.decorator.PatternMatcher;

/**
 * Rudimentary search panel.
 * 
 * Updates PatternMatchers from user input.
 * 
 * Supports
 * 
 * <ol>
 * <li> text input to match
 * <li> match rules like contains/equals/...
 * <li> toggle case sensitive match
 * </ol>
 * 
 * NOTE: the method setPatternHighlighter(PatternHighlighter) has been removed
 * without having been marked deprecated (has been an oversight on my part ;-).
 * To replace, implement a custom PatternMatcher which has a Highlighter, takes
 * a pattern and configures the Highlighter with the appropriate
 * PatternPredicate.
 * 
 * <pre><code>
 * public class PatternHandler implements PatternMatcher {
 * 
 *     private Highlighter highlighter;
 *     private Pattern pattern;
 * 
 *     public void setPattern(Pattern pattern) {
 *         this.pattern = pattern;
 *         highlighter.setHighlightPredicate(new PatternPredicate(pattern));
 *     }
 * 
 * }
 * </code></pre>
 * 
 * TODO: allow custom PatternModel and/or access to configuration of bound
 * PatternModel.
 * 
 * TODO: fully support control of multiple PatternMatchers.
 * 
 * @author Ramesh Gupta
 * @author Jeanette Winzenburg
 * 
 */
public class JXSearchPanel extends AbstractPatternPanel {


    public static final String MATCH_RULE_ACTION_COMMAND = "selectMatchRule";

    private JComboBox searchCriteria;

    private List<PatternMatcher> patternMatchers;
    

    public JXSearchPanel() {
        initComponents();
        build();
        initActions();
        bind();
        getPatternModel().setIncremental(true);
    }

//----------------- accessing public properties

    public void addPatternMatcher(PatternMatcher matcher) {
        getPatternMatchers().add(matcher);
        updateFieldName(matcher);
    }
    
    /**
     * sets the PatternFilter control.
     * 
     * PENDING: change to do a addPatternMatcher to enable multiple control.
     * 
     */
    public void setPatternFilter(PatternFilter filter) {
        getPatternMatchers().add(filter);
        updateFieldName(filter);
    }


    /**
     * set the label of the search combo.
     * 
     * @param name
     */
    public void setFieldName(String name) {
        searchLabel.setText(name);
    }

    /**
     * returns the label of the search combo.
     * 
     */
    public String getFieldName() {
        return searchLabel.getText();
    }

    /**
     * returns the current compiled Pattern.
     * 
     * @return the current compiled <code>Pattern</code>
     */
    public Pattern getPattern() {
        return patternModel.getPattern();
    }

    /**
     * @param matcher
     */
    protected void updateFieldName(PatternMatcher matcher) {
        
        if (matcher instanceof PatternFilter) {
            PatternFilter filter = (PatternFilter) matcher;
            searchLabel.setText(filter.getColumnName());
        } else {
            if (searchLabel.getText().length() == 0) { // ugly hack
                searchLabel.setText("Field");
                /** TODO: Remove this hack!!! */
            }
        }
    }

    // ---------------- action callbacks

    /**
     * 
     */
    @Override
    public void match() {
        for (Iterator<PatternMatcher> iter = getPatternMatchers().iterator(); iter.hasNext();) {
            iter.next().setPattern(getPattern());
            
        }
    }

    /**
     * set's the PatternModel's MatchRule to the selected in combo. 
     * 
     * NOTE: this
     * is public as an implementation side-effect! 
     * No need to ever call directly.
     */
    public void updateMatchRule() {
        getPatternModel().setMatchRule(
                (String) searchCriteria.getSelectedItem());
    }

    private List<PatternMatcher> getPatternMatchers() {
        if (patternMatchers == null) {
            patternMatchers = new ArrayList<PatternMatcher>();
        }
        return patternMatchers;
    }

    //---------------- init actions and model
    
    @Override
    protected void initExecutables() {
        super.initExecutables();
        getActionMap().put(MATCH_RULE_ACTION_COMMAND,
                createBoundAction(MATCH_RULE_ACTION_COMMAND, "updateMatchRule"));
    }


    //--------------------- binding support
    


    /**
     * bind the components to the patternModel/actions.
     */
    @Override
    protected void bind() {
        super.bind();
        List matchRules = getPatternModel().getMatchRules();
        // PENDING: map rules to localized strings
        ComboBoxModel model = new DefaultComboBoxModel(matchRules.toArray());
        model.setSelectedItem(getPatternModel().getMatchRule());
        searchCriteria.setModel(model);
        searchCriteria.setAction(getAction(MATCH_RULE_ACTION_COMMAND));
        
    }
    


    //------------------------ init ui
    
    /**
     * build container by adding all components.
     * PRE: all components created.
     */
    private void build() {
        add(searchLabel);
        add(searchCriteria);
        add(searchField);
        add(matchCheck);
    }

    /**
     * create contained components.
     * 
     *
     */
    @Override
    protected void initComponents() {
        super.initComponents();
        searchCriteria = new JComboBox();
    }


}
