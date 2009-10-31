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

import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextTableColumn;
import jd.plugins.optional.langfileeditor.KeyInfo;

public class TypeColumn extends JDTextTableColumn {

    private static final long serialVersionUID = 4030301646643222509L;

    public TypeColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    @Override
    protected String getStringValue(Object value) {
        KeyInfo keyInfo = (KeyInfo) value;
        if (keyInfo.isMissing()) return "M";
        if (keyInfo.getSource() == null || keyInfo.getSource().equals("")) return "O";
        if (keyInfo.getLanguage() != null && keyInfo.getLanguage().trim().length() > 0) return "D";
        return " ";
    }

}
