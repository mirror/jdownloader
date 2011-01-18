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

package jd.gui.swing.jdgui.views.settings.panels.addons;

import jd.OptionalPluginWrapper;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.menu.AddonsMenu;
import jd.gui.swing.jdgui.views.settings.sidebar.ConfigSidebar;
import jd.utils.locale.JDL;

import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtCheckColumn;

public class ActivateColumn extends ExtCheckColumn<OptionalPluginWrapper> {

    private static final long       serialVersionUID = 658156218405204887L;
    private final ConfigPanelAddons addons;

    public ActivateColumn(String name, ExtTableModel<OptionalPluginWrapper> table, ConfigPanelAddons addons) {
        super(name, table);

        this.addons = addons;
    }

    @Override
    public boolean isEditable(OptionalPluginWrapper obj) {
        return true;
    }

    @Override
    protected boolean getBooleanValue(OptionalPluginWrapper value) {
        return value.isEnabled();
    }

    @Override
    protected void setBooleanValue(boolean value, OptionalPluginWrapper object) {
        if (value == object.isEnabled()) return;
        if (value) {
            if (object.getPlugin().startAddon()) {
                if (object.getAnnotation().hasGui()) {
                    int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, object.getHost(), JDL.LF("jd.gui.swing.jdgui.settings.panels.ConfigPanelAddons.askafterinit", "Show %s now?\r\nYou may open it later using Mainmenu->Addon", object.getHost()));

                    if (UserIO.isOK(ret)) {
                        object.getPlugin().setGuiEnable(true);
                    }
                }
            }
        } else {
            object.getPlugin().setGuiEnable(false);
            object.getPlugin().stopAddon();
        }
        /*
         * we save enabled/disabled status here, plugin must be running when
         * enabled
         */
        object.setEnabled(object.getPlugin().isRunning());
        AddonsMenu.getInstance().update();
        ConfigSidebar.getInstance(null).updateAddons();
        addons.updateShowcase();
    }

}
