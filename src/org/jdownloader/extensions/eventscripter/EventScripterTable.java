package org.jdownloader.extensions.eventscripter;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtTableModel;

public class EventScripterTable extends BasicJDTable<ScriptEntry> {

    public EventScripterTable(ExtTableModel<ScriptEntry> tableModel) {
        super(tableModel);
    }

}
