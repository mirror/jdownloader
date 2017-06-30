package org.jdownloader.api.myjdownloader;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.SessionRemoteAPIRequest;
import org.appwork.remoteapi.exceptions.ApiInterfaceNotAvailable;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.Base64OutputStream;
import org.appwork.utils.net.ChunkedOutputStream;
import org.appwork.utils.net.DeChunkingOutputStream;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpconnection.SocketStreamInterface;
import org.appwork.utils.net.httpserver.EmptyRequestException;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.appwork.utils.net.httpserver.HttpConnectionExceptionHandler;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.HeadRequest;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.requests.OptionsRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.myjdownloader.api.MyJDownloaderAPI;
import org.jdownloader.myjdownloader.RequestLineParser;
import org.jdownloader.myjdownloader.client.SessionInfo;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderException;

public class MyJDownloaderHttpConnection extends HttpConnection {
    protected final static ArrayList<HttpRequestHandler>                    requestHandler = new ArrayList<HttpRequestHandler>();
    static {
        requestHandler.add(RemoteAPIController.getInstance().getRequestHandler());
    }
    protected final MyJDownloaderAPI                                        api;
    private final LogSource                                                 logger;
    private final SocketStreamInterface                                     socketStream;
    private static final HashMap<String, List<MyJDownloaderHttpConnection>> CONNECTIONS    = new HashMap<String, List<MyJDownloaderHttpConnection>>();
    private static final HashMap<String, KeyPair>                           RSAKEYPAIRS    = new HashMap<String, KeyPair>();

    public KeyPair getRSAKeyPair() {
        final String token = getRequestConnectToken();
        if (token != null) {
            KeyPair keyPair = null;
            synchronized (RSAKEYPAIRS) {
                keyPair = RSAKEYPAIRS.get(token);
                if (keyPair == null) {
                    try {
                        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                        keyPairGenerator.initialize(2048);
                        keyPair = keyPairGenerator.genKeyPair();
                        RSAKEYPAIRS.put(token, keyPair);
                    } catch (final Throwable e) {
                        getLogger().log(e);
                    }
                }
            }
            return keyPair;
        }
        return null;
    }

    public static List<MyJDownloaderHttpConnection> getConnectionsByToken(final String connectToken) {
        synchronized (CONNECTIONS) {
            return CONNECTIONS.get(connectToken);
        }
    }

    public static MyJDownloaderHttpConnection getMyJDownloaderHttpConnection(RemoteAPIRequest request) {
        if (request instanceof SessionRemoteAPIRequest<?>) {
            final Object session = ((SessionRemoteAPIRequest<?>) request).getSession();
            if (session != null && session instanceof MyJDownloaderAPISession) {
                return ((MyJDownloaderAPISession) session).getConnection();
            }
        }
        return null;
    }

    public LogSource getLogger() {
        return logger;
    }

    public MyJDownloaderHttpConnection(Socket clientConnection, MyJDownloaderAPI api) throws IOException {
        super(null, clientConnection);
        this.api = api;
        this.socketStream = null;
        logger = api.getLogger();
    }

    public MyJDownloaderHttpConnection(SocketStreamInterface socketStream, MyJDownloaderAPI api) throws IOException {
        super(null, socketStream.getSocket(), socketStream.getInputStream(), socketStream.getOutputStream());
        this.socketStream = socketStream;
        this.api = api;
        logger = api.getLogger();
    }

    public MyJDownloaderHttpConnection(final Socket clientSocket, final InputStream is, final OutputStream os, MyJDownloaderAPI api) throws IOException {
        super(null, clientSocket, is, os);
        this.socketStream = null;
        this.api = api;
        logger = api.getLogger();
    }

    protected GetRequest buildGetRequest() {
        return new MyJDownloaderGetRequest(this);
    }

    protected HeadRequest buildHeadRequest() {
        return new MyJDownloaderHeadRequest(this);
    }

