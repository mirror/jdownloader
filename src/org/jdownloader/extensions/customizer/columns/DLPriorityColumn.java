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

import javax.swing.JComboBox;
import javax.swing.JComponent;

import jd.gui.swing.jdgui.views.downloads.DownloadTable;

import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.ExtTableModel;
import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdownloader.extensions.customizer.CustomizeSetting;

public class DLPriorityColumn extends ExtColumn<CustomizeSetting> {

    private static final long    serialVersionUID = 4640856288557573254L;
    private static String[]      prioDescs;
    private final JRendererLabel renderer;
    private final JComboBox      editor;

    public DLPriorityColumn(String name, ExtTableModel<CustomizeSetting> table) {
        super(name, table);

        renderer = new JRendererLabel();

        editor = new JComboBox(prioDescs = DownloadTable.PRIO_DESCS);

    }

    @Override
    public Object getCellEditorValue() {
        return editor.getSelectedIndex();
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
        editor.setSelectedIndex(((CustomizeSetting) value).getDLPriority() + 1);
    }

    @Override
    public void configureRendererComponent(CustomizeSetting value, boolean isSelected, boolean hasFocus, int row, int column) {
        renderer.setText(prioDescs[((CustomizeSetting) value).getDLPriority() + 1]);

    }

    @Override
    public void setValue(Object value, CustomizeSetting object) {
        object.setDLPriority((Integer) value - 1);
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
        editor.setBorder(null);
    }

    @Override
    public void resetRenderer() {
        renderer.setBorder(null);
    }

}
