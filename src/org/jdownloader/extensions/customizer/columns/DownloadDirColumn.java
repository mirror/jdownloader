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

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdownloader.extensions.customizer.CustomizeSetting;

public class DownloadDirColumn extends ExtColumn<CustomizeSetting> {

    private static final long    serialVersionUID = 1687752044574718418L;
    private final JRendererLabel renderer;
    private final BrowseFile     editor;

    public DownloadDirColumn(String name, ExtTableModel<CustomizeSetting> table) {
        super(name, table);
        setClickcount(2);

        renderer = new JRendererLabel();

        editor = new BrowseFile(new MigLayout("ins 0", "[fill,grow]2[min!]", "[21!]"));
        editor.setFileSelectionMode(BrowseFile.DIRECTORIES_ONLY);
        editor.setButtonText("...");
        editor.getTextField().setBorder(null);
    }

    @Override
    public Object getCellEditorValue() {
        return editor.getText();
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
    public void configureEditorComponent(CustomizeSetting value, boolean isSelected, int row, int column) {
        editor.setText(((CustomizeSetting) value).getDownloadDir());
    }

    @Override
    public void configureRendererComponent(CustomizeSetting value, boolean isSelected, boolean hasFocus, int row, int column) {
        renderer.setText(((CustomizeSetting) value).getDownloadDir());
    }

    @Override
    public void setValue(Object value, CustomizeSetting object) {
        object.setDownloadDir((String) value);
    }

    @Override
    public JComponent getEditorComponent(CustomizeSetting value, boolean isSelected, int row, int column) {
        return editor;
    }

    @Override
    public JComponent getRendererComponent(CustomizeSetting value, boolean isSelected, boolean hasFocus, int row, int column) {
        return renderer;
    }

    @Override
    public void resetEditor() {
    }

    @Override
    public void resetRenderer() {
        renderer.setBorder(null);
    }

}
