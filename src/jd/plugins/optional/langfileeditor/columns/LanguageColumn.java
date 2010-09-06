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

import jd.parser.Regex;
import jd.plugins.optional.langfileeditor.KeyInfo;
import jd.plugins.optional.langfileeditor.LFEGui;
import jd.plugins.optional.langfileeditor.LFETableModel;
import jd.utils.locale.JDL;

import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtTextEditorColumn;

public class LanguageColumn extends ExtTextEditorColumn<KeyInfo> {

    private static final long   serialVersionUID = -2305836770033923728L;
    private static final String JDL_PREFIX       = "jd.plugins.optional.langfileeditor.columns.LanguageColumn.";

    public LanguageColumn(String name, ExtTableModel<KeyInfo> table) {
        super(name, table);
    }

    @Override
    protected void prepareLabel(KeyInfo value) {
        if (value.hasWrongParameterCount()) {
            label.setBackground(LFEGui.COLOR_MISSING);
        } else if (new Regex(value.getKey(), "gui\\.menu\\.(.*?)\\.accel").matches() && new Regex(value.getLanguage(), "(CONTROL|STRG|UMSCHALT|ALT GR|ALT_GR)").matches()) {
            label.setBackground(LFEGui.COLOR_MISSING);
        }
    }

    @Override
    public String getToolTip(KeyInfo obj) {
        if (obj.hasWrongParameterCount()) return JDL.L(JDL_PREFIX + "tooltip.wrongParameterCount", "Your translated String contains a wrong count of placeholders!");

        String match = new Regex(((KeyInfo) obj).getKey(), "gui\\.menu\\.(.*?)\\.accel").getMatch(0);
        if (match != null) {
            StringBuilder toolTip = new StringBuilder();
            toolTip.append(JDL.LF(JDL_PREFIX + "tooltip.accelerator", "Insert the hotkey for the action %s here. Allowed modifiers are CTRL, ALTGR, ALT, META, SHIFT", match));
            if (new Regex(obj.getLanguage(), "(CONTROL|STRG|UMSCHALT|ALT GR|ALT_GR)").matches()) {
                toolTip.append(new char[] { ' ', '[' }).append(JDL.LF(JDL_PREFIX + "tooltip.accelerator.wrong", "The modifier %s isn't allowed!")).append(']');
            }
            return toolTip.toString();
        }

        return super.getToolTip(obj);
    }

    @Override
    protected String getStringValue(KeyInfo value) {
        return value.getLanguage();
    }

    @Override
    public void setValue(final Object value, final KeyInfo object) {
        if (object.getLanguage().equals(value)) return;
        object.setLanguage(value == null ? "" : value.toString());
        ((LFETableModel) getModel()).getGui().dataChanged();
    }

}
