package org.jdownloader.extensions.myjdownloader;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Hash;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.Base64OutputStream;
import org.appwork.utils.net.ChunkedOutputStream;
import org.appwork.utils.net.DeChunkingOutputStream;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.requests.JSonRequest;
import org.appwork.utils.net.httpserver.requests.OptionsRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.api.RemoteAPIController;
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
        requestHandler = new ArrayList<HttpRequestHandler>();
        requestHandler.add(new HttpRequestHandler() {

            @Override
            public boolean onPostRequest(PostRequest request, HttpResponse response) {
                return false;
            }

            @Override
            public boolean onGetRequest(GetRequest request, HttpResponse response) {
                if (request instanceof OptionsRequest) {
                    response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
                    response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_METHODS, "GET, POST"));
                    response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_HEADERS, request.getRequestHeaders().getValue("Access-Control-Request-Headers")));
                    response.setResponseCode(ResponseCode.SUCCESS_OK);
                    return true;
                } else {
                    return false;
                }
            }
        });
        requestHandler.add(RemoteAPIController.getInstance().getRequestHandler());
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
                        final byte[] serverSecret = api.getServerSecret();
                        final Socket clientSocket = connectionSocket;
                        Thread connectionThread = new Thread("MyJDownloaderConnection:" + THREADCOUNTER.incrementAndGet()) {
                            @Override
                            public void run() {
                                try {
                                    HttpConnection httpConnection = new HttpConnection(null, clientSocket) {
                                        private OutputStream os        = null;
                                        private byte[]       aesSecret = null;

                                        @Override
                                        public List<HttpRequestHandler> getHandler() {
                                            return requestHandler;
                                        }

                                        @Override
                                        public boolean closableStreams() {
                                            return super.closableStreams();
                                        }

                                        @Override
                                        public byte[] getAESJSon_IV(String ID) {
                                            if (ID != null) {
                                                if (ID.equalsIgnoreCase("jd")) {
                                                    return Arrays.copyOfRange(aesSecret, 0, 16);
                                                } else if (ID.equalsIgnoreCase("server")) { return Arrays.copyOfRange(serverSecret, 0, 16); }
                                            }
                                            return null;
                                        }

                                        @Override
                                        public byte[] getAESJSon_KEY(String ID) {
                                            if (ID != null) {
                                                if (ID.equalsIgnoreCase("jd")) {
                                                    return Arrays.copyOfRange(aesSecret, 16, 32);
                                                } else if (ID.equalsIgnoreCase("server")) { return Arrays.copyOfRange(serverSecret, 16, 32); }
                                            }
                                            return null;
                                        }

                                        @Override
                                        protected HttpRequest buildRequest() throws IOException {
                                            HttpRequest ret = super.buildRequest();
                                            /* we do not allow gzip output */
                                            ret.getRequestHeaders().remove(HTTPConstants.HEADER_REQUEST_ACCEPT_ENCODING);
                                            return ret;
                                        }

                                        @Override
                                        public void closeConnection() {
                                            try {
                                                getOutputStream().close();
                                            } catch (final Throwable nothing) {
                                            }
                                            try {
                                                this.clientSocket.close();
                                            } catch (final Throwable nothing) {
                                            }
                                        }

                                        @Override
                                        public boolean isJSonRequestValid(JSonRequest aesJsonRequest) {
                                            if (aesJsonRequest == null) return false;
                                            if (StringUtils.isEmpty(aesJsonRequest.getUrl())) return false;
                                            /* TODO: fixme, timestamp replay */
                                            return true;
                                        }

                                        @Override
                                        protected String preProcessRequestLine(String requestLine) throws IOException {
                                            String remove = new Regex(requestLine, "( /t_([a-zA-z0-9]{40})/)").getMatch(0);
                                            String requestConnectToken = new Regex(requestLine, "( /t_([a-zA-z0-9]{40})/)").getMatch(1);
                                            try {
                                                aesSecret = api.getAESSecret(requestConnectToken);
                                                if (aesSecret == null) throw new IOException("Could not generate AESSecret " + requestLine);
                                            } catch (NoSuchAlgorithmException e) {
                                                throw new IOException(e);
                                            }
                                            requestLine = requestLine.replaceFirst(remove, " /");
                                            return requestLine;
                                        };

                                        public synchronized OutputStream getOutputStream() throws IOException {
                                            if (this.os != null) return this.os;
                                            HTTPHeader contentType = response.getResponseHeaders().get(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE);
                                            if (contentType != null && "application/json".equalsIgnoreCase(contentType.getValue())) {
                                                /* check for json response */
                                                try {
                                                    boolean deChunk = false;
                                                    HTTPHeader transferEncoding = response.getResponseHeaders().get(HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING);
                                                    if (transferEncoding != null) {
                                                        if (HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(transferEncoding.getValue())) {
                                                            deChunk = true;
                                                        } else {
                                                            throw new IOException("Unsupported TransferEncoding " + transferEncoding);
                                                        }
                                                    }
                                                    final boolean useDeChunkingOutputStream = deChunk;
                                                    final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                                    final IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(aesSecret, 0, 16));
                                                    final SecretKeySpec skeySpec = new SecretKeySpec(Arrays.copyOfRange(aesSecret, 16, 32), "AES");
                                                    cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
                                                    /* remove content-length because we use chunked+base64+aes */
                                                    response.getResponseHeaders().remove(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH);
                                                    response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "application/aesjson-jd; charset=utf-8"));
                                                    /* set chunked transfer header */
                                                    response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING, HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING_CHUNKED));
                                                    this.sendResponseHeaders();
                                                    this.os = new OutputStream() {
                                                        private ChunkedOutputStream chunkedOS = new ChunkedOutputStream(new BufferedOutputStream(clientSocket.getOutputStream(), 16384));
                                                        Base64OutputStream          b64os     = new Base64OutputStream(chunkedOS) {
                                                                                                  public void close() throws IOException {
                                                                                                  };

                                                                                              };
                                                        OutputStream                outos     = new CipherOutputStream(b64os, cipher);

                                                        {
                                                            if (useDeChunkingOutputStream) {
                                                                outos = new DeChunkingOutputStream(outos);
                                                            }
                                                        }

                                                        @Override
                                                        public void close() throws IOException {
                                                            outos.close();
                                                            b64os.flush();
                                                            chunkedOS.close();
                                                        }

                                                        @Override
                                                        public void flush() throws IOException {
                                                        }

                                                        @Override
                                                        public void write(int b) throws IOException {
                                                            outos.write(b);
                                                        }

                                                        @Override
                                                        public void write(byte[] b, int off, int len) throws IOException {
                                                            outos.write(b, off, len);
                                                        };

                                                    };
                                                } catch (final Throwable e) {
                                                    throw new IOException(e);
                                                }
                                            } else {
                                                this.sendResponseHeaders();
                                                this.os = super.os;
                                            }
                                            return this.os;
                                        }
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
