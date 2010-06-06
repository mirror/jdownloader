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

import java.awt.Component;

import jd.gui.swing.components.BrowseFile;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.customizer.CustomizeSetting;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class DownloadDirColumn extends JDTableColumn {

    private static final long serialVersionUID = 1687752044574718418L;
    private JRendererLabel jlr;
    private BrowseFile file;

    public DownloadDirColumn(String name, JDTableModel table) {
        super(name, table);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        file = new BrowseFile(new MigLayout("ins 0", "[fill,grow]2[min!]", "[21!]"));
        file.setFileSelectionMode(BrowseFile.DIRECTORIES_ONLY);
        file.setButtonText("...");
        file.getTextField().setBorder(null);
        setClickstoEdit(2);
    }

    @Override
    public Object getCellEditorValue() {
        return file.getText();
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
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        file.setText(((CustomizeSetting) value).getDownloadDir());
        return file;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        jlr.setText(((CustomizeSetting) value).getDownloadDir());
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
        ((CustomizeSetting) object).setDownloadDir((String) value);
    }

}
