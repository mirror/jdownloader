package jd.plugins.optional.jdreconnectrecorder;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.gui.skins.simple.SimpleGUI;
import jd.parser.Regex;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;

public class JDRR extends PluginOptional {

    static public Vector<String> steps;
    static boolean gui = false;
    static boolean running = false;
    static ServerSocket Server_Socket;

    public JDRR(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
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
        return true;
    }

    static public void startServer(String serverip) {
        steps = new Vector<String>();
        steps.add("[[[HSRC]]]");
        try {
            Server_Socket = new ServerSocket(12345);
            running = true;
            new JDRRServer(Server_Socket, serverip).start();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    static public void stopServer() {
        running = false;
        steps.add("[[[/HSRC]]]");
        try {
            Server_Socket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    static public class JDRRServer extends Thread {
        ServerSocket Server_Socket = null;
        String serverip;

        public JDRRServer(ServerSocket Server_Socket, String serverip) {
            this.Server_Socket = Server_Socket;
            this.serverip = serverip;
        }

        public void run() {
            while (running) {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                }
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
                new JDRRGui(SimpleGUI.CURRENTGUI.getFrame()).setVisible(true);
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
