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

import javax.swing.JTextField;

import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextEditorTableColumn;
import jd.plugins.optional.customizer.CustomizeSetting;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class PackageNameColumn extends JDTextEditorTableColumn {

    private static final long serialVersionUID = -2305836770033923728L;
    private final String toolTip;

    public PackageNameColumn(String name, JDTableModel table) {
        super(name, table);
        toolTip = JDL.L("jd.plugins.optional.customizer.columns.PackageNameColumn.toolTip", "The name of the filepackage, if the link matches the regex. Leave it empty to use the default name!");
    }

    @Override
    protected void prepareTableCellEditorComponent(JTextField text) {
        text.setToolTipText(toolTip);
    }

    @Override
    protected void prepareTableCellRendererComponent(JRendererLabel jlr) {
        jlr.setToolTipText(toolTip);
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
    protected String getStringValue(Object value) {
        return ((CustomizeSetting) value).getPackageName();
    }

    @Override
    protected void setStringValue(String value, Object object) {
        ((CustomizeSetting) object).setPackageName(value);
    }

}
