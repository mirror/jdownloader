package jd.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import jd.parser.Regex;

public class Socks5HTTPConnection extends HTTPConnection {

    protected Socket       socks5socket       = null;
    protected InputStream  socks5inputstream  = null;
    protected OutputStream socks5outputstream = null;
    private int            httpPort;
    private String         httpHost;

    public Socks5HTTPConnection(URL url, HTTPProxy proxy) {
        super(url, proxy);
    }

    protected int sayHello() throws IOException {
        try {
            /* socks5 */
            socks5outputstream.write((byte) 5);
            /* only none ans password/username auth method */
            socks5outputstream.write((byte) 2);
            /* none */
            socks5outputstream.write((byte) 2);
            /* username/password */
            socks5outputstream.write((byte) 0);
            socks5outputstream.flush();
            /* read response, 2 bytes */
            byte[] resp = readResponse(2);
            if (resp[0] != 5) { throw new IOException("Socks5HTTPConnection: invalid Socks5 response"); }
            if (resp[1] == 255) { throw new IOException("Socks5HTTPConnection: no acceptable authentication method found"); }
            return resp[1];
        } catch (IOException e) {
            try {
                socks5socket.close();
            } catch (Throwable e2) {
            }
            throw e;
        }
    }

    protected void authenticateProxy() throws IOException {
        try {
            String user = proxy.getUser() == null ? "" : proxy.getUser();
            String pass = proxy.getPass() == null ? "" : proxy.getPass();
            byte[] username = user.getBytes("UTF-8");
            byte[] password = pass.getBytes("UTF-8");
            /* must be 1 */
            socks5outputstream.write((byte) 1);
            /* send username */
            socks5outputstream.write((byte) username.length);
            socks5outputstream.write(username);
            /* send password */
            socks5outputstream.write((byte) password.length);
            socks5outputstream.write(password);
            /* read response, 2 bytes */
            byte[] resp = readResponse(2);
            if (resp[0] != 1) { throw new IOException("Socks5HTTPConnection: invalid Socks5 response"); }
            if (resp[1] != 0) {
                proxy.setStatus(HTTPProxy.STATUS.INVALIDAUTH);
                throw new IOException("Socks5HTTPConnection: authentication failed");
            }
        } catch (IOException e) {
            try {
                socks5socket.close();
            } catch (Throwable e2) {
            }
            throw e;
        }
    }

    protected Socket establishConnection() throws IOException {
        try {
            /* socks5 */
            socks5outputstream.write((byte) 5);
            /* tcp/ip connection */
            socks5outputstream.write((byte) 1);
            /* reserved */
            socks5outputstream.write((byte) 0);
            /* we use domain names */
            socks5outputstream.write((byte) 3);
            /* send somain name */
            byte[] domain = httpHost.getBytes("UTF-8");
            socks5outputstream.write((byte) domain.length);
            socks5outputstream.write(domain);
            /* send port */
            /* network byte order */
            socks5outputstream.write((httpPort >> 8) & 0xff);
            socks5outputstream.write(httpPort & 0xff);
            socks5outputstream.flush();
            /* read response, 4 bytes and then read rest of response */
            byte[] resp = readResponse(4);
            if (resp[0] != 5) { throw new IOException("Socks5HTTPConnection: invalid Socks5 response"); }
            switch (resp[1]) {
            case 0:
            default:
                break;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                throw new IOException("Socks5HTTPConnection: could not establish connection, status=" + resp[1]);
            }
            if (resp[3] == 1) {
                /* ip4v response */
                readResponse(4 + 2);
            } else if (resp[3] == 3) {
                /* domain name response */
                readResponse(1 + domain.length + 2);
            } else
                throw new IOException("Socks5HTTPConnection: unsupported address Type " + resp[3]);
            return socks5socket;
        } catch (IOException e) {
            try {
                socks5socket.close();
            } catch (Throwable e2) {
            }
            throw e;
        }
    }

    /* reads response with expLength bytes */
    protected byte[] readResponse(int expLength) throws IOException {
        byte[] response = new byte[expLength];
        int index = 0;
        int read = 0;
        while ((index < expLength) && (read = socks5inputstream.read()) != -1) {
            response[index] = (byte) read;
            index++;
        }
        if (index < expLength) { throw new IOException("Socks5HTTPConnection: not enough data read"); }
        return response;
    }

    @Override
    public void connect() throws IOException {
        if (isConnected()) return;/* oder fehler */
        if (proxy == null || !proxy.getType().equals(HTTPProxy.TYPE.SOCKS5)) { throw new IOException("Socks5HTTPConnection: invalid Socks5 Proxy!"); }
        /* create and connect to socks5 proxy */
        socks5socket = createSocket();
        socks5socket.setSoTimeout(readTimeout);
        long startTime = System.currentTimeMillis();
        socks5socket.connect(new InetSocketAddress(proxy.getHost(), proxy.getPort()), connectTimeout);
        socks5inputstream = socks5socket.getInputStream();
        socks5outputstream = socks5socket.getOutputStream();
        /* establish connection to socks5 */
        int method = sayHello();
        if (method == 2) {
            /* username/password authentication */
            authenticateProxy();
        }
        /* establish to destination through socks5 */
        httpPort = httpURL.getPort();
        httpHost = httpURL.getHost();
        if (httpPort == -1) httpPort = httpURL.getDefaultPort();
        Socket establishedConnection = establishConnection();
        if (httpURL.getProtocol().startsWith("https")) {
            /* we need to lay ssl over normal socks5 connection */
            SSLSocket sslSocket = null;
            try {
                SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                sslSocket = (SSLSocket) socketFactory.createSocket(establishedConnection, httpHost, httpPort, true);
                sslSocket.startHandshake();
            } catch (SSLHandshakeException e) {
                try {
                    socks5socket.close();
                } catch (Throwable e2) {
                }
                throw new IOException("Socks5HTTPConnection: " + e);
            }
            httpSocket = sslSocket;
        } else {
            /* we can continue to use the socks5 connection */
            httpSocket = establishedConnection;
        }
        httpResponseCode = -1;
        requestTime = System.currentTimeMillis() - startTime;
        httpPath = new Regex(httpURL.toString(), "https?://.*?(/.+)").getMatch(0);
        if (httpPath == null) httpPath = "/";
        /* now send Request */
        StringBuilder sb = new StringBuilder();
        sb.append(httpMethod.name()).append(' ').append(httpPath).append(" HTTP/1.1\r\n");
        for (String key : this.requestProperties.keySet()) {
            if (requestProperties.get(key) == null) continue;
            sb.append(key).append(": ").append(requestProperties.get(key)).append("\r\n");
        }
        sb.append("\r\n");
        httpSocket.getOutputStream().write(sb.toString().getBytes("UTF-8"));
        httpSocket.getOutputStream().flush();
        if (httpMethod != METHOD.POST) {
            outputClosed = true;
            connectInputStream();
        }
    }

    @Override
    public void disconnect() {
        if (isConnected()) {
            try {
                httpSocket.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        try {
            socks5socket.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
