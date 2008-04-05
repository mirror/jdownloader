/*
 * $Id: JXEditorPane.java,v 1.26 2008/02/18 15:56:23 kleopatra Exp $
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

import org.jdesktop.swingx.action.ActionManager;
import org.jdesktop.swingx.action.Targetable;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Segment;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * An extended editor pane which has the following features built in:
 * <ul>
 *   <li>Text search
 *   <li>undo/redo
 *   <li>simple html/plain text editing
 * </ul>
 *
 * @author Mark Davidson
 */
public class JXEditorPane extends JEditorPane implements /*Searchable, */Targetable {

    private static final Logger LOG = Logger.getLogger(JXEditorPane.class
            .getName());

    private UndoableEditListener undoHandler;
    private UndoManager undoManager;
    private CaretListener caretHandler;
    private JComboBox selector;

    // The ids of supported actions. Perhaps this should be public.
    private final static String ACTION_FIND = "find";
    private final static String ACTION_UNDO = "undo";
    private final static String ACTION_REDO = "redo";
    /*
     * These next 3 actions are part of a *HACK* to get cut/copy/paste
     * support working in the same way as find, undo and redo. in JTextComponent
     * the cut/copy/paste actions are _not_ added to the ActionMap. Instead,
     * a default "transfer handler" system is used, apparently to get the text
     * onto the system clipboard.
     * Since there aren't any CUT/COPY/PASTE actions in the JTextComponent's action
     * map, they cannot be referenced by the action framework the same way that
     * find/undo/redo are. So, I added the actions here. The really hacky part
     * is that by defining an Action to go along with the cut/copy/paste keys,
     * I loose the default handling in the cut/copy/paste routines. So, I have
     * to remove cut/copy/paste from the action map, call the appropriate 
     * method (cut, copy, or paste) and then add the action back into the
     * map. Yuck!
     */
    private final static String ACTION_CUT = "cut";
    private final static String ACTION_COPY = "copy";
    private final static String ACTION_PASTE = "paste";

    private TargetableSupport targetSupport = new TargetableSupport(this);
    private Searchable searchable;
    
    public JXEditorPane() {
        init();
    }

    public JXEditorPane(String url) throws IOException {
        super(url);
        init();
    }

    public JXEditorPane(String type, String text) {
        super(type, text);
        init();
    }

    public JXEditorPane(URL initialPage) throws IOException {
        super(initialPage);
        init();
    }

    private void init() {
        setEditorKitForContentType("text/html", new SloppyHTMLEditorKit());
        addPropertyChangeListener(new PropertyHandler());
        getDocument().addUndoableEditListener(getUndoableEditListener());
        initActions();
    }

