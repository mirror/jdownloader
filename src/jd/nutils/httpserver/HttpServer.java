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

package jd.nutils.httpserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import jd.controlling.JDLogger;

public class HttpServer extends Thread {
    private ServerSocket ssocket;
    private Socket csocket;
    private Handler handler;
    private boolean running = true;
    private Thread run;
    private int port;
    private boolean uselocalhost = false;

    public HttpServer(int port, Handler handler) throws IOException {
        super("HTTP-Server");
        this.handler = handler;
        this.port = port;
        this.uselocalhost = false;
    }

    public HttpServer(int port, Handler handler, boolean localhost) throws IOException {
        super("HTTP-Server");
        this.handler = handler;
        this.port = port;
        this.uselocalhost = localhost;
    }

    public void sstop() throws IOException {
        running = false;
        run = null;
    }

    public void start() {
        running = true;
        try {
            InetAddress localhost = getLocalHost();
            if (localhost != null && uselocalhost) {
                ssocket = new ServerSocket(port, 5, localhost);
            } else {
                ssocket = new ServerSocket(port, 5);
            }
            // ssocket.setSoTimeout(1000);
        } catch (IOException e) {
            JDLogger.exception(e);
        }
        run = new Thread(this, "Http-Server Consumer");
        run.start();
    }

    public static InetAddress getLocalHost() {
        InetAddress localhost = null;
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            /*
             * fallback to loopback if host has no dns entry in local dns table
             */
            try {
                localhost = InetAddress.getByName(null);
            } catch (UnknownHostException e1) {
                JDLogger.getLogger().severe("could not find localhost!");
            }
        }
        return localhost;
    }

    public void run() {
        Thread thisThread = Thread.currentThread();
        while (run == thisThread && running) {
            if (ssocket == null) return;
            try {
                csocket = ssocket.accept();
                new RequestHandler(csocket, handler).run();
                Thread.sleep(100);
            } catch (Exception e) {
                JDLogger.exception(e);
            }

        }

        try {
            ssocket.close();
        } catch (IOException e) {
            JDLogger.exception(e);
        }
    }

    public boolean isStarted() {
        return running;
    }
}