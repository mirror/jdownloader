package org.jdownloader.extensions.translator.gui;

import java.util.ArrayList;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.utils.swing.table.ExtColumn;
import org.jdownloader.extensions.translator.TranslateEntry;
import org.jdownloader.extensions.translator.gui.actions.SetDefaultAction;

/**
 * Table for all entries
 * 
 * @author thomas
 * 
 */

public class TranslateTable extends BasicJDTable<TranslateEntry> {

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, TranslateEntry contextObject, ArrayList<TranslateEntry> selection, ExtColumn<TranslateEntry> column) {

        popup.add(new SetDefaultAction(selection));

        return popup;
    }

    public TranslateTable(TranslateTableModel tableModel) {
        super(tableModel);
        this.setSearchEnabled(true);

    }
    // @Override
    // protected void onDoubleClick(MouseEvent e, TranslateEntry obj) {
    // super.onDoubleClick(e, obj);
    // }

}
