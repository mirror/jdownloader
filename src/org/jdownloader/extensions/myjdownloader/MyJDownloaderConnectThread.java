package org.jdownloader.extensions.myjdownloader;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
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
                        if ("OK".equalsIgnoreCase(parseJDTokenResponse(jdTokenResponse, "status"))) {
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
                        // System.out.println("Connection got established");
                        closeSocket = false;
                        final Socket clientSocket = connectionSocket;
                        Thread connectionThread = new Thread("MyJDownloaderConnection:" + THREADCOUNTER.incrementAndGet()) {
                            @Override
                            public void run() {
                                try {
                                    HttpConnection httpConnection = new HttpConnection(null, clientSocket) {
                                        private byte         IV[]  = null;
                                        private byte         KEY[] = null;
                                        private OutputStream os    = null;

                                        @Override
                                        public List<HttpRequestHandler> getHandler() {
                                            return requestHandler;
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
                                        protected String preProcessRequestLine(String requestLine) throws IOException {
                                            String remove = new Regex(requestLine, "( /mjd_([a-zA-z0-9]{40})/?)").getMatch(0);
                                            String ID = new Regex(requestLine, "( /mjd_([a-zA-z0-9]{40})/?)").getMatch(1);
                                            MessageDigest md;
                                            try {
                                                md = MessageDigest.getInstance("SHA-256");
                                            } catch (NoSuchAlgorithmException e) {
                                                throw new IOException(e);
                                            }
                                            if (StringUtils.isNotEmpty(config.getEncryptionKey())) {
                                                md.update(config.getEncryptionKey().getBytes("UTF-8"));
                                            }
                                            final byte[] digest = md.digest(ID.getBytes("UTF-8"));
                                            IV = Arrays.copyOfRange(digest, 0, 16);
                                            KEY = Arrays.copyOfRange(digest, 16, 32);
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
                                                    final IvParameterSpec ivSpec = new IvParameterSpec(IV);
                                                    final SecretKeySpec skeySpec = new SecretKeySpec(KEY, "AES");
                                                    cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
                                                    /* remove content-length because we use chunked+base64+aes */
                                                    response.getResponseHeaders().remove(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH);
                                                    /* set chunked transfer header */
                                                    response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING, HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING_CHUNKED));
                                                    this.sendResponseHeaders();
                                                    this.os = new OutputStream() {
                                                        private ChunkedOutputStream chunkedOS     = new ChunkedOutputStream(clientSocket.getOutputStream());
                                                        Base64OutputStream          b64os         = new Base64OutputStream(chunkedOS) {
                                                                                                      public void close() throws IOException {
                                                                                                      };

                                                                                                  };
                                                        OutputStream                outos         = new CipherOutputStream(b64os, cipher);
                                                        boolean                     endWritten    = false;
                                                        boolean                     headerWritten = false;
                                                        {
                                                            if (useDeChunkingOutputStream) {
                                                                outos = new DeChunkingOutputStream(outos);
                                                            }
                                                        }

                                                        @Override
                                                        public void close() throws IOException {
                                                            if (headerWritten == true && endWritten == false) {
                                                                outos.close();
                                                                b64os.flush();
                                                                chunkedOS.write("\"}".getBytes("UTF-8"));
                                                                endWritten = true;
                                                            }
                                                            chunkedOS.close();
                                                        }

                                                        @Override
                                                        public void flush() throws IOException {
                                                        }

                                                        @Override
                                                        public void write(int b) throws IOException {
                                                            if (headerWritten == false) {
                                                                chunkedOS.write("{\"crypted\":\"".getBytes("UTF-8"));
                                                                headerWritten = true;
                                                            }
                                                            outos.write(b);
                                                        }

                                                        @Override
                                                        public void write(byte[] b, int off, int len) throws IOException {
                                                            if (headerWritten == false) {
                                                                chunkedOS.write("{\"crypted\":\"".getBytes("UTF-8"));
                                                                headerWritten = true;
                                                            }
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
