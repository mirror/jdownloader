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

package jd.plugins.optional.JDAntiStandby;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.OSDetector;
import jd.plugins.OptionalPlugin;
import jd.plugins.Plugin;
import jd.plugins.PluginOptional;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", defaultEnabled = false, id = "jdantistandby", interfaceversion = 5, mac = false, linux = false)
public class JDAntiStandby extends PluginOptional {
    private static final String CONFIG_MODE = "CONFIG_MODE";
    Thread thread;
    private String[] MODES_AVAIL;
    private MenuAction menuAction;
    private boolean status;

    public boolean isStatus() {
        return status;
    }

    public JDAntiStandby(PluginWrapper wrapper) {
        super(wrapper);
        MODES_AVAIL = new String[] { JDL.L("gui.config.antistandby.disabled", "Disabled"), JDL.L("gui.config.antistandby.whiledl", "Prevent standby while Downloading"), JDL.L("gui.config.antistandby.whilejd", "Prevent standby while JD is running") };
        initConfig();
    }

    @Override
    public boolean initAddon() {
        if (menuAction == null) menuAction = new MenuAction(JDL.L("gui.config.antistandby.toggle", "JDAntiStandby"), "gui.images.config.eventmanager") {

            /**
             * 
             */
            private static final long serialVersionUID = -5269457972563036769L;

            public void initDefaults() {
                this.setEnabled(true);
                setType(ToolBarAction.Types.TOGGLE);
                this.setIcon("gui.images.config.eventmanager");
                this.addPropertyChangeListener(new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == SELECTED_KEY) {
                            status = isSelected();
                        }
                    }
                });
            }

        };
        switch (OSDetector.getOSID()) {
        case OSDetector.OS_WINDOWS_2003:
        case OSDetector.OS_WINDOWS_VISTA:
        case OSDetector.OS_WINDOWS_XP:
        case OSDetector.OS_WINDOWS_7:
        case OSDetector.OS_WINDOWS_2000:
        case OSDetector.OS_WINDOWS_NT:
            thread = new Thread(new JDAntiStandbyThread(Plugin.logger, this));
            thread.start();
            break;
        default:
            logger.fine("Not supported System");
        }
        return true;
    }

    @Override
    public void onExit() {
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        menu.add(menuAction);
        return menu;
    }

    public void initConfig() {
        SubConfiguration subConfig = getPluginConfig();
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, subConfig, CONFIG_MODE, MODES_AVAIL, JDL.L("gui.config.antistandby.mode", "Mode:")).setDefaultValue(0));
    }

}
