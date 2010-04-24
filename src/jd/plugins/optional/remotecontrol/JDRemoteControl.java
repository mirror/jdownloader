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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.httpserver.HttpServer;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

/**
 * Alle Ausgaben sollten lediglich eine Zeile lang sein, um die kompatibilität
 * zu erhöhen.
 */
@OptionalPlugin(rev = "$Revision$", id = "remotecontrol", interfaceversion = 5)
public class JDRemoteControl extends PluginOptional implements ControlListener {

    private static final String PARAM_PORT = "PORT";
    private static final String PARAM_ENABLED = "ENABLED";
    private final SubConfiguration subConfig;

    @SuppressWarnings("unused")
    private boolean grabberIsBusy;

    public JDRemoteControl(PluginWrapper wrapper) {
        super(wrapper);
        subConfig = getPluginConfig();
        initConfig();
    }

    @Override
    public String getIconKey() {
        return "gui.images.network";
    }

    private HttpServer server;
    private MenuAction activate;

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            subConfig.setProperty(PARAM_ENABLED, activate.isSelected());
            subConfig.save();

            if (activate.isSelected()) {
                server = new HttpServer(subConfig.getIntegerProperty(PARAM_PORT, 10025), new Serverhandler());
                server.start();
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
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        if (activate == null) {
            activate = new MenuAction("remotecontrol", 0);
            activate.setActionListener(this);
            activate.setSelected(subConfig.getBooleanProperty(PARAM_ENABLED, true));
            activate.setTitle(getHost());
        }

        menu.add(activate);
        return menu;
    }

    @Override
    public boolean initAddon() {
        logger.info("RemoteControl OK");
        initRemoteControl();
        JDUtilities.getController().addControlListener(this);
        return true;
    }

    private void initConfig() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PARAM_PORT, JDL.L("plugins.optional.RemoteControl.port", "Port:"), 1000, 65500));
        cfg.setDefaultValue(10025);
    }

    private void initRemoteControl() {
        if (subConfig.getBooleanProperty(PARAM_ENABLED, true)) {
            try {
                server = new HttpServer(subConfig.getIntegerProperty(PARAM_PORT, 10025), new Serverhandler());
                server.start();
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
    }

    @Override
    public void onExit() {
    }
}