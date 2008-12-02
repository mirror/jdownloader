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

package jd.nutils.httpserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer extends Thread {
    private ServerSocket ssocket;
    private Socket csocket;
    private Handler handler;
    private boolean running = true;

    public HttpServer(int port, Handler handler) {
        try {
            ssocket = new ServerSocket(port);
            this.handler = handler;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    public void serverstop() {
        running = false;
        suspend();
    }

    public void run() {
        while (running) {
            try {
                csocket = ssocket.accept();
                new RequestHandler(csocket, handler).run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isStarted() {
        return running;
    }
}