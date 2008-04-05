/*
 * $Id: JXTree.java,v 1.47 2008/02/26 11:00:11 kleopatra Exp $
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

import java.applet.Applet;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.CellEditor;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.CompoundHighlighter;
import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.tree.DefaultXTreeCellEditor;


/**
 * JXTree.
 *
 * PENDING: support filtering/sorting.
 * 
 * @author Ramesh Gupta
 * @author Jeanette Winzenburg
 */
public class JXTree extends JTree {
    private static final Logger LOG = Logger.getLogger(JXTree.class.getName());
    private Method conversionMethod = null;
    private final static Class[] methodSignature = new Class[] {Object.class};
    private final static Object[] methodArgs = new Object[] {null};
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final TreePath[] EMPTY_TREEPATH_ARRAY = new TreePath[0];

    protected FilterPipeline filters;
    protected CompoundHighlighter compoundHighlighter;
    private ChangeListener highlighterChangeListener;

    private DelegatingRenderer delegatingRenderer;

    /**
     * Mouse/Motion/Listener keeping track of mouse moved in
     * cell coordinates.
     */
    private RolloverProducer rolloverProducer;

    /**
     * RolloverController: listens to cell over events and
     * repaints entered/exited rows.
     */
    private TreeRolloverController linkController;
    private boolean overwriteIcons;
    private Searchable searchable;
    
    // hacks around core focus issues around editing.
    /**
     * The propertyChangeListener responsible for terminating
     * edits if focus lost.
     */
    private CellEditorRemover editorRemover;
    /**
     * The CellEditorListener responsible to force the 
     * focus back to the tree after terminating edits.
     */
    private CellEditorListener editorListener;
    
    
    
    /**
     * Constructs a <code>JXTree</code> with a sample model. The default model
     * used by this tree defines a leaf node as any node without children.
     */
    public JXTree() {
	init();
    }

    /**
     * Constructs a <code>JXTree</code> with each element of the specified array
     * as the child of a new root node which is not displayed. By default, this
     * tree defines a leaf node as any node without children.
     *
     * This version of the constructor simply invokes the super class version
     * with the same arguments.
     *
     * @param value an array of objects that are children of the root.
     */
    public JXTree(Object[] value) {
        super(value);
	init();
    }

    /**
     * Constructs a <code>JXTree</code> with each element of the specified
     * Vector as the child of a new root node which is not displayed.
     * By default, this tree defines a leaf node as any node without children.
     *
     * This version of the constructor simply invokes the super class version
     * with the same arguments.
     *
     * @param value an Vector of objects that are children of the root.
     */
    public JXTree(Vector value) {
        super(value);
	init();
    }

    /**
     * Constructs a <code>JXTree</code> created from a Hashtable which does not
     * display with root. Each value-half of the key/value pairs in the HashTable
     * becomes a child of the new root node. By default, the tree defines a leaf
     * node as any node without children.
     *
     * This version of the constructor simply invokes the super class version
     * with the same arguments.
     *
     * @param value a Hashtable containing objects that are children of the root.
     */
    public JXTree(Hashtable value) {
        super(value);
	init();
    }

    /**
     * Constructs a <code>JXTree</code> with the specified TreeNode as its root,
     * which displays the root node. By default, the tree defines a leaf node as
     * any node without children.
     *
     * This version of the constructor simply invokes the super class version
     * with the same arguments.
     *
     * @param root root node of this tree
     */
    public JXTree(TreeNode root) {
        super(root, false);
        init();
    }

    /**
     * Constructs a <code>JXTree</code> with the specified TreeNode as its root,
     * which displays the root node and which decides whether a node is a leaf
     * node in the specified manner.
     *
     * This version of the constructor simply invokes the super class version
     * with the same arguments.
     *
     * @param root root node of this tree
     * @param asksAllowsChildren if true, only nodes that do not allow children
     * are leaf nodes; otherwise, any node without children is a leaf node;
     * @see javax.swing.tree.DefaultTreeModel#asksAllowsChildren
     */
    public JXTree(TreeNode root, boolean asksAllowsChildren) {
        super(root, asksAllowsChildren);
	init();
    }

    /**
     * Constructs an instance of <code>JXTree</code> which displays the root
     * node -- the tree is created using the specified data model.
     * 
     * This version of the constructor simply invokes the super class version
     * with the same arguments.
     * 
     * @param newModel
     *            the <code>TreeModel</code> to use as the data model
     */
    public JXTree(TreeModel newModel) {
        super(newModel);
        init();
    }

