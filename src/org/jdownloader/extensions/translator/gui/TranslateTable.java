package org.jdownloader.extensions.translator.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtOverlayRowHighlighter;
import org.appwork.swing.exttable.ExtTable;
import org.jdownloader.extensions.translator.TranslateEntry;
import org.jdownloader.extensions.translator.TranslatorExtension;
import org.jdownloader.extensions.translator.gui.actions.CopyFromOtherAction;
import org.jdownloader.extensions.translator.gui.actions.ResetTranslationAction;
import org.jdownloader.extensions.translator.gui.actions.UseDefaultAction;

/**
 * Table for all entries
 * 
 * @author thomas
 * 
 */

public class TranslateTable extends BasicJDTable<TranslateEntry> {
    private ExtOverlayRowHighlighter rhDefault;
    private TranslatorExtension      owner;

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, TranslateEntry contextObject, java.util.List<TranslateEntry> selection, ExtColumn<TranslateEntry> column, MouseEvent ev) {

        popup.add(new CopyFromOtherAction(owner, selection));
        popup.add(new UseDefaultAction(owner, selection));
        popup.add(new ResetTranslationAction(owner, selection));
        return popup;
    }

    public TranslateTable(TranslatorExtension owner, final TranslateTableModel tableModel) {

        super(tableModel);
        this.owner = owner;
        this.setSearchEnabled(true);

        int opacity = 20;

        addRowHighlighter(new ExtOverlayRowHighlighter(null, new Color(255, 80, 0, opacity)) {
            @Override
            public boolean doHighlight(ExtTable<?> extTable, int row) {
                return getModel().getObjectbyRow(row).isMissing();
            }
        });
        addRowHighlighter(new ExtOverlayRowHighlighter(null, new Color(255, 0, 0, opacity)) {
            @Override
            public boolean doHighlight(ExtTable<?> extTable, int row) {
                return getModel().getObjectbyRow(row).isParameterInvalid();
            }
        });
        addRowHighlighter(new ExtOverlayRowHighlighter(null, new Color(255, 255, 80, opacity)) {
            @Override
            public boolean doHighlight(ExtTable<?> extTable, int row) {

                TranslateEntry e = getModel().getObjectbyRow(row);
                return e.isDefault();
            }
        });
        addRowHighlighter(new ExtOverlayRowHighlighter(null, new Color(0, 255, 80, opacity)) {
            @Override
            public boolean doHighlight(ExtTable<?> extTable, int row) {
                TranslateEntry e = getModel().getObjectbyRow(row);
                return (e.isOK() && !e.isDefault());
            }
        });
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("TAB"), "TAB");
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("TAB"), "TAB");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("TAB"), "TAB");

        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "ENTER");
        getActionMap().put("ENTER", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {

                TranslateEntry object = tableModel.getObjectbyRow(getSelectedRow());
                tableModel.getEditColum().startEditing(object);
                tableModel.getEditColum().getEditorField().requestFocus();

            }
        });
        getActionMap().put("TAB", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {

                int index = getSelectedRow();
                getSelectionModel().setSelectionInterval(index + 1, index + 1);
                TranslateEntry object = tableModel.getObjectbyRow(getSelectedRow());
                tableModel.getEditColum().startEditing(object);
                tableModel.getEditColum().getEditorField().requestFocus();
                TranslateTable.this.scrollToSelection(-1);
                // editCellAt(index + 1, tableModel.getEditColum().getIndex());
                // ((ExtTextColumn) getCellEditor()).
            }
        });
    }

    // @Override
    // protected void onDoubleClick(MouseEvent e, TranslateEntry obj) {
    // super.onDoubleClick(e, obj);
    // }

    public void updaterFilter(SearchField searchField) {

        ((TranslateTableModel) getModel()).updateFilter(searchField);
    }

}
