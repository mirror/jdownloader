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

package jd.plugins.optional.antistandby;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.controlling.JDLogger;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.OSDetector;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", defaultEnabled = false, id = "jdantistandby", interfaceversion = 7, mac = false, linux = false)
public class JDAntiStandby extends PluginOptional {
    private static final String CONFIG_MODE = "CONFIG_MODE";
    private String[]            MODES_AVAIL;
    private MenuAction          menuAction;
    private boolean             status;
    private JDAntiStandbyThread asthread    = null;

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
        if (menuAction == null) menuAction = new MenuAction("jdantistandby", "gui.images.config.eventmanager") {

            private static final long serialVersionUID = -5269457972563036769L;

            @Override
            public void initDefaults() {
                this.setEnabled(true);
                this.setType(ToolBarAction.Types.TOGGLE);
                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == SELECTED_KEY) {
                            status = isSelected();
                        }
                    }
                });
            }

        };
        try {
            switch (OSDetector.getID()) {
            case OSDetector.OS_WINDOWS_2003:
            case OSDetector.OS_WINDOWS_VISTA:
            case OSDetector.OS_WINDOWS_XP:
            case OSDetector.OS_WINDOWS_7:
            case OSDetector.OS_WINDOWS_2000:
            case OSDetector.OS_WINDOWS_NT:
                asthread = new JDAntiStandbyThread(this);
                asthread.start();
                return true;
            default:
                logger.fine("JDAntiStandby: System is not supported (" + OSDetector.getOSString() + ")");
            }
        } catch (Throwable e) {
            JDLogger.exception(e);
            logger.fine("JDAntiStandby: init failed");
        }
        return false;
    }

    @Override
    public void onExit() {
        if (asthread != null) {
            asthread.setRunning(false);
            asthread = null;
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        menu.add(menuAction);
        return menu;
    }

    private void initConfig() {
        config.setGroup(new ConfigGroup(getHost(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), CONFIG_MODE, MODES_AVAIL, JDL.L("gui.config.antistandby.mode", "Mode:")).setDefaultValue(0));
    }

}