    @Override
    public void setModel(TreeModel newModel) {
        // To support delegation of convertValueToText() to the model...
        // JW: method needs to be set before calling super
        // otherwise there are size caching problems
        conversionMethod = getValueConversionMethod(newModel);
        super.setModel(newModel);
    }

    /**
     * Tries to find and return a method for Object --> to String conversion on the
     * model by reflection. Looks for a signature:
     * 
     * <pre> <code>
     *   String convertValueToText(Object);
     * </code> </pre>
     * 
     * 
     * 
     * PENDING JW: check - does this work with restricted permissions?
     * JW: widened access for testing - do test!
     * 
     * @param model the model to detect the method
     * @return the <code> Method </code> or null if the model has no method with
     *   the expected signature 
     */
    protected Method getValueConversionMethod(TreeModel model) {
        try {
            return model == null ? null : model.getClass().getMethod(
                    "convertValueToText", methodSignature);
        } catch (NoSuchMethodException ex) {
            LOG.finer("ex " + ex);
            LOG.finer("no conversionMethod in " + model.getClass());
        }
        return null;
    }

    
    @Override
    public String convertValueToText(Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
        // Delegate to model, if possible. Otherwise fall back to superclass...
        if (value != null) {
            if (conversionMethod == null) {
                return value.toString();
            } else {
                try {
                    methodArgs[0] = value;
                    return (String) conversionMethod.invoke(getModel(),
                            methodArgs);
                } catch (Exception ex) {
                    LOG.finer("ex " + ex);
                    LOG.finer("can't invoke " + conversionMethod);
                }
            }
        }
        return "";
    }

    private void init() {
        // To support delegation of convertValueToText() to the model...
        // JW: need to set again (is done in setModel, but at call
        // in super constructor the field is not yet valid)
        conversionMethod = getValueConversionMethod(getModel());
        // Issue #233-swingx: default editor not bidi-compliant 
        // manually install an enhanced TreeCellEditor which 
        // behaves slightly better in RtoL orientation.
        // Issue #231-swingx: icons lost
        // Anyway, need to install the editor manually because
        // the default install in BasicTreeUI doesn't know about
        // the DelegatingRenderer and therefore can't see
        // the DefaultTreeCellRenderer type to delegate to. 
        // As a consequence, the icons are lost in the default
        // setup.
        // JW PENDING need to mimic ui-delegate default re-set?
        // JW PENDING alternatively, cleanup and use DefaultXXTreeCellEditor in incubator
        TreeCellRenderer xRenderer = getCellRenderer();
        if (xRenderer instanceof JXTree.DelegatingRenderer) {
            TreeCellRenderer delegate = ((JXTree.DelegatingRenderer) xRenderer).getDelegateRenderer();
            if (delegate instanceof DefaultTreeCellRenderer) { 
                setCellEditor(new DefaultXTreeCellEditor(this, (DefaultTreeCellRenderer) delegate));
            }   
        }

        // Register the actions that this class can handle.
        ActionMap map = getActionMap();
        map.put("expand-all", new Actions("expand-all"));
        map.put("collapse-all", new Actions("collapse-all"));
        map.put("find", createFindAction());

        KeyStroke findStroke = SearchFactory.getInstance().getSearchAccelerator();
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(findStroke, "find");
    }

    /**
     * Listens to the model and updates the {@code expandedState} accordingly
     * when nodes are removed, or changed.
     * <p>
     * This class will expand an invisible root when a child has been added to
     * it.
     * 
     * @author Karl George Schaefer
     */
    protected class XTreeModelHandler extends TreeModelHandler {
        /**
         * {@inheritDoc}
         */
        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            TreePath path = e.getTreePath();
            
            //fixes SwingX bug #612
            if (path.getParentPath() == null && !isRootVisible() && isCollapsed(path)) {
                //should this be wrapped in SwingUtilities.invokeLater?
                expandPath(path);
            }
            
