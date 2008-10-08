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
            incoming.setSoTimeout(10000);
            outgoing = new Socket(serverip, 80);
            outgoing.setSoTimeout(10000);
            ProxyThread thread1 = new ProxyThread(incoming, outgoing, 1, steps);
            thread1.start();

            ProxyThread thread2 = new ProxyThread(outgoing, incoming, 2, steps);
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
        instance = this;
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
            while (true) {
                numberRead = fromClient.read(minibuffer, 0, 2000);

                if (numberRead == -1) {
                    break;
                } else {
                    if (dowhat > 0) {
                        if (bigbuffer.remaining() < numberRead) {
                            ByteBuffer newbuffer = ByteBuffer.allocateDirect((bigbuffer.capacity() * 2));
                            bigbuffer.flip();
                            newbuffer.put(bigbuffer);
                            bigbuffer = newbuffer;
                        }
                        bigbuffer.put(minibuffer, 0, numberRead);
                    }
                }
                if (dowhat != 2) toClient.write(minibuffer, 0, numberRead);
            }
        } catch (Exception e) {
        }
        if (dowhat == 1) {
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
            }
        } else if (dowhat == 2) {
            /* Responses verändern */
            try {
                bigbuffer.flip();
                renewbuffer = false;
                byte[] b = new byte[bigbuffer.limit()];
                bigbuffer.get(b);
                buffer = JDHexUtils.getHexString(b);
                JDRRUtils.rewriteLocationHeader(instance);
                if (renewbuffer == true) {
                    bigbuffer = ByteBuffer.wrap(JDHexUtils.getByteArray(buffer));
                } else {
                    bigbuffer.flip();
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
            }
        }
    }

}
