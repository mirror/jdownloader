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

package org.jdownloader.extensions.langfileeditor.columns;

import org.appwork.utils.Regex;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.extensions.langfileeditor.KeyInfo;
import org.jdownloader.extensions.langfileeditor.LFEGui;
import org.jdownloader.extensions.langfileeditor.LFETableModel;
import org.jdownloader.extensions.langfileeditor.translate.T;

public class LanguageColumn extends ExtTextColumn<KeyInfo> {

    private static final long serialVersionUID = -2305836770033923728L;

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
        if (obj.hasWrongParameterCount()) return T._.jd_plugins_optional_langfileeditor_columns_LanguageColumn_tooltip_wrongParameterCount();

        String match = new Regex(((KeyInfo) obj).getKey(), "gui\\.menu\\.(.*?)\\.accel").getMatch(0);
        if (match != null) {
            StringBuilder toolTip = new StringBuilder();
            toolTip.append(T._.jd_plugins_optional_langfileeditor_columns_LanguageColumn_tooltip_accelerator(match));
            if (new Regex(obj.getLanguage(), "(CONTROL|STRG|UMSCHALT|ALT GR|ALT_GR)").matches()) {
                toolTip.append(new char[] { ' ', '[' }).append(T._.jd_plugins_optional_langfileeditor_columns_LanguageColumn_tooltip_accelerator_wrong(obj.getLanguage())).append(']');
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
    protected void setStringValue(final String value, final KeyInfo object) {
        if (object.getLanguage().equals(value)) return;
        object.setLanguage(value);
        ((LFETableModel) getModel()).getGui().dataChanged();
    }

}