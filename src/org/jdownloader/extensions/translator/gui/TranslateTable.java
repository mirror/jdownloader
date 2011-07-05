package org.jdownloader.extensions.translator.gui;

import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.BasicJDTable;

import org.jdownloader.extensions.translator.TranslateEntry;

public class TranslateTable extends BasicJDTable<TranslateEntry> {

    public TranslateTable(TranslateTableModel tableModel) {
        super(tableModel);
        this.setSearchEnabled(true);

    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, TranslateEntry contextObject, ArrayList<TranslateEntry> selection) {
        return super.onContextMenu(popup, contextObject, selection);
    }

    @Override
    protected void onDoubleClick(MouseEvent e, TranslateEntry obj) {
        super.onDoubleClick(e, obj);
    }

}
