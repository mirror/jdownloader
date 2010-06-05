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

package jd.gui.swing.jdgui.views.settings.panels.addons.columns;

import jd.OptionalPluginWrapper;
import jd.config.Configuration;
import jd.gui.UserIO;
import jd.gui.swing.components.table.JDCheckBoxTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.menu.AddonsMenu;
import jd.gui.swing.jdgui.views.settings.panels.addons.ConfigPanelAddons;
import jd.gui.swing.jdgui.views.settings.sidebar.ConfigSidebar;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class ActivateColumn extends JDCheckBoxTableColumn {

    private static final long serialVersionUID = 658156218405204887L;
    private final Configuration config;
    private final ConfigPanelAddons addons;

    public ActivateColumn(String name, JDTableModel table, ConfigPanelAddons addons) {
        super(name, table);

        this.config = JDUtilities.getConfiguration();
        this.addons = addons;
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
    public void sort(Object obj, boolean sortingToggle) {
    }

    @Override
    protected boolean getBooleanValue(Object value) {
        return ((OptionalPluginWrapper) value).isEnabled();
    }

    @Override
    protected void setBooleanValue(boolean value, Object object) {
        OptionalPluginWrapper plgWrapper = ((OptionalPluginWrapper) object);
        if (value == plgWrapper.isEnabled()) return;
        config.setProperty(plgWrapper.getConfigParamKey(), value);
        config.save();
        if (value) {
            if (plgWrapper.getPlugin().startAddon()) {
                if (plgWrapper.getAnnotation().hasGui()) {
                    int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, plgWrapper.getHost(), JDL.LF("jd.gui.swing.jdgui.settings.panels.ConfigPanelAddons.askafterinit", "Show %s now?\r\nYou may open it later using Mainmenu->Addon", plgWrapper.getHost()));

                    if (UserIO.isOK(ret)) {
                        plgWrapper.getPlugin().setGuiEnable(true);
                    }
                }
            }
        } else {
            plgWrapper.getPlugin().setGuiEnable(false);
            plgWrapper.getPlugin().stopAddon();
        }
        AddonsMenu.getInstance().update();
        ConfigSidebar.getInstance(null).updateAddons();
        addons.updateShowcase();
    }

}