    private class PropertyHandler implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            String name = evt.getPropertyName();
            if (name.equals("document")) {
                Document doc = (Document)evt.getOldValue();
                if (doc != null) {
                    doc.removeUndoableEditListener(getUndoableEditListener());
                }

                doc = (Document)evt.getNewValue();
                if (doc != null) {
                    doc.addUndoableEditListener(getUndoableEditListener());
                }
            }
        }

    }

    // pp for testing
    CaretListener getCaretListener() {
        return caretHandler;
    }

    // pp for testing
    UndoableEditListener getUndoableEditListener() {
        if (undoHandler == null) {
            undoHandler = new UndoHandler();
            undoManager = new UndoManager();
        }
        return undoHandler;
    }

    /**
     * Overidden to perform document initialization based on type.
     */
    @Override
    public void setEditorKit(EditorKit kit) {
        super.setEditorKit(kit);

        if (kit instanceof StyledEditorKit) {
            if (caretHandler == null) {
                caretHandler = new CaretHandler();
            }
            addCaretListener(caretHandler);
        }
    }

    /**
     * Register the actions that this class can handle.
     */
    protected void initActions() {
        ActionMap map = getActionMap();
        map.put(ACTION_FIND, new Actions(ACTION_FIND));
        map.put(ACTION_UNDO, new Actions(ACTION_UNDO));
        map.put(ACTION_REDO, new Actions(ACTION_REDO));
        map.put(ACTION_CUT, new Actions(ACTION_CUT));
        map.put(ACTION_COPY, new Actions(ACTION_COPY));
        map.put(ACTION_PASTE, new Actions(ACTION_PASTE));
        
        KeyStroke findStroke = SearchFactory.getInstance().getSearchAccelerator();
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(findStroke, "find");
    }

    // undo/redo implementation

    private class UndoHandler implements UndoableEditListener {
        public void undoableEditHappened(UndoableEditEvent evt) {
            undoManager.addEdit(evt.getEdit());
            updateActionState();
        }
    }

    /**
     * Updates the state of the actions in response to an undo/redo operation. <p>
     * 
     */
    private void updateActionState() {
        // Update the state of the undo and redo actions
        // JW: fiddling with actionManager's actions state? I'm pretty sure
        // we don't want that: the manager will get nuts with multiple
        // components with different state.
        // It's up to whatever manager to listen
        // to our changes and update itself accordingly. Which is not
        // well supported with the current design ... nobody 
        // really cares about enabled as it should. 
        //
        Runnable doEnabled = new Runnable() {
                public void run() {
                    ActionManager manager = ActionManager.getInstance();
                    manager.setEnabled(ACTION_UNDO, undoManager.canUndo());
                    manager.setEnabled(ACTION_REDO, undoManager.canRedo());
                }
            };
        SwingUtilities.invokeLater(doEnabled);
    }

    /**
     * A small class which dispatches actions.
     * TODO: Is there a way that we can make this static?
     * JW: these if-constructs are totally crazy ... we live in OO world!
     * 
     */
    private class Actions extends UIAction {
        Actions(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent evt) {
            String name = getName();
            if (ACTION_FIND.equals(name)) {
                find();
            }
            else if (ACTION_UNDO.equals(name)) {
                try {
                    undoManager.undo();
                } catch (CannotUndoException ex) {
                    LOG.info("Could not undo");
                }
                updateActionState();
            }
            else if (ACTION_REDO.equals(name)) {
                try {
                    undoManager.redo();
                } catch (CannotRedoException ex) {
                    LOG.info("Could not redo");
                }
                updateActionState();
            } else if (ACTION_CUT.equals(name)) {
                ActionMap map = getActionMap();
                map.remove(ACTION_CUT);
                cut();
                map.put(ACTION_CUT, this);
            } else if (ACTION_COPY.equals(name)) {
                ActionMap map = getActionMap();
                map.remove(ACTION_COPY);
                copy();
                map.put(ACTION_COPY, this);
            } else if (ACTION_PASTE.equals(name)) {
                ActionMap map = getActionMap();
                map.remove(ACTION_PASTE);
                paste();
                map.put(ACTION_PASTE, this);
            }
            else {
                LOG.fine("ActionHandled: " + name);
            }

        }

        @Override
        public boolean isEnabled(Object sender) {
                String name = getName();
                if (ACTION_UNDO.equals(name)) {
                    return isEditable() && undoManager.canUndo();
                } 
                if (ACTION_REDO.equals(name)) {
                    return isEditable() && undoManager.canRedo();
                } 
                if (ACTION_PASTE.equals(name)) {
                    if (!isEditable()) return false;
                    // is this always possible?
                    boolean dataOnClipboard = false;
                    try {
                        dataOnClipboard = getToolkit()
                        .getSystemClipboard().getContents(null) != null;
                    } catch (Exception e) {
                        // can't do anything - clipboard unaccessible
                    }
                    return dataOnClipboard;
                } 
                boolean selectedText = getSelectionEnd()
                    - getSelectionStart() > 0;
                if (ACTION_CUT.equals(name)) {
                   return isEditable() && selectedText;
                }
                if (ACTION_COPY.equals(name)) {
                    return selectedText;
                } 
                if (ACTION_FIND.equals(name)) {
                    return getDocument().getLength() > 0;
                }
                return true;
        }
        
        
    }

    /**
     * Retrieves a component which will be used as the paragraph selector.
     * This can be placed in the toolbar.
     * <p>
     * Note: This is only valid for the HTMLEditorKit
     */
    public JComboBox getParagraphSelector() {
        if (selector == null) {
            selector = new ParagraphSelector();
        }
        return selector;
    }

    /**
     * A control which should be placed in the toolbar to enable
     * paragraph selection.
     */
    private class ParagraphSelector extends JComboBox implements ItemListener {

        private Map itemMap;

        public ParagraphSelector() {

            // The item map is for rendering
            itemMap = new HashMap();
            itemMap.put(HTML.Tag.P, "Paragraph");
            itemMap.put(HTML.Tag.H1, "Heading 1");
            itemMap.put(HTML.Tag.H2, "Heading 2");
            itemMap.put(HTML.Tag.H3, "Heading 3");
            itemMap.put(HTML.Tag.H4, "Heading 4");
            itemMap.put(HTML.Tag.H5, "Heading 5");
            itemMap.put(HTML.Tag.H6, "Heading 6");
            itemMap.put(HTML.Tag.PRE, "Preformatted");

            // The list of items
            Vector items = new Vector();
            items.addElement(HTML.Tag.P);
            items.addElement(HTML.Tag.H1);
            items.addElement(HTML.Tag.H2);
            items.addElement(HTML.Tag.H3);
            items.addElement(HTML.Tag.H4);
            items.addElement(HTML.Tag.H5);
            items.addElement(HTML.Tag.H6);
            items.addElement(HTML.Tag.PRE);

            setModel(new DefaultComboBoxModel(items));
            setRenderer(new ParagraphRenderer());
            addItemListener(this);
            setFocusable(false);
        }

        public void itemStateChanged(ItemEvent evt) {
            if (evt.getStateChange() == ItemEvent.SELECTED) {
                applyTag((HTML.Tag)evt.getItem());
            }
        }

        private class ParagraphRenderer extends DefaultListCellRenderer {

            public ParagraphRenderer() {
                setOpaque(true);
            }

            @Override
            public Component getListCellRendererComponent(JList list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected,
                                                   cellHasFocus);

                setText((String)itemMap.get(value));

                return this;
            }
        }


        // TODO: Should have a rendererer which does stuff like:
        // Paragraph, Heading 1, etc...
    }

    /**
     * Applys the tag to the current selection
     */
    protected void applyTag(HTML.Tag tag) {
        Document doc = getDocument();
        if (!(doc instanceof HTMLDocument)) {
            return;
        }
        HTMLDocument hdoc = (HTMLDocument)doc;
        int start = getSelectionStart();
        int end = getSelectionEnd();

        Element element = hdoc.getParagraphElement(start);
        MutableAttributeSet newAttrs = new SimpleAttributeSet(element.getAttributes());
        newAttrs.addAttribute(StyleConstants.NameAttribute, tag);

        hdoc.setParagraphAttributes(start, end - start, newAttrs, true);
    }

    /**
     * The paste method has been overloaded to strip off the <html><body> tags
     * This doesn't really work.
     */
    @Override
    public void paste() {
        Clipboard clipboard = getToolkit().getSystemClipboard();
        Transferable content = clipboard.getContents(this);
        if (content != null) {
            DataFlavor[] flavors = content.getTransferDataFlavors();
            try {
                for (int i = 0; i < flavors.length; i++) {
                    if (String.class.equals(flavors[i].getRepresentationClass())) {
                        Object data = content.getTransferData(flavors[i]);

                        if (flavors[i].isMimeTypeEqual("text/plain")) {
                            // This works but we lose all the formatting.
                            replaceSelection(data.toString());
                            break;
                        } 
                    }
                }
            } catch (Exception ex) {
                // TODO change to something meaningful - when can this acutally happen?
                LOG.log(Level.FINE, "What can produce a problem with data flavor?", ex);
            }
        }
    }

    private void find() {
        SearchFactory.getInstance().showFindInput(this, getSearchable());
    }

    /**
     * 
     * @return a not-null Searchable for this editor.
     */
    public Searchable getSearchable() {
        if (searchable == null) {
            searchable = new DocumentSearchable();
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
    
    public class DocumentSearchable implements Searchable {
        public int search(String searchString) {
            return search(searchString, -1);
        }

        public int search(String searchString, int columnIndex) {
            return search(searchString, columnIndex, false);
        }
        
        public int search(String searchString, int columnIndex, boolean backward) {
            Pattern pattern = null;
            if (!isEmpty(searchString)) {
                pattern = Pattern.compile(searchString, 0);
            }
            return search(pattern, columnIndex, backward);
        }

        /**
         * checks if the searchString should be interpreted as empty.
         * here: returns true if string is null or has zero length.
         *
         * TODO: This should be in a utility class.
         * 
         * @param searchString
         * @return true if string is null or has zero length
         */
        protected boolean isEmpty(String searchString) {
            return (searchString == null) || searchString.length() == 0;
        }

        public int search(Pattern pattern) {
            return search(pattern, -1);
        }

        public int search(Pattern pattern, int startIndex) {
            return search(pattern, startIndex, false);
        }

        int lastFoundIndex = -1;

        MatchResult lastMatchResult;
        String lastRegEx;
        /**
         * @return start position of matching string or -1
         */
        public int search(Pattern pattern, final int startIndex,
                boolean backwards) {
            if ((pattern == null)
                    || (getDocument().getLength() == 0)
                    || ((startIndex > -1) && (getDocument().getLength() < startIndex))) {
                updateStateAfterNotFound();
                return -1;
            }

            int start = startIndex;
            if (maybeExtendedMatch(startIndex)) {
                if (foundExtendedMatch(pattern, start)) {
                    return lastFoundIndex;
                }
                start++;
            }

            int length;
            if (backwards) {
                start = 0;
                if (startIndex < 0) {
                    length = getDocument().getLength() - 1;
                } else {
                    length = -1 + startIndex;
                }
            } else {
                // start = startIndex + 1;
                if (start < 0)
                    start = 0;
                length = getDocument().getLength() - start;
            }
            Segment segment = new Segment();

            try {
                getDocument().getText(start, length, segment);
            } catch (BadLocationException ex) {
                LOG.log(Level.FINE,
                        "this should not happen (calculated the valid start/length) " , ex);
            }

            Matcher matcher = pattern.matcher(segment.toString());
            MatchResult currentResult = getMatchResult(matcher, !backwards);
            if (currentResult != null) {
                updateStateAfterFound(currentResult, start);
            } else {
                updateStateAfterNotFound();
            }
            return lastFoundIndex;

        }

        /**
         * Search from same startIndex as the previous search. 
         * Checks if the match is different from the last (either 
         * extended/reduced) at the same position. Returns true
         * if the current match result represents a different match 
         * than the last, false if no match or the same.
         * 
         * @param pattern
         * @param start
         * @return true if the current match result represents a different
         * match than the last, false if no match or the same.
         */
        private boolean foundExtendedMatch(Pattern pattern, int start) {
            // JW: logic still needs cleanup...
            if (pattern.pattern().equals(lastRegEx)) {
                return false;
            }
            int length = getDocument().getLength() - start;
            Segment segment = new Segment();

            try {
                getDocument().getText(start, length, segment);
            } catch (BadLocationException ex) {
                LOG.log(Level.FINE,
                        "this should not happen (calculated the valid start/length) " , ex);
            }
            Matcher matcher = pattern.matcher(segment.toString());
            MatchResult currentResult = getMatchResult(matcher, true);
            if (currentResult != null) {
                // JW: how to compare match results reliably?
                // the group().equals probably isn't the best idea...
                // better check pattern?
                if ((currentResult.start() == 0) && 
                   (!lastMatchResult.group().equals(currentResult.group()))) {
                    updateStateAfterFound(currentResult, start);
                    return true;
                } 
            }
            return false;
        }

        /**
         * Checks if the startIndex is a candidate for trying a re-match.
         * 
         * 
         * @param startIndex
         * @return true if the startIndex should be re-matched, false if not.
         */
        private boolean maybeExtendedMatch(final int startIndex) {
            return (startIndex >= 0) && (startIndex == lastFoundIndex);
        }

        /**
         * @param currentResult
         * @param offset
         * @return the start position of the selected text
         */
        private int updateStateAfterFound(MatchResult currentResult, final int offset) {
            int end = currentResult.end() + offset;
            int found = currentResult.start() + offset; 
            select(found, end);
            getCaret().setSelectionVisible(true);
            lastFoundIndex = found;
            lastMatchResult = currentResult;
            lastRegEx = ((Matcher) lastMatchResult).pattern().pattern();
            return found;
        }

        /**
         * @param matcher
         * @param useFirst whether or not to return after the first match is found.
         * @return <code>MatchResult</code> or null
         */
        private MatchResult getMatchResult(Matcher matcher, boolean  useFirst) {
            MatchResult currentResult = null;
            while (matcher.find()) {
                currentResult = matcher.toMatchResult();
                if (useFirst) break;
            }
            return currentResult;
        }

        /**
         */
        private void updateStateAfterNotFound() {
            lastFoundIndex = -1;
            lastMatchResult = null;
            lastRegEx = null;
            setCaretPosition(getSelectionEnd());
        }

    }
    
    public boolean hasCommand(Object command) {
        return targetSupport.hasCommand(command);
    }

    public Object[] getCommands() {
        return targetSupport.getCommands();
    }

    public boolean doCommand(Object command, Object value) {
        return targetSupport.doCommand(command, value);
    }

    /**
     * Listens to the caret placement and adjusts the editing
     * properties as appropriate.
     *
     * Should add more attributes as required.
     */
    private class CaretHandler implements CaretListener {
        public void caretUpdate(CaretEvent evt) {
            StyledDocument document = (StyledDocument)getDocument();
            int dot = evt.getDot();
            Element elem = document.getCharacterElement(dot);
            AttributeSet set = elem.getAttributes();

            // JW: see comment in updateActionState
            ActionManager manager = ActionManager.getInstance();
            manager.setSelected("font-bold", StyleConstants.isBold(set));
            manager.setSelected("font-italic", StyleConstants.isItalic(set));
            manager.setSelected("font-underline", StyleConstants.isUnderline(set));

            elem = document.getParagraphElement(dot);
            set = elem.getAttributes();

            // Update the paragraph selector if applicable.
            if (selector != null) {
                selector.setSelectedItem(set.getAttribute(StyleConstants.NameAttribute));
            }

            switch (StyleConstants.getAlignment(set)) {
                // XXX There is a bug here. the setSelected method
                // should only affect the UI actions rather than propagate
                // down into the action map actions.
            case StyleConstants.ALIGN_LEFT:
                manager.setSelected("left-justify", true);
                break;

            case StyleConstants.ALIGN_CENTER:
                manager.setSelected("center-justify", true);
                break;

            case StyleConstants.ALIGN_RIGHT:
                manager.setSelected("right-justify", true);
                break;
            }
        }
    }
    
    /**
     * Handles sloppy HTML. This implementation currently only looks for
     * tags that have a / at the end (self-closing tags) and fixes them
     * to work with the version of HTML supported by HTMLEditorKit
     * <p>TODO: Need to break this functionality out so it can take pluggable
     * replacement code blocks, allowing people to write custom replacement
     * routines. The idea is that with some simple modifications a lot more
     * sloppy HTML can be rendered correctly.
     *
     * @author rbair
     */
    private static final class SloppyHTMLEditorKit extends HTMLEditorKit {
        @Override
        public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException {
            //read the reader into a String
            StringBuffer buffer = new StringBuffer();
            int length;
            char[] data = new char[1024];
            while ((length = in.read(data)) != -1) {
                buffer.append(data, 0, length);
            }
            //TODO is this regex right?
            StringReader reader = new StringReader(buffer.toString().replaceAll("/>", ">"));
            super.read(reader, doc, pos);
        }
    }    
}

