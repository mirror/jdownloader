package jd.plugins.optional.jdreconnectrecorder;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Vector;

import jd.utils.JDHexUtils;

public class JDRRproxy extends Thread {

    public final static int FORWARD = 1 << 1;
    public final static int RECORD_HEADER = 1 << 2;
    public final static int CHANGE_HEADER = 1 << 3;
    public final static int BLOCKED_READ = 1 << 10;

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
            ProxyThread thread1 = new ProxyThread(incoming, outgoing, CHANGE_HEADER | RECORD_HEADER, steps);
            thread1.start();

            ProxyThread thread2 = new ProxyThread(outgoing, incoming, CHANGE_HEADER | BLOCKED_READ, steps);
            thread2.start();
            thread2.join();
            try {
                outgoing.shutdownInput();
                incoming.shutdownInput();
                outgoing.shutdownOutput();
                incoming.shutdownOutput();
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
    }
}

class ProxyThread extends Thread {

    Socket incoming, outgoing;
    public Vector<String> steps = null;
    int dowhat = 0;
    boolean renewbuffer = false;
    String buffer;
    ProxyThread instance;

    /*
     * 0=normal weiterleiten, 1 = http header vom browser mitschneiden, 2 =
     * antworten manipulieren
     */
    public ProxyThread(Socket in, Socket out, int dowhat, Vector<String> steps) {
        incoming = in;
        outgoing = out;
        this.dowhat = dowhat;
        this.steps = steps;
        this.instance = this;
    }

    public boolean dothis(int dothis) {
        return (this.dowhat & dothis) > 0;
    }

    public void run() {
        byte[] minibuffer = new byte[2048];
        ByteBuffer bigbuffer = ByteBuffer.allocateDirect(4096);
        int numberRead = 0;
        OutputStream toClient = null;
        InputStream fromClient;
        try {
            toClient = outgoing.getOutputStream();
            fromClient = incoming.getInputStream();
            BufferedInputStream reader = new BufferedInputStream(fromClient);
            int hack = 0;
            while (true) {
                if (dothis(JDRRproxy.BLOCKED_READ)) {
                    numberRead = fromClient.read(minibuffer, 0, 2000);
                } else {
                    if (reader.available() > 0) {
                        numberRead = reader.read(minibuffer, 0, 2000);
                    } else {
                        numberRead = 0;
                    }
                    if (numberRead == 0) hack++;
                }
                if (numberRead == -1 || hack > 10000) {                    
                    break;
                } else {
                    if (numberRead > 0) {
                        if (!dothis(JDRRproxy.FORWARD)) {
                            if (bigbuffer.remaining() < numberRead) {
                                ByteBuffer newbuffer = ByteBuffer.allocateDirect((bigbuffer.capacity() * 2));
                                bigbuffer.flip();
                                newbuffer.put(bigbuffer);
                                bigbuffer = newbuffer;
                            }
                            bigbuffer.put(minibuffer, 0, numberRead);
                        } else {
                            toClient.write(minibuffer, 0, numberRead);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (dothis(JDRRproxy.RECORD_HEADER)) {
            /* Header aufzeichnen */
            try {
                bigbuffer.flip();
                InputStream k = JDRRUtils.newInputStream(bigbuffer);
                BufferedInputStream reader = new BufferedInputStream(k);
                String line = null;
                HashMap<String, String> headers = new HashMap<String, String>();

                while ((line = JDRRUtils.readline(reader)) != null && line.trim().length() > 0) {
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
                JDRRUtils.createStep(headers, postdata, steps);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (dothis(JDRRproxy.CHANGE_HEADER) || !dothis(JDRRproxy.FORWARD)) {
            /* Responses verändern */
            try {
                bigbuffer.flip();
                if (dothis(JDRRproxy.CHANGE_HEADER)) {
                    renewbuffer = false;
                    byte[] b = new byte[bigbuffer.limit()];
                    bigbuffer.get(b);
                    buffer = JDHexUtils.getHexString(b);
                    JDRRUtils.rewriteLocationHeader(instance);
                    JDRRUtils.rewriteHostHeader(instance);
                    JDRRUtils.rewriteRefererHeader(instance);
                    if (renewbuffer == true) {
                        bigbuffer = ByteBuffer.wrap(JDHexUtils.getByteArray(buffer));
                    } else {
                        bigbuffer = ByteBuffer.wrap(b);
                    }
                }
                fromClient = JDRRUtils.newInputStream(bigbuffer);
                while (true) {
                    numberRead = fromClient.read(minibuffer, 0, 2000);
                    if (numberRead == -1) {
                        break;
                    }
                    toClient.write(minibuffer, 0, numberRead);
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

}
