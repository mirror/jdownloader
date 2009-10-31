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

package jd.plugins.optional.langfileeditor.columns;

import java.awt.Component;

import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextEditorTableColumn;
import jd.plugins.optional.langfileeditor.KeyInfo;
import jd.plugins.optional.langfileeditor.LFEGui;
import jd.plugins.optional.langfileeditor.LFETableModel;

public class LanguageColumn extends JDTextEditorTableColumn {

    private static final long serialVersionUID = -2305836770033923728L;

    public LanguageColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    public boolean isEditable(Object obj) {
        return true;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return true;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
        ((LFETableModel) getJDTableModel()).setSorting(LFETableModel.SORT_LANGUAGE, sortingToggle);
    }

    @Override
    public void postprocessCell(Component c, JDTableModel table, Object value, boolean isSelected, int row, int column) {
        if (((KeyInfo) value).hasWrongParameterCount()) {
            c.setBackground(LFEGui.COLOR_MISSING);
        }
    }

    @Override
    protected String getStringValue(Object value) {
        return ((KeyInfo) value).getLanguage();
    }

    @Override
    protected void setStringValue(String value, Object object) {
        if (((KeyInfo) object).getLanguage().equals(value)) return;
        ((KeyInfo) object).setLanguage(value);
        ((LFETableModel) getJDTableModel()).getGui().dataChanged();
    }

}