    protected OptionsRequest buildOptionsRequest() {
        return new MyJDownloaderOptionsRequest(this);
    }

    protected PostRequest buildPostRequest() {
        return new MyJDownloaderPostRequest(this);
    }

    private OutputStream os = null;
    private byte[]       iv = null;

    public byte[] getIv() {
        return iv;
    }

    public byte[] getKey() {
        return key;
    }

    private byte[] key = null;

    protected void setIv(byte[] iv) {
        this.iv = iv;
    }

    protected void setKey(byte[] key) {
        this.key = key;
    }

    private String     requestConnectToken;
    private HTTPHeader accept_encoding;

    @Override
    public List<HttpRequestHandler> getHandler() {
        return requestHandler;
    }

    protected void onUnhandled(final HttpRequest request, final HttpResponse response) throws IOException {
        onException(new ApiInterfaceNotAvailable(), request, response);
    }

    @Override
    public boolean onException(Throwable e, final HttpRequest request, final HttpResponse response) throws IOException {
        if (Exceptions.containsInstanceOf(e, SocketException.class, ClosedChannelException.class)) {
            // socket already closed
            return true;
        }
        if (e instanceof HttpConnectionExceptionHandler) {
            return ((HttpConnectionExceptionHandler) e).handle(response);
        }
        final BasicRemoteAPIException apiException;
        if (!(e instanceof BasicRemoteAPIException)) {
            apiException = new InternalApiException(e);
        } else {
            apiException = (BasicRemoteAPIException) e;
        }
        logger.log(apiException);
        this.response = new HttpResponse(this);
        return apiException.handle(this.response);
    }

    public String getRequestConnectToken() {
        return requestConnectToken;
    }

    @Override
    protected HttpRequest buildRequest() throws IOException {
        final HttpRequest ret = super.buildRequest();
        ret.setBridge(MyJDownloaderController.getInstance().getConnectThread());
        /* we do not allow gzip output */
        final HTTPHeader xAcceptEncoding = ret.getRequestHeaders().get("X-Accept-Encoding");
        if (xAcceptEncoding != null && (StringUtils.containsIgnoreCase(xAcceptEncoding.getValue(), "gazeisp") || StringUtils.containsIgnoreCase(xAcceptEncoding.getValue(), "gzip_aes"))) {
            accept_encoding = xAcceptEncoding;
        } else {
            accept_encoding = ret.getRequestHeaders().get("Accept-Encoding");
        }
        ret.getRequestHeaders().remove(HTTPConstants.HEADER_REQUEST_ACCEPT_ENCODING);
        return ret;
    }

    @Override
    public void closeConnection() {
        final String token = getRequestConnectToken();
        if (token != null) {
            synchronized (CONNECTIONS) {
                List<MyJDownloaderHttpConnection> list = CONNECTIONS.get(token);
                if (list != null && list.remove(this) && list.size() == 0) {
                    CONNECTIONS.remove(token);
                }
            }
        }
        try {
            if (!clientSocket.isClosed()) {
                getOutputStream(true).close();
            }
        } catch (final Throwable nothing) {
            nothing.printStackTrace();
        }
        if (socketStream != null) {
            try {
                this.socketStream.close();
            } catch (final Throwable nothing) {
            }
        }
        try {
            this.clientSocket.close();
        } catch (final Throwable nothing) {
        }
    }

