package org.jdownloader.extensions.myjdownloader;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Hash;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.jdownloader.api.RemoteAPIController;

public class MyJDownloaderConnectThread extends Thread {

    private final AtomicLong              THREADCOUNTER = new AtomicLong(0);
    private MyJDownloaderExtension        myJDownloaderExtension;
    private String                        jdToken       = null;
    private int                           loginError    = 0;
    private int                           connectError  = 0;
    private MyDownloaderExtensionConfig   config;
    private Socket                        connectionSocket;
    private ArrayList<HttpRequestHandler> requestHandler;

    public MyJDownloaderConnectThread(MyJDownloaderExtension myJDownloaderExtension) {
        setName("MyJDownloaderConnectThread");
        this.setDaemon(true);
        this.myJDownloaderExtension = myJDownloaderExtension;
        config = myJDownloaderExtension.getConfig();
        requestHandler = new ArrayList<HttpRequestHandler>();
        requestHandler.add(RemoteAPIController.getInstance().getRequestHandler());
    }

    @Override
    public void run() {
        mainLoop: while (myJDownloaderExtension.getConnectThread() == this) {
            try {
                if (loginError > 5 || connectError > 5) {
                    try {
                        myJDownloaderExtension.setEnabled(false);
                    } catch (final Throwable e) {
                        e.printStackTrace();
                    }
                    return;
                }
                try {
                    /* fetch new jdToken if needed */
                    if (jdToken == null) {
                        HashMap<String, Object> jdTokenResponse = getJDToken();
                        ((MyJDownloaderConfigPanel) myJDownloaderExtension.getConfigPanel()).setCurrentIP(parseJDTokenResponse(jdTokenResponse, "ip"));
                        if ("OK".equalsIgnoreCase(parseJDTokenResponse(jdTokenResponse, "responseCode"))) {
                            jdToken = parseJDTokenResponse(jdTokenResponse, "token");
                            loginError = 0;
                        }
                        if (StringUtils.isEmpty(jdToken)) { throw new WTFException("Login failed"); }
                    }
                } catch (final Throwable e) {
                    loginError++;
                    e.printStackTrace();
                    Thread.sleep(1000);
                    continue mainLoop;
                }
                boolean closeSocket = true;
                try {
                    connectionSocket = new Socket();
                    connectionSocket.setSoTimeout(60000);
                    connectionSocket.connect(new InetSocketAddress(this.config.getAPIURL(), this.config.getAPIPort()), 30000);
                    connectionSocket.getOutputStream().write(("JD" + jdToken).getBytes("ISO-8859-1"));
                    int validToken = connectionSocket.getInputStream().read();
                    if (validToken == 0) {
                        loginError++;
                        System.out.println("Token seems to be invalid!");
                        jdToken = null;
                    } else if (validToken == 1) {
                        connectError = 0;
                        System.out.println("Connection got established");
                        closeSocket = false;
                        final Socket clientSocket = connectionSocket;
                        Thread connectionThread = new Thread("MyJDownloaderConnection:" + THREADCOUNTER.incrementAndGet()) {
                            @Override
                            public void run() {
                                try {
                                    HttpConnection httpConnection = new HttpConnection(null, clientSocket) {
                                        @Override
                                        public List<HttpRequestHandler> getHandler() {
                                            return requestHandler;
                                        }

                                        @Override
                                        protected HttpRequest buildRequest() throws IOException {
                                            HttpRequest ret = super.buildRequest();
                                            return ret;
                                        }

                                        @Override
                                        protected String preProcessRequestLine(String requestLine) {
                                            String ID = new Regex(requestLine, "( /mjd_[a-zA-z0-9]{40}/?)").getMatch(0);
                                            requestLine = requestLine.replaceFirst(ID, " /");
                                            return requestLine;
                                        };
                                    };
                                    httpConnection.run();
                                } catch (final Throwable e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        connectionThread.setDaemon(true);
                        connectionThread.start();
                    } else {
                        System.out.println("Something else!?!?! WTF!" + validToken);
                    }
                } catch (ConnectException e) {
                    System.out.println("Could not connect! Server down?");
                    connectError++;
                } catch (SocketTimeoutException e) {
                    System.out.println("ReadTimeout! No possible Connection found?");
                } finally {
                    try {
                        if (closeSocket) connectionSocket.close();
                    } catch (final Throwable e) {
                    }
                }

            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public void interruptConnectThread() {
        try {
            connectionSocket.close();
        } catch (final Throwable e) {
        }
    }

    protected HashMap<String, Object> getJDToken() throws IOException {
        Browser br = new Browser();
        return JSonStorage.restoreFromString(br.getPage("http://" + config.getAPIURL() + ":" + config.getAPIPort() + "/myjdownloader/getJDToken?" + Encoding.urlEncode(config.getUsername()) + "&" + Hash.getSHA256(config.getPassword())), new TypeRef<HashMap<String, Object>>() {
        });
    }

    protected String parseJDTokenResponse(HashMap<String, Object> apiResponse, String field) {
        String ret = null;
        if (apiResponse != null) {
            Object data = apiResponse.get("data");
            if (data != null && data instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) data;
                return (String) dataMap.get(field);
            }
        }
        return ret;

    }

}
