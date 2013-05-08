package org.jdownloader.extensions.myjdownloader;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.myjdownloader.api.MyJDownloaderAPI;
import org.jdownloader.myjdownloader.client.exceptions.AuthException;
import org.jdownloader.myjdownloader.client.exceptions.EmailInvalidException;
import org.jdownloader.myjdownloader.client.exceptions.EmailNotValidatedException;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderException;
import org.jdownloader.myjdownloader.client.json.DeviceData;

public class MyJDownloaderConnectThread extends Thread {

    private final AtomicLong            THREADCOUNTER  = new AtomicLong(0);
    private MyJDownloaderExtension      myJDownloaderExtension;
    private MyDownloaderExtensionConfig config;
    private Socket                      connectionSocket;
    private final MyJDownloaderAPI      api;
    private LogSource                   logger;
    private int                         backoffCounter = 0;
    private boolean                     sessionValid   = false;

    public MyJDownloaderConnectThread(MyJDownloaderExtension myJDownloaderExtension) {
        setName("MyJDownloaderConnectThread");
        this.setDaemon(true);
        this.myJDownloaderExtension = myJDownloaderExtension;
        config = myJDownloaderExtension.getSettings();
        api = new MyJDownloaderAPI(myJDownloaderExtension);
        logger = myJDownloaderExtension.getLogger();
    }

    private void backoff() throws InterruptedException {
        long timeout = 300 * 1000l;
        if (backoffCounter <= 5) {
            timeout = ((long) Math.pow(3.0d, backoffCounter)) * 1000;
        }
        timeout = Math.min(300 * 1000l, timeout);
        timeout = timeout + new Random().nextInt(5000);
        logger.info("Backoff:" + backoffCounter + "->" + timeout);
        backoffCounter++;
        Thread.sleep(timeout);
    }

    @Override
    public void run() {
        try {
            mainLoop: while (myJDownloaderExtension.getConnectThread() == this) {
                try {
                    boolean closeSocket = true;
                    try {
                        ensureValidSession();
                        connectionSocket = new Socket();
                        connectionSocket.setSoTimeout(120000);
                        connectionSocket.setTcpNoDelay(false);
                        InetSocketAddress ia = new InetSocketAddress(this.config.getAPIServerURL(), this.config.getAPIServerPort());
                        logger.info("Connect " + ia);
                        connectionSocket.connect(ia, 30000);
                        connectionSocket.getOutputStream().write(("DEVICE" + api.getSessionInfo().getSessionToken()).getBytes("ISO-8859-1"));
                        int validToken = connectionSocket.getInputStream().read();
                        if (validToken == 4 || validToken == 0 || validToken == 1) {
                            backoffCounter = 0;
                            if (validToken == 4) {
                                logger.info("KeepAlive");
                                closeSocket = true;
                            } else if (validToken == 0) {
                                logger.info("Invalid sessionToken");
                                sessionValid = false;
                                closeSocket = true;
                            } else if (validToken == 1) {
                                logger.info("valid connection");
                                closeSocket = false;
                                final Socket clientSocket = connectionSocket;
                                Thread connectionThread = new Thread("MyJDownloaderConnection:" + THREADCOUNTER.incrementAndGet()) {
                                    @Override
                                    public void run() {
                                        try {
                                            MyJDownloaderHttpConnection httpConnection = new MyJDownloaderHttpConnection(clientSocket, api);
                                            httpConnection.run();
                                        } catch (final Throwable e) {
                                            logger.log(e);
                                        }
                                    }
                                };
                                connectionThread.setDaemon(true);
                                connectionThread.start();
                                continue mainLoop;
                            }
                        } else {
                            logger.info("Something else!?!?! WTF!" + validToken);
                            backoff();
                        }
                    } catch (final MyJDownloaderException e) {
                        if (e instanceof EmailInvalidException) {
                            logger.info("Invalid email!");
                            Dialog.getInstance().showMessageDialog(0, "MyJDownloader", "Invalid email!\r\nMyJDownloader Extension is disabled now.");
                            myJDownloaderExtension.setEnabled(false);
                            return;
                        } else if (e instanceof EmailNotValidatedException) {
                            logger.info("Account is not confirmed!");
                            Dialog.getInstance().showMessageDialog(0, "MyJDownloader", "Account is not confirmed!\r\nMyJDownloader Extension is disabled now.");
                            myJDownloaderExtension.setEnabled(false);
                            return;
                        } else if (e instanceof AuthException) {
                            logger.info("Wrong Username/Password!");
                            Dialog.getInstance().showMessageDialog(0, "MyJDownloader", "Wrong Username/Password!\r\nMyJDownloader Extension is disabled now.");
                            myJDownloaderExtension.setEnabled(false);
                            return;
                        } else if (Exceptions.containsInstanceOf(e, ConnectException.class, SocketTimeoutException.class)) {
                            logger.info("Could not connect! Server down?");
                            backoff();
                        } else {
                            logger.log(e);
                            backoff();
                        }
                    } catch (ConnectException e) {
                        logger.info("Could not connect! Server down?");
                        backoff();
                    } catch (SocketTimeoutException e) {
                        logger.info("ReadTimeout on server connect!");
                    } catch (final Throwable e) {
                        logger.log(e);
                        backoff();
                    } finally {
                        try {
                            if (closeSocket) connectionSocket.close();
                        } catch (final Throwable e) {
                        }
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
        } finally {
            disconnect();
        }
    }

    public void disconnect() {
        try {
            connectionSocket.close();
        } catch (final Throwable e) {
        }
        try {
            interrupt();
        } catch (final Throwable e) {
        }
        try {
            api.disconnect();
        } catch (final Throwable e) {
        }
    }

    protected void ensureValidSession() throws MyJDownloaderException {
        if (sessionValid && api.getSessionInfo() != null) return;
        /* fetch new jdToken if needed */
        if (api.getSessionInfo() != null) {
            try {
                api.reconnect();
            } catch (MyJDownloaderException e) {
                sessionValid = false;
                api.setSessionInfo(null);
                ensureValidSession();
            }
        } else {
            api.connect(config.getEmail(), config.getPassword());
            boolean deviceBound = false;
            try {
                DeviceData device = api.bindDevice(new DeviceData(config.getUniqueDeviceID(), "jd", config.getDeviceName()));
                if (StringUtils.isNotEmpty(device.getId())) {
                    deviceBound = true;
                    config.setUniqueDeviceID(device.getId());
                }
            } finally {
                if (deviceBound == false) {
                    api.disconnect();
                }
            }
        }
        //
        sessionValid = true;
    }

    protected HashMap<String, Object> getJDToken() throws IOException {
        Browser br = new Browser();
        return JSonStorage.restoreFromString(br.getPage("http://" + config.getAPIServerURL() + ":" + config.getAPIServerPort() + "/myjdownloader/getJDToken?" + Encoding.urlEncode(config.getEmail()) + "&" + Hash.getSHA256(config.getPassword())), new TypeRef<HashMap<String, Object>>() {
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
