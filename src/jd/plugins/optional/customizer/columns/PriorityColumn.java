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
import javax.swing.SwingConstants;

import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextEditorTableColumn;
import jd.plugins.optional.customizer.CustomizeSetting;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class PriorityColumn extends JDTextEditorTableColumn {

    private static final long serialVersionUID = 4640856288557573254L;

    public PriorityColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    protected void prepareTableCellRendererComponent(JRendererLabel jlr) {
        jlr.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    protected void prepareTableCellEditorComponent(JTextField text) {
        text.setHorizontalAlignment(SwingConstants.CENTER);
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
    protected String getStringValue(Object value) {
        return String.valueOf(((CustomizeSetting) value).getPriority());
    }

    @Override
    protected void setStringValue(String value, Object object) {
        try {
            ((CustomizeSetting) object).setPriority(Integer.parseInt(value));
        } catch (Exception e) {
            return;
        }
    }

}
