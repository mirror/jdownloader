package org.jdownloader.extensions.myjdownloader;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.jdownloader.extensions.myjdownloader.api.MyJDownloaderAPI;

public class MyJDownloaderConnectThread extends Thread {

    private final AtomicLong              THREADCOUNTER = new AtomicLong(0);
    private MyJDownloaderExtension        myJDownloaderExtension;
    private int                           loginError    = 0;
    private int                           connectError  = 0;
    private MyDownloaderExtensionConfig   config;
    private Socket                        connectionSocket;
    private ArrayList<HttpRequestHandler> requestHandler;
    private final MyJDownloaderAPI        api;

    public MyJDownloaderConnectThread(MyJDownloaderExtension myJDownloaderExtension) {
        setName("MyJDownloaderConnectThread");
        this.setDaemon(true);
        this.myJDownloaderExtension = myJDownloaderExtension;
        config = myJDownloaderExtension.getSettings();
        api = new MyJDownloaderAPI(config);
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
                    String connectToken = api.getConnectToken();
                    if (StringUtils.isEmpty(connectToken)) { throw new WTFException("Login failed"); }
                } catch (final Throwable e) {
                    loginError++;
                    e.printStackTrace();
                    Thread.sleep(1000);
                    continue mainLoop;
                }
                boolean closeSocket = true;
                try {
                    connectionSocket = new Socket();
                    connectionSocket.setSoTimeout(120000);
                    connectionSocket.setTcpNoDelay(false);
                    connectionSocket.connect(new InetSocketAddress(this.config.getAPIServerURL(), this.config.getAPIServerPort()), 30000);
                    connectionSocket.getOutputStream().write(("JD" + api.getConnectToken()).getBytes("ISO-8859-1"));
                    int validToken = connectionSocket.getInputStream().read();
                    if (validToken == 4) {
                        System.out.println("KeepAlive");
                        closeSocket = true;
                    } else if (validToken == 0) {
                        loginError++;
                        System.out.println("Token seems to be invalid!");
                        api.invalidateConnectToken();
                    } else if (validToken == 1) {
                        connectError = 0;
                        // System.out.println("Connection got established");
                        closeSocket = false;

                        final Socket clientSocket = connectionSocket;
                        Thread connectionThread = new Thread("MyJDownloaderConnection:" + THREADCOUNTER.incrementAndGet()) {
                            @Override
                            public void run() {
                                try {
                                    MyJDownloaderHttpConnection httpConnection = new MyJDownloaderHttpConnection(clientSocket, api);
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
                    System.out.println("ReadTimeout!");
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
        return JSonStorage.restoreFromString(br.getPage("http://" + config.getAPIServerURL() + ":" + config.getAPIServerPort() + "/myjdownloader/getJDToken?" + Encoding.urlEncode(config.getUsername()) + "&" + Hash.getSHA256(config.getPassword())), new TypeRef<HashMap<String, Object>>() {
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
