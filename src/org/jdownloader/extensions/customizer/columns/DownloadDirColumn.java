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

package org.jdownloader.extensions.customizer.columns;

import javax.swing.JComponent;

import jd.gui.swing.components.BrowseFile;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.ExtTable;
import org.appwork.utils.swing.table.ExtTableModel;
import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdownloader.extensions.customizer.CustomizeSetting;

public class DownloadDirColumn extends ExtColumn<CustomizeSetting> {

    private static final long    serialVersionUID = 1687752044574718418L;
    private final JRendererLabel jlr;
    private final BrowseFile     file;

    public DownloadDirColumn(String name, ExtTableModel<CustomizeSetting> table) {
        super(name, table);
        setClickcount(2);

        jlr = new JRendererLabel();
        jlr.setBorder(null);

        file = new BrowseFile(new MigLayout("ins 0", "[fill,grow]2[min!]", "[21!]"));
        file.setFileSelectionMode(BrowseFile.DIRECTORIES_ONLY);
        file.setButtonText("...");
        file.getTextField().setBorder(null);
    }

    @Override
    public Object getCellEditorValue() {
        return file.getText();
    }

    @Override
    public boolean isEditable(CustomizeSetting obj) {
        return isEnabled(obj);
    }

    @Override
    public boolean isEnabled(CustomizeSetting obj) {
        return obj.isEnabled();
    }

    @Override
    public boolean isSortable(CustomizeSetting obj) {
        return false;
    }

    @Override
    public JComponent getEditorComponent(ExtTable<CustomizeSetting> table, CustomizeSetting value, boolean isSelected, int row, int column) {
        file.setText(((CustomizeSetting) value).getDownloadDir());
        return file;
    }

    @Override
    public JComponent getRendererComponent(ExtTable<CustomizeSetting> table, CustomizeSetting value, boolean isSelected, boolean hasFocus, int row, int column) {
        jlr.setText(((CustomizeSetting) value).getDownloadDir());
        return jlr;
    }

    @Override
    public void setValue(Object value, CustomizeSetting object) {
        object.setDownloadDir((String) value);
    }

}
