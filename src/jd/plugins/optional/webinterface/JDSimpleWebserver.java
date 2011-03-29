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

package jd.plugins.optional.webinterface;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDIO;
import jd.plugins.optional.interfaces.HttpServer;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;

public class JDSimpleWebserver extends Thread {

    private class JDRequestHandler implements Runnable {

        private Socket Current_Socket;

        private Logger logger = jd.controlling.JDLogger.getLogger();

        public JDRequestHandler(Socket Client_Socket) {
            Current_Socket = Client_Socket;
        }

        public String readline(BufferedInputStream in) {
            StringBuilder data = new StringBuilder("");
            int c;
            try {
                in.mark(1);
                if (in.read() == -1)
                    return null;
                else
                    in.reset();
                while ((c = in.read()) >= 0) {
                    if ((c == 0) || (c == 10) || (c == 13))
                        break;
                    else
                        data.append((char) c);
                }
                if (c == 13) {
                    in.mark(1);
                    if (in.read() != 10) in.reset();
                }
            } catch (Exception e) {
            }
            return data.toString();
        }

        public void run() {
            try {
                InputStream requestInputStream = Current_Socket.getInputStream();
                BufferedInputStream reader = new BufferedInputStream(requestInputStream);

                String line = null;
                HashMap<String, String> headers = new HashMap<String, String>();

                while ((line = readline(reader)) != null && line.trim().length() > 0) {
                    String key = null;
                    String value = null;
                    if (line.indexOf(": ") > 0) {
                        key = line.substring(0, line.indexOf(": ")).toLowerCase();
                        value = line.substring(line.indexOf(": ") + 2);
                    } else {
                        key = null;
                        value = line;
                    }
                    headers.put(key, value);
                }

                if (headers.containsKey(null)) {
                    String Method = headers.get(null).split(" ")[0];
                    if (Method.compareToIgnoreCase("get") == 0 || Method.compareToIgnoreCase("post") == 0) {
                        /* get oder post header gefunden */
                        if (headers.containsKey("content-type")) {
                            if (headers.get("content-type").compareToIgnoreCase("application/x-www-form-urlencoded") == 0) {
                                if (headers.containsKey("content-length")) {
                                    /*
                                     * POST Form Daten in GET Format übersetzen,
                                     * damit der RequestParams Parser nicht
                                     * geändert werden muss
                                     */
                                    int post_len = new Integer(headers.get("content-length"));
                                    int post_len_toread = post_len;
                                    int post_len_read = 0;
                                    byte[] cbuf = new byte[post_len];
                                    int indexstart = 0;
                                    while (post_len_toread > 0) {
                                        if ((post_len_read = reader.read(cbuf, indexstart, post_len_toread)) == -1) {
                                            break;
                                        }
                                        indexstart = indexstart + post_len_read;
                                        post_len_toread = post_len_toread - post_len_read;
                                    }
                                    String RequestParams = new String(cbuf).trim();
                                    if (indexstart == post_len) {
                                        /*
                                         * alten POST aus Header Liste holen,
                                         * neuen zusammenbauen
                                         */
                                        String request = headers.get(null);
                                        String[] requ = request.split(" ");
                                        if (Method.compareToIgnoreCase("post") == 0) {
                                            headers.put(null, requ[0] + " " + requ[1] + "?" + RequestParams + " " + requ[2]);
                                        } else {
                                            logger.severe("POST Daten bei nem GET aufruf???");
                                        }
                                    } else {
                                        logger.severe("POST Fehler postlen soll = " + post_len + " postlen gelesen = " + post_len_read);
                                    }

                                }
                            } else if (headers.get("content-type").contains("multipart/form-data")) {
                                /*
                                 * POST Form Daten in GET Format übersetzen,
                                 * damit der RequestParams Parser nicht geändert
                                 * werden muss
                                 * 
                                 * Zusätzlich das File auslesen (die komplette
                                 * Verarbeiten findet auf Hex statt!!)
                                 */
                                if (headers.containsKey("content-length")) {
                                    int post_len = new Integer(headers.get("content-length"));
                                    int post_len_toread = post_len;
                                    int post_len_read = 0;
                                    byte[] cbuf = new byte[post_len];
                                    int indexstart = 0;
                                    String limiter = new Regex(headers.get("content-type"), Pattern.compile("boundary=(.*)", Pattern.CASE_INSENSITIVE)).getMatch(0);
                                    if (limiter != null) {
                                        /*
                                         * nur weitermachen falls ein limiter
                                         * vorhanden ist
                                         */
                                        limiter = "--" + limiter;
                                        limiter = JDHexUtils.getHexString(limiter);
                                        while (post_len_toread > 0) {
                                            if ((post_len_read = reader.read(cbuf, indexstart, post_len_toread)) == -1) {
                                                break;
                                            }
                                            indexstart = indexstart + post_len_read;
                                            post_len_toread = post_len_toread - post_len_read;
                                        }
                                        if (indexstart == post_len) {
                                            String RequestParams = "";
                                            /*
                                             * momentan wird multipart nur für
                                             * containerupload genutzt, daher
                                             * form-data parsing unnötig
                                             */
                                            String MultiPartData[][] = new Regex(JDHexUtils.getHexString(cbuf), Pattern.compile(limiter + JDHexUtils.getHexString("\r") + "{0,1}" + JDHexUtils.getHexString("\n") + "{0,1}" + JDHexUtils.REGEX_MATCH_ALL_HEX + "(?=" + "" + JDHexUtils.getHexString("\r") + "{0,1}" + JDHexUtils.getHexString("\n") + "{0,1}" + limiter + ")", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatches();
                                            for (String[] element : MultiPartData) {
                                                if (element[0].contains(JDHexUtils.getHexString("Content-Disposition: form-data; name=\"container\""))) {
                                                    String containertyp = new Regex(element[0], Pattern.compile(JDHexUtils.getHexString("filename=\"") + JDHexUtils.REGEX_FIND_ALL_HEX + JDHexUtils.getHexString(".") + JDHexUtils.REGEX_MATCH_ALL_HEX + JDHexUtils.getHexString("\""), Pattern.CASE_INSENSITIVE)).getMatch(0);
                                                    if (containertyp != null) {
                                                        containertyp = new String(JDHexUtils.getByteArray(containertyp));
                                                    }
                                                    if (containertyp != null && (containertyp.contains("dlc") || containertyp.contains("ccf") || containertyp.contains("rsdf") || containertyp.contains("jdc"))) {
                                                        File containerfile = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containertyp);
                                                        if (JDIO.saveToFile(containerfile, JDHexUtils.getByteArray(element[0].substring(element[0].indexOf(JDHexUtils.getHexString("\r\n\r\n")) + 8)))) {
                                                            /*
                                                             * RequestParameter
                                                             * zusammenbauen
                                                             */
                                                            RequestParams = "do=Upload&file=" + Encoding.urlEncode(containerfile.getName());
                                                            break;
                                                        }
                                                    } else {
                                                        if (containertyp != null) {
                                                            logger.severe("unknown container typ: " + containertyp);
                                                        }
                                                    }
                                                }
                                            }
                                            /*
                                             * alten POST aus Header Liste
                                             * holen, neuen zusammenbauen
                                             */
                                            String request = headers.get(null);
                                            String[] requ = request.split(" ");
                                            if (Method.compareToIgnoreCase("post") == 0) {
                                                headers.put(null, requ[0] + " " + requ[1] + "?" + RequestParams + " " + requ[2]);
                                            } else {
                                                logger.severe("POST Daten bei nem GET aufruf???");
                                            }
                                        } else {
                                            logger.severe("POST Fehler postlen soll = " + post_len + " postlen gelesen = " + post_len_read);
                                        }
                                    }
                                }
                            }
                        }

                        JDSimpleWebserverResponseCreator response = new JDSimpleWebserverResponseCreator();
                        JDSimpleWebserverRequestHandler request = new JDSimpleWebserverRequestHandler(headers, response);
                        OutputStream outputStream = Current_Socket.getOutputStream();
                        if (NeedAuth == true) {/* need authorization */
                            if (headers.containsKey("authorization")) {
                                if (JDSimpleWebserver.AuthUser.equals(headers.get("authorization"))) {
                                    /*
                                     * send authorization granted
                                     */
                                    /* logger.info("pass stimmt"); */
                                    request.handle();

                                } else { /* send authorization failed */
                                    response.setAuth_failed();
                                }
                            } else { /* send autorization needed */
                                response.setAuth_needed();
                            }
                        } else { /* no autorization needed */
                            request.handle();
                        }

                        response.writeToStream(outputStream);
                        outputStream.close();
                    }
                } else {
                    /* kein get oder post header */
                    logger.severe("kein post oder get header");
                }
                Current_Socket.close();

            } catch (SocketException e) {
                logger.severe("WebInterface: Socket error");
            } catch (IOException e) {
                logger.severe("WebInterface: I/O Error");
            }
        }
    }

