package jd.nutils;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import jd.http.SocketConnectionFactory;

import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.logging.LogController;

public class SimpleUseNet {

    public static enum COMMAND {
        HEAD {

            @Override
            public boolean isMultiLineResponse(int code) {
                switch (code) {
                case 221:
                    return true;
                }
                return false;
            }

        },
        AUTHINFO_USER {

            @Override
            public String getCommand() {
                return "AUTHINFO USER";
            }
        },
        AUTHINFO_PASS {

            @Override
            public String getCommand() {
                return "AUTHINFO PASS";
            }
        },
        BODY,
        QUIT;

        public boolean isMultiLineResponse(int code) {
            return false;
        };

        public String getCommand() {
            return this.name();
        }
    }

    /**
     * rfc3977 nntp
     */

    private final Socket socket;

    private static class CommandResponse {
        private final int responseCode;

        public int getResponseCode() {
            return responseCode;
        }

        public String getMessage() {
            return message;
        }

        private final String message;

        private CommandResponse(final int responseCode, final String message) {
            this.responseCode = responseCode;
            this.message = message;
        }

        @Override
        public String toString() {
            return getResponseCode() + ":" + getMessage();
        }

    }

    public Socket getSocket() {
        return socket;
    }

    private OutputStream    outputStream = null;
    private InputStream     inputStream  = null;
    private final byte[]    CRLF         = "\r\n".getBytes();

    private final LogSource logger;

    public SimpleUseNet(HTTPProxy proxy, LogSource logger) {
        socket = SocketConnectionFactory.createSocket(proxy);
        if (logger != null) {
            this.logger = logger;
        } else {
            this.logger = LogController.TRASH;
        }
    }

    public synchronized void connect(String server, int port, boolean ssl, String username, String password) throws IOException {
        connect(new InetSocketAddress(server, port), ssl, username, password);
    }

    public synchronized void connect(SocketAddress socketAddress, boolean ssl, String username, String password) throws IOException {
        try {
            socket.connect(socketAddress);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            CommandResponse response = readCommandResponse(null);
            switch (response.getResponseCode()) {
            case 200:
                // Service available, posting allowed
            case 201:
                // Service available, posting prohibited
                break;
            case 400:
                throw new IOException("Service temporarily unavailable");
            case 500:
                throw new IOException("Service permanently unavailable");
            default:
                throw new IOException("Unknown Response: " + response);
            }
            if (username != null || password != null) {
                authenticate(username, password);
            }
        } catch (final IOException e) {
            silentDisconnect();
            throw e;
        }
    }

    private void authenticate(String username, String password) throws IOException {
        final String user = username != null ? username : "";
        CommandResponse response = sendCmd(COMMAND.AUTHINFO_USER, user);
        switch (response.getResponseCode()) {
        case 281:
            // no pass required
            return;
        case 381:
            // pass required
            final String pass = password != null ? password : "";
            response = sendCmd(COMMAND.AUTHINFO_PASS, pass);
            switch (response.getResponseCode()) {
            case 281:
                // user/pass correct
                return;
            case 481:
                // user/pass incorrect
                throw new IOException("User/Pass invalid?!");
            }
        }
        throw new IOException("Unknown Response: " + response);
    }

    private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream() {
                                                       @Override
                                                       public synchronized byte[] toByteArray() {
                                                           return buf;
                                                       };
                                                   };

    protected synchronized String readLine() throws IOException {
        return readLine(lineBuffer);
    }

    protected synchronized String readLine(final ByteArrayOutputStream buffer) throws IOException {
        try {
            buffer.reset();
            final int length = readLine(getInputStream(), buffer);
            if (length == -1) {
                throw new EOFException();
            }
            final String ret = new String(buffer.toByteArray(), 0, length, "ISO-8859-1");
            logger.info("Read Response:" + ret);
            return ret;
        } catch (final IOException e) {
            silentDisconnect();
            throw e;
        }
    }

