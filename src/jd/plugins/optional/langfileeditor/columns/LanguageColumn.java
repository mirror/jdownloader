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

import javax.swing.JComponent;

import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextEditorTableColumn;
import jd.parser.Regex;
import jd.plugins.optional.langfileeditor.KeyInfo;
import jd.plugins.optional.langfileeditor.LFEGui;
import jd.plugins.optional.langfileeditor.LFETableModel;
import jd.utils.locale.JDL;

public class LanguageColumn extends JDTextEditorTableColumn {

    private static final long serialVersionUID = -2305836770033923728L;
    private static final String JDL_PREFIX = "jd.plugins.optional.langfileeditor.columns.LanguageColumn.";

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
            ((JComponent) c).setToolTipText(JDL.L(JDL_PREFIX + "tooltip.wrongParameterCount", "Your translated String contains a wrong count of placeholders!"));
            return;
        }
        String match = new Regex(((KeyInfo) value).getKey(), "gui\\.menu\\.(.*?)\\.accel").getMatch(0);
        if (match != null) {
            StringBuilder toolTip = new StringBuilder();

            toolTip.append(JDL.LF(JDL_PREFIX + "tooltip.accelerator", "Insert the hotkey for the action %s here. Allowed modifiers are CTRL, ALTGR, ALT, META, SHIFT", match));
            String match2 = new Regex(((KeyInfo) value).getLanguage(), "(CONTROL|STRG|UMSCHALT|ALT GR|ALT_GR)").getMatch(0);
            if (match2 != null) {
                toolTip.append(new char[] { ' ', '[' }).append(JDL.LF(JDL_PREFIX + "tooltip.accelerator.wrong", "The modifier %s isn't allowed!")).append(']');
                c.setBackground(LFEGui.COLOR_MISSING);
            }
            ((JComponent) c).setToolTipText(toolTip.toString());
        } else {
            ((JComponent) c).setToolTipText(null);
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
