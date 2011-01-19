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

package jd.controlling.reconnect.plugins.liveheader.recorder;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;

import org.appwork.utils.Regex;

public class ReconnectRecorder {

    static public Vector<String> steps;
    static boolean               running       = false;
    static ServerSocket          Server_Socket_HTTP;
    static ServerSocket          Server_Socket_HTTPS;
    static final String          PROPERTY_PORT = "PARAM_PORT";
    static String                AUTH;

    static public void startServer(String serverip, final boolean rawmode) {
        steps = new Vector<String>();
        AUTH = null;
        running = true;
        steps.add("[[[HSRC]]]");
        int port = 80;
        try {
            Server_Socket_HTTP = new ServerSocket(SubConfiguration.getConfig("ReconnectRecorder").getIntegerProperty(ReconnectRecorder.PROPERTY_PORT, 8972));
            Server_Socket_HTTPS = new ServerSocket(SubConfiguration.getConfig("ReconnectRecorder").getIntegerProperty(ReconnectRecorder.PROPERTY_PORT, 8972) + 1);
            if (serverip.contains(":")) {
                final String ports = new Regex(serverip, ".*?:(\\d+)").getMatch(0);
                port = Integer.parseInt(ports);
                serverip = new Regex(serverip, "(.*?):").getMatch(0);
            }
            new JDRRServer(Server_Socket_HTTP, serverip, port, false, rawmode).start();
            new JDRRServer(Server_Socket_HTTPS, serverip, 443, true, rawmode).start();
        } catch (Exception e) {
            JDLogger.exception(e);
        }
    }

    static public void stopServer() {
        if (running == false) return;
        running = false;
        if (steps != null) {
            steps.add("[[[/HSRC]]]");
        }
        try {
            Server_Socket_HTTP.close();
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        try {
            Server_Socket_HTTPS.close();
        } catch (Exception e) {
            JDLogger.exception(e);
        }
    }

    static public class JDRRServer extends Thread {
        final ServerSocket Server_Socket; // = null;
        final String       serverip;
        final int          port;
        final boolean      ishttps;      // = false;
        final boolean      israw;        // = false;

        public JDRRServer(final ServerSocket Server_Socket, final String server, final int port, final boolean ishttps, final boolean israw) {
            this.Server_Socket = Server_Socket;
            this.serverip = server;
            this.setName("JDRRServer " + port + " " + server);
            this.port = port;
            this.ishttps = ishttps;
            this.israw = israw;
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
                    (new Proxy(Client_Socket, steps, serverip, port, ishttps, israw)).start();
                }
            }
            running = false;
            try {
                Server_Socket.close();
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
    }

}
