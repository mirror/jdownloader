//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.remotecontrol;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.plugins.optional.interfaces.HttpServer;
import jd.utils.locale.JDL;

/**
 * Alle Ausgaben sollten lediglich eine Zeile lang sein, um die kompatibilität
 * zu erhöhen.
 */
@OptionalPlugin(rev = "$Revision$", id = "remotecontrol", interfaceversion = 7)
public class JDRemoteControl extends PluginOptional {

    private static final String    PARAM_PORT      = "PORT";
    private static final String    PARAM_LOCALHOST = "LOCALHOST";
    private static final String    PARAM_ENABLED   = "ENABLED";

    private final SubConfiguration subConfig;

    private HttpServer             server;
    private MenuAction             activate;

    public JDRemoteControl(final PluginWrapper wrapper) {
        super(wrapper);
        subConfig = getPluginConfig();
        initConfig();
    }

    @Override
    public String getIconKey() {
        return "gui.images.server";
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        try {
            subConfig.setProperty(PARAM_ENABLED, activate.isSelected());
            subConfig.save();

            if (activate.isSelected()) {
                server = initServer();
                if (server != null) server.start();
                UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.optional.remotecontrol.startedonport2", "%s started on port %s\nhttp://127.0.0.1:%s\n/help for Developer Information.", getHost(), subConfig.getIntegerProperty(PARAM_PORT, 10025), subConfig.getIntegerProperty(PARAM_PORT, 10025)));
            } else {
                if (server != null) server.sstop();
                UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.optional.remotecontrol.stopped2", "%s stopped.", getHost()));
            }
        } catch (Exception ex) {
            JDLogger.exception(ex);
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        final ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(activate);

        return menu;
    }

    @Override
    public boolean initAddon() {
        boolean enabled = subConfig.getBooleanProperty(PARAM_ENABLED, true);

        activate = new MenuAction("remotecontrol", 0);
        activate.setActionListener(this);
        activate.setIcon(this.getIconKey());
        activate.setSelected(enabled);

        if (enabled) {
            server = initServer();
            if (server != null) server.start();
        }

        logger.info("RemoteControl OK");
        return true;
    }

    private void initConfig() {
        config.setGroup(new ConfigGroup(getHost(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PARAM_PORT, JDL.L("plugins.optional.RemoteControl.port", "Port:"), 1000, 65500, 1).setDefaultValue(10025));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PARAM_LOCALHOST, JDL.L("plugins.optional.RemoteControl.localhost", "localhost only?")).setDefaultValue(false));
    }

    private HttpServer initServer() {
        try {
            return new HttpServer(subConfig.getIntegerProperty(PARAM_PORT, 10025), new Serverhandler(), subConfig.getBooleanProperty(PARAM_LOCALHOST, false));
        } catch (IOException e) {
            JDLogger.exception(e);
        }

        return null;
    }

    @Override
    public void onExit() {
    }
}