    @Override
    protected String preProcessRequestLine(String requestLine) throws IOException {
        if (StringUtils.isEmpty(requestLine)) {
            throw new EmptyRequestException();
        }
        final RequestLineParser parser = RequestLineParser.parse(requestLine.getBytes("UTF-8"));
        if (parser == null || parser.getSessionToken() == null) {
            throw new InvalidMyJDownloaderRequest();
        }
        requestConnectToken = parser.getSessionToken();
        final String token = getRequestConnectToken();
        if (token != null) {
            synchronized (CONNECTIONS) {
                List<MyJDownloaderHttpConnection> list = CONNECTIONS.get(token);
                if (list == null) {
                    list = new CopyOnWriteArrayList<MyJDownloaderHttpConnection>();
                    CONNECTIONS.put(token, list);
                }
                list.add(this);
            }
        }
        try {
            SessionInfo session = api.getSessionInfo();
            final byte[] payloadEncryptionToken;
            if (StringUtils.equals(parser.getSessionToken(), session.getSessionToken())) {
                // the request origin is the My JDownloader Server
                payloadEncryptionToken = session.getServerEncryptionToken();
            } else {
                // The request origin is a remote client
                payloadEncryptionToken = api.getDeviceEncryptionTokenBySession(parser.getSessionToken());
            }
            iv = Arrays.copyOfRange(payloadEncryptionToken, 0, 16);
            key = Arrays.copyOfRange(payloadEncryptionToken, 16, 32);
        } catch (final MyJDownloaderException e) {
            throw new IOException(e);
        }
        requestLine = requestLine.replaceFirst(" /t_[a-zA-z0-9]{40}_.+?/", " /");
        return requestLine;
    };

    @Override
    public synchronized OutputStream getOutputStream(boolean sendHeaders) throws IOException {
        if (this.os != null) {
            return this.os;
        }
        HTTPHeader contentType = response.getResponseHeaders().get(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE);
        if (contentType != null && StringUtils.startsWithCaseInsensitive(contentType.getValue(), "application/json")) {
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
                final IvParameterSpec ivSpec = new IvParameterSpec(getIv());
                final SecretKeySpec skeySpec = new SecretKeySpec(getKey(), "AES");
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
                /* remove content-length because we use chunked+base64+aes */
                response.getResponseHeaders().remove(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH);
                // response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE,
                // "application/aesjson-jd; charset=utf-8"));
                /* set chunked transfer header */
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING, HTTPConstants.HEADER_RESPONSE_TRANSFER_ENCODING_CHUNKED));
                if (accept_encoding != null && (StringUtils.containsIgnoreCase(accept_encoding.getValue(), "gazeisp") || StringUtils.containsIgnoreCase(accept_encoding.getValue(), "gzip_aes"))) {
                    /* chunked->gzip->aes */
                    if (StringUtils.containsIgnoreCase(accept_encoding.getValue(), "gazeisp")) {
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_ENCODING, "gazeisp"));
                        response.getResponseHeaders().add(new HTTPHeader("X-" + HTTPConstants.HEADER_RESPONSE_CONTENT_ENCODING, "gazeisp"));
                    } else {
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_ENCODING, "gzip_aes"));
                        response.getResponseHeaders().add(new HTTPHeader("X-" + HTTPConstants.HEADER_RESPONSE_CONTENT_ENCODING, "gzip_aes"));
                    }
                    this.sendResponseHeaders();
                    if (useDeChunkingOutputStream) {
                        this.os = new DeChunkingOutputStream(new GZIPOutputStream(new CipherOutputStream(new ChunkedOutputStream(getRawOutputStream(), 16384), cipher)));
                    } else {
                        this.os = new GZIPOutputStream(new CipherOutputStream(new ChunkedOutputStream(getRawOutputStream(), 16384), cipher));
                    }
                } else {
                    this.sendResponseHeaders();
                    this.os = new OutputStream() {
                        private ChunkedOutputStream chunkedOS = new ChunkedOutputStream(new BufferedOutputStream(getRawOutputStream(), 16384));
                        Base64OutputStream          b64os     = new Base64OutputStream(chunkedOS) {
                            // public void close() throws IOException {
                            // };
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
                }
            } catch (final Throwable e) {
                throw new IOException(e);
            }
        } else {
            if (sendHeaders) {
                this.sendResponseHeaders();
            }
            this.os = getRawOutputStream();
        }
        return this.os;
    }
}
