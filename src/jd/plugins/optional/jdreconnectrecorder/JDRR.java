//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.optional.jdreconnectrecorder;

import java.awt.event.ActionEvent;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.parser.Regex;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDRR extends PluginOptional {

    static public Vector<String> steps;
    static boolean gui = false;
    static boolean running = false;
    static ServerSocket Server_Socket;
    static final String PROPERTY_PORT = "PARAM_PORT";
    static String auth;

    public JDRR(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static int getAddonInterfaceVersion() {
        return 2;
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public boolean initAddon() {
        running = false;
        SubConfiguration subConfig = JDUtilities.getSubConfig("JDRR");
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_PORT, JDLocale.L("plugins.optional.jdrr.port", "Port"), 1024, 65000));
        cfg.setStep(1);
        cfg.setDefaultValue(8972);
        return true;
    }

    static public void startServer(String serverip) {
        steps = new Vector<String>();
        auth = null;
        steps.add("[[[HSRC]]]");
        try {
            Server_Socket = new ServerSocket(JDUtilities.getSubConfig("JDRR").getIntegerProperty(JDRR.PROPERTY_PORT, 8972));
            running = true;
            new JDRRServer(Server_Socket, serverip).start();
        } catch (Exception e) {
        }
    }

    static public void stopServer() {
        running = false;
        steps.add("[[[/HSRC]]]");
        try {
            Server_Socket.close();
        } catch (Exception e) {
        }
    }

    static public class JDRRServer extends Thread {
        ServerSocket Server_Socket = null;
        String serverip;

        public JDRRServer(ServerSocket Server_Socket, String serverip) {
            this.Server_Socket = Server_Socket;
            this.serverip = serverip;
            this.setName("JDRRServer");
        }

        public void run() {
            while (running) {
                Socket Client_Socket = null;
                try {
                    Client_Socket = Server_Socket.accept();
                } catch (Exception e) {
                    break;
                }
                JDRRproxy record = new JDRRproxy(Client_Socket, steps, serverip);
                record.start();
            }
            running = false;
            try {
                Server_Socket.close();
            } catch (Exception e) {
            }
        }

    }

    @Override
    public void onExit() {
        stopServer();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Create ReconnectScript")) {
            if (!gui) {
                gui = true;
                new JDRRGui().setVisible(true);
            }
        }
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(new MenuItem("Create ReconnectScript", 0).setActionListener(this));
        return menu;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision: 2851 $", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.jdreconnectrecorder.name", "ReconnectRecorder");
    }

}
