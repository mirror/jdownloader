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

package jd.gui.swing.jdgui.settings.panels.addons.columns;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import jd.OptionalPluginWrapper;
import jd.config.Configuration;
import jd.gui.UserIO;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.menu.AddonsMenu;
import jd.gui.swing.jdgui.views.sidebars.configuration.ConfigSidebar;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.JRendererCheckBox;

public class ActivateColumn extends JDTableColumn implements ActionListener {

    private static final long serialVersionUID = 658156218405204887L;
    private Configuration config;
    private JRendererCheckBox boolrend;
    private JCheckBox checkbox;

    public ActivateColumn(String name, JDTableModel table) {
        super(name, table);
        config = JDUtilities.getConfiguration();
        boolrend = new JRendererCheckBox();
        boolrend.setHorizontalAlignment(JCheckBox.CENTER);
        checkbox = new JCheckBox();
        checkbox.setHorizontalAlignment(JCheckBox.CENTER);
    }

    @Override
    public Object getCellEditorValue() {
        return checkbox.isSelected();
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
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        checkbox.removeActionListener(this);
        checkbox.setSelected(((OptionalPluginWrapper) value).isEnabled());
        checkbox.addActionListener(this);
        return checkbox;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        boolrend.setSelected(((OptionalPluginWrapper) value).isEnabled());
        return boolrend;
    }

    @Override
    public void setValue(Object value, Object object) {
        OptionalPluginWrapper plgWrapper = ((OptionalPluginWrapper) object);
        config.setProperty(plgWrapper.getConfigParamKey(), value);
        config.save();
        if ((Boolean) value) {
            plgWrapper.getPlugin().initAddon();
            if (plgWrapper.getAnnotation().hasGui()) {
                int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, plgWrapper.getHost(), JDL.LF("jd.gui.swing.jdgui.settings.panels.ConfigPanelAddons.askafterinit", "Show %s now?\r\nYou may open it later using Mainmenu->Addon", plgWrapper.getHost()));

                if (UserIO.isOK(ret)) {
                    plgWrapper.getPlugin().setGuiEnable(true);
                }
            }
        } else {
            plgWrapper.getPlugin().setGuiEnable(false);
            plgWrapper.getPlugin().onExit();
        }
        AddonsMenu.getInstance().update();
        ConfigSidebar.getInstance(null).updateAddons();
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    public void actionPerformed(ActionEvent e) {
        checkbox.removeActionListener(this);
        this.fireEditingStopped();
    }

}
