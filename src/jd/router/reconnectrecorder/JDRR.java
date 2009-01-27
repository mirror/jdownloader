//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.router.reconnectrecorder;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import jd.utils.JDUtilities;

public class JDRR {

    static public Vector<String> steps;
    static boolean running = false;
    static ServerSocket Server_Socket_HTTP;
    static ServerSocket Server_Socket_HTTPS;
    static final String PROPERTY_PORT = "PARAM_PORT";
    static String auth;

    static public void startServer(String serverip) {
        steps = new Vector<String>();
        auth = null;
        running = true;
        steps.add("[[[HSRC]]]");
        try {
            Server_Socket_HTTP = new ServerSocket(JDUtilities.getSubConfig("JDRR").getIntegerProperty(JDRR.PROPERTY_PORT, 8972));
            Server_Socket_HTTPS = new ServerSocket(JDUtilities.getSubConfig("JDRR").getIntegerProperty(JDRR.PROPERTY_PORT, 8972) + 1);
            new JDRRServer(Server_Socket_HTTP, serverip, 80, false).start();
            new JDRRServer(Server_Socket_HTTPS, serverip, 443, true).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static public void stopServer() {
        running = false;
        if (steps != null) steps.add("[[[/HSRC]]]");
        try {
            Server_Socket_HTTP.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Server_Socket_HTTPS.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static public class JDRRServer extends Thread {
        ServerSocket Server_Socket = null;
        String serverip;
        int port;
        boolean ishttps = false;

        public JDRRServer(ServerSocket Server_Socket, String server, int port, boolean ishttps) {
            this.Server_Socket = Server_Socket;
            this.serverip = server;
            this.setName("JDRRServer " + port + " " + server);
            this.port = port;
            this.ishttps = ishttps;
        }

        public void run() {
            while (running) {
                Socket Client_Socket = null;
                try {
                    Client_Socket = Server_Socket.accept();
                } catch (Exception e) {
                    break;
                }
                if (running) {
                    JDRRproxy record = new JDRRproxy(Client_Socket, steps, serverip, port, ishttps);
                    record.start();
                }
            }
            running = false;
            try {
                Server_Socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
