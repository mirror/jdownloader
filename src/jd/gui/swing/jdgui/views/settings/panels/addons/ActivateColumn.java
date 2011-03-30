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

import jd.gui.UserIO;
import jd.gui.swing.jdgui.menu.AddonsMenu;
import jd.gui.swing.jdgui.menu.WindowMenu;
import jd.gui.swing.jdgui.views.settings.sidebar.ConfigSidebar;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtCheckColumn;
import org.jdownloader.extensions.AbstractExtensionWrapper;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.translate.JDT;

public class ActivateColumn extends ExtCheckColumn<AbstractExtensionWrapper> {

    private static final long       serialVersionUID = 658156218405204887L;
    private final ConfigPanelAddons addons;

    public ActivateColumn(String name, ExtTableModel<AbstractExtensionWrapper> table, ConfigPanelAddons addons) {
        super(name, table);

        this.addons = addons;
    }

    @Override
    public boolean isEditable(AbstractExtensionWrapper obj) {
        return true;
    }

    @Override
    protected boolean getBooleanValue(AbstractExtensionWrapper value) {
        return value._isEnabled();
    }

    @Override
    protected void setBooleanValue(boolean value, AbstractExtensionWrapper object) {
        if (value == object._isEnabled()) return;
        if (value) {
            try {
                object._setEnabled(true);

                if (object._getExtension().getGUI() != null) {
                    int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, object.getName(), JDT._.gui_settings_extensions_show_now(object.getName()));

                    if (UserIO.isOK(ret)) {
                        object._getExtension().getGUI().setActive(true);
                    }
                }
            } catch (StartException e) {
                Dialog.getInstance().showExceptionDialog(JDT._.dialog_title_exception(), e.getMessage(), e);
            } catch (StopException e) {
                e.printStackTrace();
            }
        } else {
            try {

                object._setEnabled(false);
            } catch (StartException e) {
                e.printStackTrace();
            } catch (StopException e) {
                Dialog.getInstance().showExceptionDialog(JDT._.dialog_title_exception(), e.getMessage(), e);
            }
        }
        /*
         * we save enabled/disabled status here, plugin must be running when
         * enabled
         */

        AddonsMenu.getInstance().update();
        WindowMenu.getInstance().update();
        ConfigSidebar.getInstance(null).updateAddons();
        addons.updateShowcase();
    }

}
