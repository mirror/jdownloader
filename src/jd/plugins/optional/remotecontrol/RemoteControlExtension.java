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
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.plugins.AddonPanel;
import jd.plugins.OptionalPlugin;
import jd.plugins.optional.ExtensionConfigPanel;
import jd.plugins.optional.PluginOptional;
import jd.plugins.optional.StartException;
import jd.plugins.optional.StopException;
import jd.plugins.optional.interfaces.HttpServer;
import jd.utils.locale.JDL;

import org.appwork.shutdown.ShutdownVetoException;

/**
 * Alle Ausgaben sollten lediglich eine Zeile lang sein, um die kompatibilität
 * zu erhöhen.
 */
@OptionalPlugin(rev = "$Revision$", id = "remotecontrol", interfaceversion = 7)
public class RemoteControlExtension extends PluginOptional implements ActionListener {

    private static final String PARAM_PORT      = "PORT";
    private static final String PARAM_LOCALHOST = "LOCALHOST";
    private static final String PARAM_ENABLED   = "ENABLED";

    private final JSonWrapper   subConfig;

    private HttpServer          server;
    private MenuAction          activate;

    public ExtensionConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public RemoteControlExtension() throws StartException {
        super(JDL.L("jd.plugins.optional.remotecontrol.jdremotecontrol", null));
        subConfig = getPluginConfig();

    }

    @Override
    public String getIconKey() {
        return "gui.images.server";
    }

    public void actionPerformed(final ActionEvent e) {
        try {
            subConfig.setProperty(PARAM_ENABLED, activate.isSelected());
            subConfig.save();

            if (activate.isSelected()) {
                server = initServer();
                if (server != null) server.start();
                UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.optional.remotecontrol.startedonport2", "%s started on port %s\nhttp://127.0.0.1:%s\n/help for Developer Information.", getName(), subConfig.getIntegerProperty(PARAM_PORT, 10025), subConfig.getIntegerProperty(PARAM_PORT, 10025)));
            } else {
                if (server != null) server.sstop();
                UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.optional.remotecontrol.stopped2", "%s stopped.", getName()));
            }
        } catch (Exception ex) {
            JDLogger.exception(ex);
        }
    }

    private HttpServer initServer() {
        try {
            return new HttpServer(subConfig.getIntegerProperty(PARAM_PORT, 10025), new Serverhandler(this), subConfig.getBooleanProperty(PARAM_LOCALHOST, false));
        } catch (IOException e) {
            JDLogger.exception(e);
        }

        return null;
    }

    public void onShutdown() {
    }

    public boolean onShutdownRequest() throws ShutdownVetoException {
        return false;
    }

    public void onShutdownVeto(ArrayList<ShutdownVetoException> vetos) {
    }

    @Override
    protected void stop() throws StopException {
    }

    @Override
    protected void start() throws StartException {
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

    }

    @Override
    protected void initSettings(ConfigContainer config) {
        config.setGroup(new ConfigGroup(getName(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PARAM_PORT, JDL.L("plugins.optional.RemoteControl.port", "Port:"), 1000, 65500, 1).setDefaultValue(10025));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PARAM_LOCALHOST, JDL.L("plugins.optional.RemoteControl.localhost", "localhost only?")).setDefaultValue(false));

    }

    @Override
    public String getConfigID() {
        return null;
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return JDL.L("jd.plugins.optional.remotecontrol.jdremotecontrol.description", null);
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        final ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(activate);

        return menu;
    }

    @Override
    protected void initExtension() throws StartException {
    }
}