    protected int readLine(final InputStream inputStream, final OutputStream buffer) throws IOException {
        try {
            int c = 0;
            int length = 0;
            boolean CR = false;
            while (true) {
                c = inputStream.read();
                if (c == -1) {
                    if (length > 0) {
                        return length;
                    }
                    return -1;
                } else if (c == 13) {
                    if (CR) {
                        throw new IOException("CRCR!?");
                    } else {
                        CR = true;
                    }
                } else if (c == 10) {
                    if (CR) {
                        break;
                    } else {
                        throw new IOException("LF!?");
                    }
                } else {
                    if (CR) {
                        throw new IOException("CRXX!?");
                    }
                    buffer.write(c);
                    length++;
                }
            }
            return length;
        } catch (final IOException e) {
            silentDisconnect();
            throw e;
        }
    }

    private synchronized CommandResponse readCommandResponse(COMMAND command) throws IOException {
        String line = readLine();
        final int code = Integer.parseInt(line.substring(0, 3));
        if (command != null && command.isMultiLineResponse(code)) {
            final StringBuilder sb = new StringBuilder();
            sb.append(line);
            while (true) {
                line = readLine();
                if (".".equals(line)) {
                    break;
                } else {
                    sb.append("\r\n");
                    sb.append(line);
                }
            }
            return new CommandResponse(code, sb.toString());
        } else {
            return new CommandResponse(code, line.substring(3));
        }
    }

    protected InputStream getInputStream() {
        return inputStream;
    }

    public synchronized InputStream requestMessageBodyAsInputStream(final String messageID) throws IOException {
        final CommandResponse response = sendCmd(COMMAND.BODY, messageID);
        switch (response.getResponseCode()) {
        case 222:
            break;
        default:
            throw new IOException("Unknown Response: " + response);
        }
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream() {
            @Override
            public synchronized byte[] toByteArray() {
                return buf;
            };
        };
        buffer.reset();
        String line = null;
        int lineLength = readLine(getInputStream(), buffer);
        if (lineLength > 0) {
            line = new String(buffer.toByteArray(), 0, lineLength, "ISO-8859-1");
            logger.info("Read Response:" + line);
            if (line.startsWith("=ybegin")) {
                logger.info("yEnc Body detected");
                return new YEncInputStream(this, buffer);
            }
        }
        silentDisconnect();
        throw new IOException("Unknown Body Format:" + line);
    }

    private synchronized void sendCommand(String request) throws IOException {
        try {
            logger.info("Send Command:" + request);
            outputStream.write(request.getBytes("ISO-8859-1"));
            outputStream.write(CRLF);
            outputStream.flush();
        } catch (IOException e) {
            silentDisconnect();
            throw e;
        }
    }

    public CommandResponse articleHead(final String articleID) throws IOException {
        return sendCmd(COMMAND.HEAD, articleID);
    }

    public CommandResponse sendCmd(COMMAND command) throws IOException {
        return sendCmd(command, null);
    }

    public synchronized CommandResponse sendCmd(COMMAND command, String parameter) throws IOException {
        if (parameter != null) {
            sendCommand(command.getCommand() + " " + parameter);
        } else {
            sendCommand(command.getCommand());
        }
        final CommandResponse response = readCommandResponse(command);
        switch (response.getResponseCode()) {
        case 480:
            throw new IOException("Authentication required for command");
        }
        return response;
    }

    public void quit() throws IOException {
        try {
            final CommandResponse response = sendCmd(COMMAND.QUIT, null);
            if (response.getResponseCode() != 205) {
                throw new IOException("Unclean QUIT:" + response);
            }
        } finally {
            disconnect();
        }
    }

    public void disconnect() throws IOException {
        socket.close();
    }

    private void silentDisconnect() {
        try {
            disconnect();
        } catch (final IOException ignore) {
        }
    }

}