    private static String AuthUser = "";

    private static boolean NeedAuth = false;

    private Logger logger = jd.controlling.JDLogger.getLogger();
    private boolean Server_Running = true;

    private ServerSocket Server_Socket;

    public SSLServerSocketFactory setupSSL() throws Exception {
        char[] password = "jdwebinterface".toCharArray();
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream fis = new FileInputStream(JDUtilities.getResourceFile("plugins/webinterface/jdwebinterface").getAbsolutePath());
        ks.load(fis, password);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        SSLContext c = SSLContext.getInstance("SSL");
        c.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        SSLServerSocketFactory sf = c.getServerSocketFactory();
        return sf;
    }

    public JDSimpleWebserver() {
        SubConfiguration subConfig = SubConfiguration.getConfig("WEBINTERFACE");
        Boolean https = subConfig.getBooleanProperty(WebinterfaceClassicExtension.PROPERTY_HTTPS, false);
        AuthUser = "Basic " + Encoding.Base64Encode(subConfig.getStringProperty(WebinterfaceClassicExtension.PROPERTY_USER, "JD") + ":" + subConfig.getStringProperty(WebinterfaceClassicExtension.PROPERTY_PASS, "JD"));
        NeedAuth = subConfig.getBooleanProperty(WebinterfaceClassicExtension.PROPERTY_LOGIN, true);
        boolean localhostonly = subConfig.getBooleanProperty(WebinterfaceClassicExtension.PROPERTY_LOCALHOST_ONLY, false);
        int port = subConfig.getIntegerProperty(WebinterfaceClassicExtension.PROPERTY_PORT, 8765);
        try {
            if (!https) {
                if (localhostonly) {
                    Server_Socket = new ServerSocket(port, -1, HttpServer.getLocalHost());
                } else {
                    Server_Socket = new ServerSocket(port);
                }
            } else {
                try {
                    ServerSocketFactory ssocketFactory = setupSSL();

                    if (localhostonly) {
                        Server_Socket = ssocketFactory.createServerSocket(port, -1, HttpServer.getLocalHost());
                    } else {
                        Server_Socket = ssocketFactory.createServerSocket(port);
                    }
                } catch (Exception e) {
                    logger.severe("WebInterface: Server failed to start (SSL Setup Failed)!");
                    return;
                }
            }
            logger.info("Webinterface: Server started");
            start();
        } catch (IOException e) {
            logger.severe("WebInterface: Server failed to start!");
        }
    }

    // @Override
    public void run() {
        this.setName("Webinterface");
        while (Server_Running) {
            try {
                Socket Client_Socket = Server_Socket.accept();
                Thread client_thread = new Thread(new JDRequestHandler(Client_Socket));
                client_thread.start();
            } catch (IOException e) {
                JDLogger.exception(e);
                logger.severe("WebInterface: Client-Connection failed");
            }
        }
    }

}
