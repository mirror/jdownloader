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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import jd.controlling.JDLogger;

public class RequestHandler extends Thread {

    /**
     * The CR/LF pair.
     */
    private static final String CRLF = "\r\n";

    /**
     * The CR octet.
     */
    private static final int CR = 13;

    /**
     * The LF octet.
     */
    private static final int LF = 10;

    private final Socket socket;
    private final Handler handler;
    private boolean eof = false;

    public RequestHandler(final Socket socket, final Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    public void run() {
        try {
            final BufferedInputStream reader = new BufferedInputStream(socket.getInputStream());
            String line = "";

            final Request req = new Request();

            while (!eof && (line = readline(reader)) != null) {
                String key = null;
                String value = null;
                if (line.equals(CRLF) && req.getRequestType().equals("GET")) {
                    eof = true;
                    continue;
                }

                if (line.equals(CRLF) && req.getRequestType().equals("POST")) {
                    // TODO: only post data < 2 gb (int range) possible
                    final int data = (int) req.getContentLength();
                    final byte[] buffer = new byte[data];
                    int offset = 0;
                    while (data - offset > 0) {
                        offset += reader.read(buffer, offset, data - offset);
                    }
                    req.setPostData(buffer);
                    this.parseParameter(req, new String(buffer));
                    eof = true;
                    continue;
                }
                line = line.trim();
                if (line.startsWith("GET ") || line.startsWith("POST ")) {
                    final String[] help = line.split(" ");
                    req.setRequestType(help[0]);
                    req.setHttpType(help[2]);

                    if (help[1].indexOf("?") > 0) {
                        req.setRequestUrl(help[1].substring(0, help[1].indexOf("?")));
                        parseParameter(req, help[1].substring(help[1].indexOf("?") + 1));
                    } else {
                        req.setRequestUrl(help[1]);
                    }
                } else if (line.indexOf(": ") > 0) {
                    key = line.substring(0, line.indexOf(": ")).toLowerCase();
                    value = line.substring(line.indexOf(": ") + 2);
                }
                if (key != null) {
                    req.addHeader(key, value);
                }
            }

            final Response res = new Response();
            try {
                handler.handle(req, res);
            } catch (Exception e) {
                JDLogger.exception(e);
                res.setReturnStatus(Response.ERROR);
                res.addContent(e.toString());
            }
            final OutputStream out = socket.getOutputStream();
            res.writeToStream(out);
            out.close();

        } catch (IOException e) {
            JDLogger.exception(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                JDLogger.exception(e);
            }
        }
    }

    public void parseParameter(final Request req, final String parameter) {
        final String[] help = parameter.split("\\&");

        for (String entry : help) {
            entry = entry.trim();
            final int index = entry.indexOf("=");

            if (index > 0) {
                req.addParameter(entry.substring(0, index), entry.substring(index + 1));
            } else {
                req.addParameter(entry, "");
            }
        }
    }

    private String readline(final BufferedInputStream reader) {
        final StringBuilder ret = new StringBuilder();

        int byteread = -6;
        try {

            while ((byteread = reader.read()) != -1) {

                if (byteread == LF || byteread == CR) {

                    reader.mark(0);

                    ret.append((char) byteread);
                    if ((byteread = reader.read()) != -1) {

                        if (byteread == CR || byteread == LF) {

                            ret.append((char) byteread);
                            break;
                        } else {
                            reader.reset();
                            break;
                        }
                    }

                }

                ret.append((char) byteread);

            }
            if (byteread == -1) {
                eof = true;
            }
        } catch (IOException e) {
            JDLogger.exception(e);
        }

        return ret.toString();
    }
}