            super.treeNodesInserted(e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected TreeModelListener createTreeModelListener() {
        return new XTreeModelHandler();
    }

    /**
     * A small class which dispatches actions.
     * TODO: Is there a way that we can make this static?
     */
    private class Actions extends UIAction {
        Actions(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent evt) {
            if ("expand-all".equals(getName())) {
		expandAll();
            }
            else if ("collapse-all".equals(getName())) {
                collapseAll();
            }
        }
    }


//-------------------- search support
    
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
            searchable = new TreeSearchable();
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
    
 
    /**
     * A searchable targetting the visible rows of a JXTree.
     * 
     * PENDING: value to string conversion should behave as nextMatch (?) which
     * uses the convertValueToString().
     * 
     */
    public class TreeSearchable extends AbstractSearchable {

        @Override
        protected void findMatchAndUpdateState(Pattern pattern, int startRow,
                boolean backwards) {
            SearchResult searchResult = null;
            if (backwards) {
                for (int index = startRow; index >= 0 && searchResult == null; index--) {
                    searchResult = findMatchAt(pattern, index);
                }
            } else {
                for (int index = startRow; index < getSize()
                        && searchResult == null; index++) {
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
         * @param row
         *            a valid row index in view coordinates
         *            a valid column index in view coordinates
         * @return an appropriate <code>SearchResult</code> if matching or
         * null if no matching
         */
        protected SearchResult findMatchAt(Pattern pattern, int row) {
            TreePath path = getPathForRow(row);
            Object value = null;
            if (path != null) {
                value = path.getLastPathComponent();
            }
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
            return getRowCount();
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
            // the common behaviour (JXList, JXTable) is to not
            // move the selection if not found
            if (!hasMatch(lastSearchResult)) {
                return;
            }
            setSelectionRow(lastSearchResult.foundRow);
            scrollRowToVisible(lastSearchResult.foundRow);

        }

    }
    
    /**
     * Collapses all nodes in the tree table.
     */
    public void collapseAll() {
        for (int i = getRowCount() - 1; i >= 0 ; i--) {
            collapseRow(i);
        }
    }

    /**
     * Expands all nodes in the tree table.
     */
    public void expandAll() {
        if (getRowCount() == 0) {
            expandRoot();
        }
        for (int i = 0; i < getRowCount(); i++) {
            expandRow(i);
        }
    }

    /**
     * Expands the root path, assuming the current TreeModel has been set.
     */
    private void expandRoot() {
        TreeModel              model = getModel();
        if(model != null && model.getRoot() != null) {
            expandPath(new TreePath(model.getRoot()));
        }
    }

    /**
     * overridden to always return a not-null array 
     * (following SwingX convention).
     */
    @Override
    public int[] getSelectionRows() {
        int[] rows = super.getSelectionRows();
        return rows != null ? rows : EMPTY_INT_ARRAY; 
    }
    
    

    /**
     * overridden to always return a not-null array 
     * (following SwingX convention).
     */
    @Override
    public TreePath[] getSelectionPaths() {
        // TODO Auto-generated method stub
        TreePath[] paths = super.getSelectionPaths();
        return paths != null ? paths : EMPTY_TREEPATH_ARRAY; 
    }

//----------------------- Highlighter api
    
    /**
     * Returns the CompoundHighlighter assigned to the table, null if none.
     * PENDING: open up for subclasses again?.
     * 
     * @return the CompoundHighlighter assigned to the table.
     * @see #setCompoundHighlighter(CompoundHighlighter)
     */
    private CompoundHighlighter getCompoundHighlighter() {
        return compoundHighlighter;
    }

    /**
     * Assigns a CompoundHighlighter to the tree, maybe null to remove all
     * Highlighters.<p>
     * 
     * The default value is <code>null</code>. <p>
     * 
     * PENDING: open up for subclasses again?.
     * @param pipeline the CompoundHighlighter to use for renderer decoration. 
     * @see #getCompoundHighlighter()
     * @see #addHighlighter(Highlighter)
     * @see #removeHighlighter(Highlighter)
     * 
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
     * Sets the <code>Highlighter</code>s to the table, replacing any old settings.
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
     * Returns the <code>Highlighter</code>s used by this tree.
     * Maybe empty, but guarantees to be never null.
     * @return the Highlighters used by this tree, guaranteed to never null.
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
           setCompoundHighlighter(new CompoundHighlighter(new Highlighter[] {highlighter})); 
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
    

    private ChangeListener getHighlighterChangeListener() {
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
     * Property to enable/disable rollover support. This can be enabled
     * to show "live" rollover behaviour, f.i. the cursor over LinkModel cells. 
     * Default is disabled.
     * @param rolloverEnabled
     */
    public void setRolloverEnabled(boolean rolloverEnabled) {
        boolean old = isRolloverEnabled();
        if (rolloverEnabled == old) return;
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

    protected TreeRolloverController getLinkController() {
        if (linkController == null) {
            linkController = createLinkController();
        }
        return linkController;
    }

    protected TreeRolloverController createLinkController() {
        return new TreeRolloverController();
    }

    /**
     * creates and returns the RolloverProducer to use with this tree.
     * A "hit" for rollover is covering the total width of the tree.
     * Additionally, a pressed to the right (but outside of the label bounds)
     * is re-dispatched as a pressed just inside the label bounds. This 
     * is a first go for #166-swingx.
     * 
     * @return <code>RolloverProducer</code> to use with this tree
     */
    protected RolloverProducer createRolloverProducer() {
        return new RolloverProducer() {
            @Override
            public void mousePressed(MouseEvent e) {
                JXTree tree = (JXTree) e.getComponent();
                Point mousePoint = e.getPoint();
              int labelRow = tree.getRowForLocation(mousePoint.x, mousePoint.y);
              // default selection
              if (labelRow >= 0) return;
              int row = tree.getClosestRowForLocation(mousePoint.x, mousePoint.y);
              Rectangle bounds = tree.getRowBounds(row);
              if (bounds == null) {
                  row = -1;
              } else {
                  if ((bounds.y + bounds.height < mousePoint.y) || 
                       bounds.x > mousePoint.x)   {
                      row = -1;
                  }
              }
              // no hit
              if (row < 0) return;
              tree.dispatchEvent(new MouseEvent(tree, e.getID(), e.getWhen(), 
                      e.getModifiers(), bounds.x + bounds.width - 2, mousePoint.y,
                      e.getClickCount(), e.isPopupTrigger(), e.getButton()));
            }

            @Override
            protected void updateRolloverPoint(JComponent component,
                    Point mousePoint) {
                JXTree tree = (JXTree) component;
                int row = tree.getClosestRowForLocation(mousePoint.x, mousePoint.y);
                Rectangle bounds = tree.getRowBounds(row);
                if (bounds == null) {
                    row = -1;
                } else {
                    if ((bounds.y + bounds.height < mousePoint.y) || 
                            bounds.x > mousePoint.x)   {
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
     * TODO: Why doesn't this just return rolloverEnabled???
     *
     * @return if rollober is enabled.
     */
    public boolean isRolloverEnabled() {
        return rolloverProducer != null;
    }


    /**
     * listens to rollover properties. 
     * Repaints effected component regions.
     * Updates link cursor.
     * 
     * @author Jeanette Winzenburg
     */
    public  class TreeRolloverController<T extends JTree>  extends RolloverController<T> {
    
        private Cursor oldCursor;
        
//    -------------------------------------JTree rollover
        
        @Override
        protected void rollover(Point oldLocation, Point newLocation) {
            // JW: conditional repaint not working?
//            component.repaint();
            if (oldLocation != null) {
                Rectangle r = component.getRowBounds(oldLocation.y);
                if (r != null) {
                    r.x = 0;
                    r.width = component.getWidth();
                    component.repaint(r);
                }
            }
            if (newLocation != null) {
                Rectangle r = component.getRowBounds(newLocation.y);
                if (r != null) {
                    r.x = 0;
                    r.width = component.getWidth();
                    component.repaint(r);
                }
            }
            setRolloverCursor(newLocation);
        }


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

        }


        @Override
        protected RolloverRenderer getRolloverRenderer(Point location, boolean prepare) {
            TreeCellRenderer renderer = component.getCellRenderer();
            RolloverRenderer rollover = renderer instanceof RolloverRenderer 
                ? (RolloverRenderer) renderer : null;
            if ((rollover != null) && !rollover.isEnabled()) {
                rollover = null;
            }
            if ((rollover != null) && prepare) {
                TreePath path = component.getPathForRow(location.y);
                Object element = path != null ? path.getLastPathComponent() : null;
                renderer.getTreeCellRendererComponent(component, element, false, 
                        false, false, 
                        location.y, false);
            }
            return rollover;
        }


        @Override
        protected Point getFocusedCell() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    /**
     * Returns the string representation of the cell value at the given position. 
     * 
     * @param row the row index of the cell in view coordinates
     * @return the string representation of the cell value as it will appear in the 
     *   table. 
     */
    public String getStringAt(int row) {
        return getStringAt(getPathForRow(row));
    }

    /**
     * Returns the string representation of the cell value at the given position. 
     * 
     * @param path the TreePath representing the node.
     * @return the string representation of the cell value as it will appear in the 
     *   table, or null if the path is not visible. 
     */
    public String getStringAt(TreePath path) {
        if (path == null) return null;
        TreeCellRenderer renderer = getDelegatingRenderer().getDelegateRenderer();
        if (renderer instanceof StringValue) {
            return ((StringValue) renderer).getString(path.getLastPathComponent());
        }
        return StringValue.TO_STRING.getString(path.getLastPathComponent());
    }

    
    private DelegatingRenderer getDelegatingRenderer() {
        if (delegatingRenderer == null) {
            // only called once... to get hold of the default?
            delegatingRenderer = new DelegatingRenderer();
            delegatingRenderer.setDelegateRenderer(super.getCellRenderer());
        }
        return delegatingRenderer;
    }


    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to return the DelegateRenderer which
     * is wrapped around the actual renderer. 
     */
    @Override
    public TreeCellRenderer getCellRenderer() {
        return getDelegatingRenderer();
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to wrap the given renderer in a DelegateRenderer.
     */
    @Override
    public void setCellRenderer(TreeCellRenderer renderer) {
        // PENDING: do something against recursive setting
        // == multiple delegation...
        getDelegatingRenderer().setDelegateRenderer(renderer);
        super.setCellRenderer(delegatingRenderer);
    }

    /**
     * Sets the Icon to use for the handle of an expanded node.<p>
     * 
     * Note: this will only succeed if the current ui delegate is
     * a BasicTreeUI otherwise it will do nothing.<p>
     * 
     * PENDING JW: incomplete api (no getter) and not a bound property.
     * 
     * @param expandedIcon the Icon to use for the handle of an expanded node.
     */
    public void setExpandedIcon(Icon expandedIcon) {
        if (getUI() instanceof BasicTreeUI) {
            ((BasicTreeUI) getUI()).setExpandedIcon(expandedIcon);
        }
    }
    
    /**
     * Sets the Icon to use for the handle of a collapsed node.
     * 
     * Note: this will only succeed if the current ui delegate is
     * a BasicTreeUI otherwise it will do nothing.
     *  
     * PENDING JW: incomplete api (no getter) and not a bound property.
     * 
     * @param collapsedIcon the Icon to use for the handle of a collapsed node.
     */
    public void setCollapsedIcon(Icon collapsedIcon) {
        if (getUI() instanceof BasicTreeUI) {
            ((BasicTreeUI) getUI()).setCollapsedIcon(collapsedIcon);
        }
    }
    
    /**
     * Sets the Icon to use for a leaf node.<p>
     * 
     * Note: this will only succeed if current renderer is a 
     * DefaultTreeCellRenderer.<p>
     * 
     * PENDING JW: this (all setXXIcon) is old api pulled up from the JXTreeTable. 
     * Need to review if we really want it - problematic if sharing the same
     * renderer instance across different trees.
     * 
     * PENDING JW: incomplete api (no getter) and not a bound property.<p>
     * 
     * @param leafIcon the Icon to use for a leaf node.
     */
    public void setLeafIcon(Icon leafIcon) {
        getDelegatingRenderer().setLeafIcon(leafIcon);
    }
    
    /**
     * Sets the Icon to use for an open folder node.
     * 
     * Note: this will only succeed if current renderer is a 
     * DefaultTreeCellRenderer.
     * 
     * PENDING JW: incomplete api (no getter) and not a bound property.
     * 
     * @param openIcon the Icon to use for an open folder node.
     */
    public void setOpenIcon(Icon openIcon) {
        getDelegatingRenderer().setOpenIcon(openIcon);
    }
    
    /**
     * Sets the Icon to use for a closed folder node.
     * 
     * Note: this will only succeed if current renderer is a 
     * DefaultTreeCellRenderer.
     * 
     * PENDING JW: incomplete api (no getter) and not a bound property.
     * 
     * @param closedIcon the Icon to use for a closed folder node.
     */
    public void setClosedIcon(Icon closedIcon) {
        getDelegatingRenderer().setClosedIcon(closedIcon);
    }
    
    /**
     * Property to control whether per-tree icons should be 
     * copied to the renderer on setCellRenderer. <p>
     * 
     * The default value is false.
     * 
     * PENDING: should update the current renderer's icons when 
     * setting to true?
     * 
     * @param overwrite a boolean to indicate if the per-tree Icons should
     *   be copied to the new renderer on setCellRenderer.
     * 
     * @see #isOverwriteRendererIcons()  
     * @see #setLeafIcon(Icon)
     * @see #setOpenIcon(Icon)
     * @see #setClosedIcon(Icon)  
     */
    public void setOverwriteRendererIcons(boolean overwrite) {
        if (overwriteIcons == overwrite) return;
        boolean old = overwriteIcons;
        this.overwriteIcons = overwrite;
        firePropertyChange("overwriteRendererIcons", old, overwrite);
    }

    /**
     * Returns a boolean indicating whether the per-tree icons should be 
     * copied to the renderer on setCellRenderer.
     * 
     * @return true if a TreeCellRenderer's icons will be overwritten with the
     *   tree's Icons, false if the renderer's icons will be unchanged.
     *   
     * @see #setOverwriteRendererIcons(boolean)
     * @see #setLeafIcon(Icon)
     * @see #setOpenIcon(Icon)
     * @see #setClosedIcon(Icon)  
     *     
     */
    public boolean isOverwriteRendererIcons() {
        return overwriteIcons;
    }
    
    public class DelegatingRenderer implements TreeCellRenderer, RolloverRenderer {
        private Icon    closedIcon = null;
        private Icon    openIcon = null;
        private Icon    leafIcon = null;
       
        private TreeCellRenderer delegate;
        
        public DelegatingRenderer() {
            initIcons(new DefaultTreeCellRenderer());
        }

        /**
         * initially sets the icons to the defaults as given
         * by a DefaultTreeCellRenderer.
         * 
         * @param renderer
         */
        private void initIcons(DefaultTreeCellRenderer renderer) {
            closedIcon = renderer.getDefaultClosedIcon();
            openIcon = renderer.getDefaultOpenIcon();
            leafIcon = renderer.getDefaultLeafIcon();
        }

        /**
         * Set the delegate renderer. 
         * Updates the folder/leaf icons. 
         * 
         * THINK: how to update? always override with this.icons, only
         * if renderer's icons are null, update this icons if they are not,
         * update all if only one is != null.... ??
         * 
         * @param delegate
         */
        public void setDelegateRenderer(TreeCellRenderer delegate) {
            if (delegate == null) {
                delegate = new DefaultTreeCellRenderer();
            }
            this.delegate = delegate;
            updateIcons();
        }
        
        /**
         * tries to set the renderers icons. Can succeed only if the
         * delegate is a DefaultTreeCellRenderer.
         * THINK: how to update? always override with this.icons, only
         * if renderer's icons are null, update this icons if they are not,
         * update all if only one is != null.... ??
         * 
         */
        private void updateIcons() {
            if (!isOverwriteRendererIcons()) return;
            setClosedIcon(closedIcon);
            setOpenIcon(openIcon);
            setLeafIcon(leafIcon);
        }

        public void setClosedIcon(Icon closedIcon) {
            if (delegate instanceof DefaultTreeCellRenderer) {
                ((DefaultTreeCellRenderer) delegate).setClosedIcon(closedIcon);
            }
            this.closedIcon = closedIcon;
        }
        
        public void setOpenIcon(Icon openIcon) {
            if (delegate instanceof DefaultTreeCellRenderer) {
                ((DefaultTreeCellRenderer) delegate).setOpenIcon(openIcon);
            }
            this.openIcon = openIcon;
        }
        
        public void setLeafIcon(Icon leafIcon) {
            if (delegate instanceof DefaultTreeCellRenderer) {
                ((DefaultTreeCellRenderer) delegate).setLeafIcon(leafIcon);
            }
            this.leafIcon = leafIcon;
        }
        
        //--------------- TreeCellRenderer
        
        public TreeCellRenderer getDelegateRenderer() {
            return delegate;
        }
            public Component getTreeCellRendererComponent(JTree tree, Object value, 
                    boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                Component result = delegate.getTreeCellRendererComponent(tree, value, 
                        selected, expanded, leaf, row, hasFocus);

                    if ((compoundHighlighter != null) && (row < getRowCount()) && (row >= 0)){
                        result = compoundHighlighter.highlight(result, getComponentAdapter(row));
                    }

                 return result;
            }
            
            //------------------ RolloverRenderer
            
            public boolean isEnabled() {
                return (delegate instanceof RolloverRenderer) && 
                   ((RolloverRenderer) delegate).isEnabled();
            }
            
            public void doClick() {
                if (isEnabled()) {
                    ((RolloverRenderer) delegate).doClick();
                }
            }

    }

    
//----------------------- edit
    
    /**
     * {@inheritDoc} <p>
     * Overridden to fix focus issues with editors. 
     * This method installs and updates the internal CellEditorRemover which
     * to terminates ongoing edits if appropriate. Additionally, it
     * registers a CellEditorListener with the cell editor to grab the 
     * focus back to tree, if appropriate.
     * 
     * @see #updateEditorRemover()
     */
    @Override
    public void startEditingAtPath(TreePath path) {
        super.startEditingAtPath(path);
        if (isEditing()) {
            updateEditorListener();
            updateEditorRemover();
        }
    }

    
    /**
     * Hack to grab focus after editing.
     */
    private void updateEditorListener() {
        if (editorListener == null) {
            editorListener = new CellEditorListener() {

                public void editingCanceled(ChangeEvent e) {
                    terminated(e);
                }

                /**
                 * @param e
                 */
                private void terminated(ChangeEvent e) {
                    analyseFocus();
                    ((CellEditor) e.getSource()).removeCellEditorListener(editorListener);
                }

                public void editingStopped(ChangeEvent e) {
                    terminated(e);
                }
                
            };
        }
        getCellEditor().addCellEditorListener(editorListener);

    }

    /**
     * This is called from cell editor listener if edit terminated.
     * Trying to analyse if we should grab the focus back to the
     * tree after. Brittle ... we assume we are the first to 
     * get the event, so we can analyse the hierarchy before the
     * editing component is removed.
     */
    protected void analyseFocus() {
        final boolean isFocusOwnerInTheTable = isFocusOwnerDescending();    
        if (isFocusOwnerInTheTable) {
            requestFocusInWindow();
        }
    }


    /**
     * Returns a boolean to indicate if the current focus owner 
     * is descending from this table. 
     * Returns false if not editing, otherwise walks the focusOwner
     * hierarchy, taking popups into account. <p>
     * 
     * PENDING: copied from JXTable ... should be somewhere in a utility
     * class?
     * 
     * @return a boolean to indicate if the current focus
     *   owner is contained.
     */
    private boolean isFocusOwnerDescending() {
        if (!isEditing()) return false;
        Component focusOwner = 
            KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        // PENDING JW: special casing to not fall through ... really wanted?
        if (focusOwner == null) return false;
        if (isDescending(focusOwner)) return true;
        // same with permanent focus owner
        Component permanent = 
            KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        return isDescending(permanent);
    }

    /**
     * PENDING: copied from JXTable ... should be somewhere in a utility
     * class?
     * 
     * @param focusOwner
     * @return
     */
    private boolean isDescending(Component focusOwner) {
        while (focusOwner !=  null) {
            if (focusOwner instanceof JPopupMenu) {
                focusOwner = ((JPopupMenu) focusOwner).getInvoker();
                if (focusOwner == null) {
                    return false;
                }
            }
            if (focusOwner == this) {
                return true;
            }
            focusOwner = focusOwner.getParent();
        }
        return false;
    }


    /**
     * Overridden to release the CellEditorRemover, if any.
     */
    @Override
    public void removeNotify() {
        if (editorRemover != null) {
            editorRemover.release();
            editorRemover = null;
        }
        super.removeNotify();
    }

    /**
     * Lazily creates and updates the internal CellEditorRemover.
     * 
     *
     */
    private void updateEditorRemover() {
        if (editorRemover == null) {
            editorRemover = new CellEditorRemover();
        }
        editorRemover.updateKeyboardFocusManager();
    }

    /** This class tracks changes in the keyboard focus state. It is used
     * when the JXTree is editing to determine when to terminate the edit.
     * If focus switches to a component outside of the JXTree, but in the
     * same window, this will terminate editing. The exact terminate 
     * behaviour is controlled by the invokeStopEditing property.
     * 
     * @see javax.swing.JTree#setInvokesStopCellEditing(boolean)
     * 
     */
    public class CellEditorRemover implements PropertyChangeListener {
        /** the focusManager this is listening to. */
        KeyboardFocusManager focusManager;

        public CellEditorRemover() {
            updateKeyboardFocusManager();
        }

        /**
         * Updates itself to listen to the current KeyboardFocusManager. 
         *
         */
        public void updateKeyboardFocusManager() {
            KeyboardFocusManager current = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            setKeyboardFocusManager(current);
        }

        /**
         * stops listening.
         *
         */
        public void release() {
            setKeyboardFocusManager(null);
        }
        
        /**
         * Sets the focusManager this is listening to. 
         * Unregisters/registers itself from/to the old/new manager, 
         * respectively. 
         * 
         * @param current the KeyboardFocusManager to listen too.
         */
        private void setKeyboardFocusManager(KeyboardFocusManager current) {
            if (focusManager == current)
                return;
            KeyboardFocusManager old = focusManager;
            if (old != null) {
                old.removePropertyChangeListener("permanentFocusOwner", this);
            }
            focusManager = current;
            if (focusManager != null) {
                focusManager.addPropertyChangeListener("permanentFocusOwner",
                        this);
            }

        }
        public void propertyChange(PropertyChangeEvent ev) {
            if (!isEditing()) {
                return;
            }

            Component c = focusManager.getPermanentFocusOwner();
            JXTree tree = JXTree.this;
            while (c != null) {
                if (c instanceof JPopupMenu) {
                    c = ((JPopupMenu) c).getInvoker();
                } else {

                    if (c == tree) {
                        // focus remains inside the table
                        return;
                    } else if ((c instanceof Window) ||
                            (c instanceof Applet && c.getParent() == null)) {
                        if (c == SwingUtilities.getRoot(tree)) {
                            if (tree.getInvokesStopCellEditing()) {
                                tree.stopEditing();
                            }
                            if (tree.isEditing()) {
                                tree.cancelEditing();
                            }
                        }
                        break;
                    }
                    c = c.getParent();
                }
            }
        }
    }

    
    /**
     * @return the unconfigured ComponentAdapter.
     */
    protected ComponentAdapter getComponentAdapter() {
        if (dataAdapter == null) {
            dataAdapter = new TreeAdapter(this);
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

    protected ComponentAdapter dataAdapter;

    protected static class TreeAdapter extends ComponentAdapter {
        private final JXTree tree;

        /**
         * Constructs a <code>TableCellRenderContext</code> for the specified
         * target component.
         *
         * @param component the target component
         */
        public TreeAdapter(JXTree component) {
            super(component);
            tree = component;
        }
        
        public JXTree getTree() {
            return tree;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasFocus() {
            return tree.isFocusOwner() && (tree.getLeadSelectionRow() == row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(int row, int column) {
            TreePath path = tree.getPathForRow(row);
            return path.getLastPathComponent();
        }

        /**
         * {@inheritDoc} <p>
         * 
         * JXTree doesn't support filtering/sorting. This implies that
         * model and view coordinates are the same. So this method is
         * implemented to call getValueAt(row, column).
         * 
         */
        @Override
        public Object getFilteredValueAt(int row, int column) {
            /** TODO: Implement filtering */
            return getValueAt(row, column);
        }
        
        
        /**
         * {@inheritDoc} <p>
         * 
         * JXTree doesn't support filtering/sorting. This implies that
         * model and view coordinates are the same. So this method is
         * implemented to call getValueAt(row, column).
         * 
         */
        @Override
        public Object getValue() {
            return getValueAt(row, column);
        }
        
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String getFilteredStringAt(int row, int column) {
            return tree.getStringAt(row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getString() {
            // TODO Auto-generated method stub
            return tree.getStringAt(row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEditable() {
            //this is not as robust as JXTable; should it be? -- kgs
            return tree.isPathEditable(tree.getPathForRow(row));
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSelected() {
            return tree.isRowSelected(row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isExpanded() {
            return tree.isExpanded(row);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getDepth() {
            return tree.getPathForRow(row).getPathCount() - 1;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isLeaf() {
            return tree.getModel().isLeaf(getValue());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;	/** TODO:  */
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValueAt(Object aValue, int row, int column) {
            /** TODO:  */
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


}
