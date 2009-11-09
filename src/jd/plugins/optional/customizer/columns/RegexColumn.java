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

package jd.plugins.optional.customizer.columns;

import java.awt.Color;
import java.awt.Component;
import java.util.regex.Pattern;

import javax.swing.JComponent;

import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextEditorTableColumn;
import jd.plugins.optional.customizer.CustomizeSetting;
import jd.utils.locale.JDL;

public class RegexColumn extends JDTextEditorTableColumn {

    private static final long serialVersionUID = -2305836770033923728L;
    private static final String JDL_PREFIX = "jd.plugins.optional.customizer.columns.RegexColumn.";

    public RegexColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    public boolean isEditable(Object obj) {
        return isEnabled(obj);
    }

    @Override
    public boolean isEnabled(Object obj) {
        return ((CustomizeSetting) obj).isEnabled();
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    @Override
    public void postprocessCell(Component c, JDTableModel table, Object value, boolean isSelected, int row, int column) {
        if (((CustomizeSetting) value).getRegex() == null || ((CustomizeSetting) value).getRegex().equals("")) {
            c.setBackground(new Color(221, 34, 34));
            ((JComponent) c).setToolTipText(JDL.L(JDL_PREFIX + "regex.empty", "Regex shouldn't be empty!"));
            return;
        }
        try {
            Pattern.compile(((CustomizeSetting) value).getRegex());
        } catch (Exception e) {
            c.setBackground(new Color(221, 34, 34));
            ((JComponent) c).setToolTipText(JDL.LF(JDL_PREFIX + "regex.malformed", "Malformed Regex: %s", e.getMessage()));
        }
    }

    @Override
    protected String getStringValue(Object value) {
        return ((CustomizeSetting) value).getRegex();
    }

    @Override
    protected void setStringValue(String value, Object object) {
        ((CustomizeSetting) object).setRegex(value);
    }

}
