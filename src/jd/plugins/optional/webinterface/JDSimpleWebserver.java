package jd.plugins.optional.webinterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.logging.Logger;

import jd.config.SubConfiguration;
import jd.utils.JDUtilities;

public class JDSimpleWebserver extends Thread {

    private ServerSocket Server_Socket;

    private boolean Server_Running = true;

    private Logger logger = JDUtilities.getLogger();

    public static int CURRENT_CLIENT_COUNTER = 0;

    private static int max_clientCounter = 0;

    private static String AuthUser = "";
    private static boolean NeedAuth = false;

    public JDSimpleWebserver() {

        SubConfiguration subConfig = JDUtilities.getSubConfig("WEBINTERFACE");
        max_clientCounter = subConfig.getIntegerProperty(JDWebinterface.PROPERTY_CONNECTIONS, 10);
        AuthUser = "Basic " + JDUtilities.Base64Encode(subConfig.getStringProperty(JDWebinterface.PROPERTY_USER, "JD") + ":" + subConfig.getStringProperty(JDWebinterface.PROPERTY_PASS, "JD"));
        NeedAuth = subConfig.getBooleanProperty(JDWebinterface.PROPERTY_LOGIN, true);
        try {
            Server_Socket = new ServerSocket(subConfig.getIntegerProperty(JDWebinterface.PROPERTY_PORT, 8765));
            logger.info("Webinterface: Server started");
            start();
        } catch (IOException e) {
            logger.info("WebInterface: Server failed to start!");
        }
    }

    public void run() {
        while (Server_Running) {
            try {
                while (getCurrentClientCounter() >= max_clientCounter) {
                    try {
                        logger.info("warte");
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    ;
                }
                ;
                Socket Client_Socket = Server_Socket.accept();
                logger.info("WebInterface: Client[" + getCurrentClientCounter() + "/" + max_clientCounter + "] connecting from " + Client_Socket.getInetAddress());

                Thread client_thread = new Thread(new JDRequestHandler(Client_Socket));
                client_thread.start();

            } catch (IOException e) {
                logger.info("WebInterface: Client-Connection failed");
            }
        }
    }

    private class JDRequestHandler implements Runnable {

        private Socket Current_Socket;

        private Logger logger = JDUtilities.getLogger();

        public JDRequestHandler(Socket Client_Socket) {
            this.Current_Socket = Client_Socket;
        }

        public void run() {
            addToCurrentClientCounter(1);
            run0();
            addToCurrentClientCounter(-1);

        }

        public void run0() {
            try {
                InputStream requestInputStream = Current_Socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(requestInputStream));
                String line = null;
                HashMap<String, String> headers = new HashMap<String, String>();

                while ((line = reader.readLine()) != null && line.trim().length() > 0) {
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
                                    logger.info("get post data length " + headers.get("content-length"));
                                    Integer post_len = new Integer(headers.get("content-length"));
                                    Integer post_len_toread = new Integer(post_len);
                                    Integer post_len_read = new Integer(0);
                                    char[] cbuf = new char[post_len];
                                    Integer indexstart = 0;

                                    while (post_len_toread > 0) {
                                        if ((post_len_read = reader.read(cbuf, indexstart, post_len_toread)) == -1) break;
                                        indexstart = indexstart + post_len_read;
                                        post_len_toread = post_len_toread - post_len_read;
                                        logger.info("gelesen " + post_len_read + " index " + indexstart + " noch zu lesen " + post_len_toread);
                                    }

                                    String RequestParams = String.copyValueOf(cbuf);
                                    if (indexstart.compareTo(post_len) == 0) {
                                        /* alten POST aus Header Liste holen */
                                        String request = headers.get(null);
                                        String[] requ = request.split(" ");
                                        logger.info(requ[0]);
                                        if (Method.compareToIgnoreCase("post") == 0) {
                                            /*
                                             * alter POST aus Header Liste
                                             * entfernen
                                             */
                                            headers.remove(null);
                                            /*
                                             * FIXME: ist remove nötig
                                             */
                                            /*
                                             * neuer POST mit RequestParams in
                                             * Header-Liste einfügen
                                             */
                                            headers.put(null, requ[0] + " " + requ[1] + "?" + RequestParams + " " + requ[2]);
                                        } else
                                            logger.info("POST Daten bei nem GET aufruf???");

                                    } else {
                                        logger.info("POST Fehler postlen soll = " + post_len + " postlen gelesen = " + post_len_read);
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
                                    logger.info("pass stimmt");
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
                    logger.info("kein post oder get header");
                }
                Current_Socket.close();

            } catch (SocketException e) {
                logger.info("WebInterface: Socket error");
            } catch (IOException e) {
                logger.info("WebInterface: I/O Error");
            }
        }

    }

    /**
     * greift Threadsafe auf den clientcounter zu
     * 
     * @return
     */
    public synchronized int getCurrentClientCounter() {
        return CURRENT_CLIENT_COUNTER;
    }

    /**
     * Fügt einen Wert zum aktuellen Clientzähler hinzu
     * 
     * @param i
     * @return
     */
    public synchronized int addToCurrentClientCounter(int i) {
        CURRENT_CLIENT_COUNTER += i;
        return CURRENT_CLIENT_COUNTER;
    }

    /**
     * setzt den aktuellen Client Zähler.
     * 
     * @param current_clientCounter
     */
    public synchronized void setCurrentClientCounter(int cc) {
        CURRENT_CLIENT_COUNTER = cc;
    }

}
