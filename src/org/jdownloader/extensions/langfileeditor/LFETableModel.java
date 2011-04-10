//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.extensions.langfileeditor;

import java.util.ArrayList;

import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtTextEditorColumn;
import org.jdownloader.extensions.langfileeditor.columns.LanguageColumn;
import org.jdownloader.extensions.langfileeditor.translate.T;

public class LFETableModel extends ExtTableModel<KeyInfo> {

    private static final long serialVersionUID = -1775792404758292253L;

    private final LFEGui      gui;

    public LFETableModel(LFEGui gui) {
        super("lfetable");

        this.gui = gui;

        refreshData();
    }

    public LFEGui getGui() {
        return gui;
    }

    @Override
    protected void initColumns() {
        this.addColumn(new ExtTextEditorColumn<KeyInfo>(T._.plugins_optional_langfileeditor_key(), this) {

            private static final long serialVersionUID = 7120563498624188924L;

            @Override
            protected String getStringValue(KeyInfo value) {
                return value.getKey();
            }

            @Override
            protected void setStringValue(String value, KeyInfo object) {
            }

        });
        this.addColumn(new ExtTextEditorColumn<KeyInfo>(T._.plugins_optional_langfileeditor_sourceValue(), this) {

            private static final long serialVersionUID = 8317088520940463895L;

            @Override
            protected String getStringValue(KeyInfo value) {
                return value.getSource();
            }

            @Override
            protected void setStringValue(String value, KeyInfo object) {
            }

        });
        this.addColumn(new ExtTextEditorColumn<KeyInfo>(T._.plugins_optional_langfileeditor_english(), this) {

            private static final long serialVersionUID = -2259126596005921191L;

            @Override
            protected String getStringValue(KeyInfo value) {
                return value.getEnglish();
            }

            @Override
            protected void setStringValue(String value, KeyInfo object) {
            }

        });
        this.addColumn(new LanguageColumn(T._.plugins_optional_langfileeditor_languageFileValue(), this));
    }

    protected void refreshData() {
        final ArrayList<KeyInfo> tmp = new ArrayList<KeyInfo>(gui.getData());

        final ArrayList<KeyInfo> selection = this.getSelectedObjects();
        tableData = tmp;
        refreshSort();

        fireTableStructureChanged();

        setSelectedObjects(selection);
    }

}