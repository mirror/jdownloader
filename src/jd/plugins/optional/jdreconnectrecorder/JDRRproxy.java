package jd.plugins.optional.jdreconnectrecorder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Vector;

import jd.http.Encoding;
import jd.parser.Regex;

public class JDRRproxy extends Thread {

    private Socket Current_Socket;
    public Vector<String> steps = null;
    String serverip;

    public JDRRproxy(Socket Client_Socket, Vector<String> steps, String serverip) {
        Current_Socket = Client_Socket;
        this.steps = steps;
        this.serverip = serverip;
    }

    public void run() {

        Socket incoming = Current_Socket;

        Socket outgoing = null;
        try {
            outgoing = new Socket(serverip, 80);
            ProxyThread thread1 = new ProxyThread(incoming, outgoing, 1, steps);
            thread1.start();

            ProxyThread thread2 = new ProxyThread(outgoing, incoming, 0, steps);
            thread2.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class ProxyThread extends Thread {

    Socket incoming, outgoing;
    public Vector<String> steps = null;
    int dowhat = 0;

    public ProxyThread(Socket in, Socket out, int dowhat, Vector<String> steps) {
        incoming = in;
        outgoing = out;
        this.dowhat = dowhat;
        this.steps = steps;
    }

    // Overwritten run() method of thread,
    // does the data transfers
    public void run() {
        byte[] buffer = new byte[60];
        ByteBuffer head = ByteBuffer.allocateDirect(32767);
        int numberRead = 0;
        OutputStream toClient;
        InputStream fromClient;
        StringBuffer hlh = new StringBuffer();
        try {
            toClient = outgoing.getOutputStream();
            fromClient = incoming.getInputStream();

            while (true) {

                numberRead = fromClient.read(buffer, 0, 50);

                if (numberRead == -1) {
                    incoming.close();
                    outgoing.close();
                } else {
                    head.put(buffer, 0, numberRead);
                }
                toClient.write(buffer, 0, numberRead);
            }
        } catch (Exception e) {
        }
        try {
            if (dowhat == 1) {
                head.flip();
                InputStream k = newInputStream(head);
                BufferedInputStream reader = new BufferedInputStream(k);
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
                String postdata = null;
                if (headers.containsKey("content-type")) {
                    if (headers.get("content-type").compareToIgnoreCase("application/x-www-form-urlencoded") == 0) {
                        if (headers.containsKey("content-length")) {
                            int post_len = new Integer(headers.get("content-length"));
                            int post_len_toread = new Integer(post_len);
                            int post_len_read = new Integer(0);
                            byte[] cbuf = new byte[post_len];
                            int indexstart = 0;
                            while (post_len_toread > 0) {

                                if ((post_len_read = reader.read(cbuf, indexstart, post_len_toread)) == -1) {
                                    break;
                                }

                                indexstart = indexstart + post_len_read;
                                post_len_toread = post_len_toread - post_len_read;
                            }
                            postdata = new String(cbuf).trim();
                        }

                    }
                }
                hlh.append("    [[[STEP]]]" + "\r\n");
                hlh.append("        [[[REQUEST]]]" + "\r\n");
                hlh.append("        " + headers.get(null) + "\r\n");
                hlh.append("        Host: %%%routerip%%%" + "\r\n");
                if (headers.containsKey("authorization")) {
                    String auth = new Regex(headers.get("authorization"), "Basic (.+)").getMatch(0);
                    if (auth != null) JDRR.auth = Encoding.Base64Decode(auth.trim());
                    hlh.append("        Authorization: Basic %%%basicauth%%%" + "\r\n");
                }
                if (headers.get(null).contains("POST") && postdata != null) {
                    hlh.append("\r\n");
                    hlh.append(postdata.trim());
                    hlh.append("\r\n");
                }
                hlh.append("        [[[/REQUEST]]]" + "\r\n");
                hlh.append("    [[[/STEP]]]" + "\r\n");
                steps.add(hlh.toString());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public String readline(BufferedInputStream reader) {
        /* ne eigene readline für BufferedInputStream */
        /*
         * BufferedReader hat nur böse Probleme mit dem Verarbeiten von
         * FileUploads gehabt
         */
        int max_buf = 1024;
        byte[] buffer = new byte[max_buf];
        int index = 0;
        int byteread = 0;
        try {

            while ((byteread = reader.read()) != -1) {
                if (byteread == 10 || byteread == 13) {
                    reader.mark(0);
                    if ((byteread = reader.read()) != -1) {
                        if (byteread == 13 || byteread == 10) {
                            break;
                        } else {
                            reader.reset();
                            break;
                        }
                    }
                }
                if (index > max_buf) { return null; }
                buffer[index] = (byte) byteread;
                index++;
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
        return new String(buffer).substring(0, index);
    }

    public static InputStream newInputStream(final ByteBuffer buf) {
        return new InputStream() {
            public synchronized int read() throws IOException {
                if (!buf.hasRemaining()) { return -1; }
                return buf.get();
            }

            public synchronized int read(byte[] bytes, int off, int len) throws IOException {
                // Read only what's left
                len = Math.min(len, buf.remaining());
                buf.get(bytes, off, len);
                return len;
            }
        };
    }

}
