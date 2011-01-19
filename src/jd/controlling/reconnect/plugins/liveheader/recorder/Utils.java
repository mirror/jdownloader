//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.controlling.reconnect.plugins.liveheader.recorder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.nutils.encoding.Encoding;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.AwReg;

public final class Utils {
    /**
     * Don't let anyone instantiate this class.
     */
    private Utils() {
    }

    public static String readline(final BufferedInputStream in) {
        final StringBuilder data = new StringBuilder("");
        int c;
        try {
            in.mark(1);
            if (in.read() == -1)
                return null;
            else
                in.reset();
            while ((c = in.read()) >= 0) {
                if ((c == 0) || (c == 10) || (c == 13))
                    break;
                else
                    data.append((char) c);
            }
            if (c == 13) {
                in.mark(1);
                if (in.read() != 10) {
                    in.reset();
                }
            }
        } catch (Exception e) {
        }
        return data.toString();
    }

    public static ByteBuffer readheader(final InputStream in) {
        ByteBuffer bigbuffer = ByteBuffer.allocateDirect(4096);
        final byte[] minibuffer = new byte[1];
        int position;
        int c;
        boolean complete = false;
        try {
            while ((c = in.read(minibuffer)) >= 0) {
                if (bigbuffer.remaining() < 1) {
                    final ByteBuffer newbuffer = ByteBuffer.allocateDirect((bigbuffer.capacity() * 2));
                    bigbuffer.flip();
                    newbuffer.put(bigbuffer);
                    bigbuffer = newbuffer;
                }
                if (c > 0) bigbuffer.put(minibuffer);
                if (bigbuffer.position() >= 4) {
                    position = bigbuffer.position();
                    complete = bigbuffer.get(position - 4) == (byte) 13;
                    complete &= bigbuffer.get(position - 3) == (byte) 10;
                    complete &= bigbuffer.get(position - 2) == (byte) 13;
                    complete &= bigbuffer.get(position - 1) == (byte) 10;
                    if (complete) break;
                }
            }
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        bigbuffer.flip();
        return bigbuffer;
    }

    public static InputStream newInputStream(final ByteBuffer buf) {
        return new InputStream() {
            public synchronized int read() throws IOException {
                if (!buf.hasRemaining()) { return -1; }
                return buf.get();
            }

            public synchronized int read(final byte[] bytes, final int off, int len) throws IOException {
                // Read only what's left
                if (!buf.hasRemaining()) { return -1; }
                len = Math.min(len, buf.remaining());
                buf.get(bytes, off, len);
                return len;
            }
        };
    }

    public static void createStep(LinkedHashMap<String, String> headers, String postdata, Vector<String> steps, boolean ishttps, boolean rawmode) {
        if (!new AwReg(headers.get(null), ".*?\\.(gif|jpg|png|bmp|ico|css).*?").matches()) {
            String httpstrue = "";
            String rawtrue = "";
            if (ishttps) {
                httpstrue = " https=\"true\"";
            }
            if (rawmode) {
                rawtrue = " raw=\"true\"";
            }
            final StringBuilder hlh = new StringBuilder();
            hlh.append("    [[[STEP]]]" + "\r\n");
            hlh.append("        [[[REQUEST" + httpstrue + rawtrue + "]]]" + "\r\n");
            if (rawmode == true) {
                for (final Entry<String, String> entry : headers.entrySet()) {
                    final String key = entry.getKey();
                    /*
                     * werden vom browser gesetzt
                     */
                    if (key == null) {
                        hlh.append("        " + headers.get(null) + "\r\n");
                    } else {
                        if (key.equalsIgnoreCase("referer")) {
                            continue;
                        }
                        if (key.equalsIgnoreCase("host")) {
                            hlh.append("        Host: %%%routerip%%%" + "\r\n");
                            continue;
                        }

                        hlh.append("        " + key + ": " + entry.getValue() + "\r\n");
                    }
                }
            } else {
                hlh.append("        " + headers.get(null) + "\r\n");
                hlh.append("        Host: %%%routerip%%%" + "\r\n");
                if (headers.containsKey("authorization")) {
                    String auth = new AwReg(headers.get("authorization"), "Basic (.+)").getMatch(0);
                    if (auth != null) ReconnectRecorder.AUTH = Encoding.Base64Decode(auth.trim());
                    hlh.append("        Authorization: Basic %%%basicauth%%%" + "\r\n");
                }
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
    }

    public static void rewriteLocationHeader(final ProxyThread instance) {
        String location = JDHexUtils.toString(new AwReg(instance.buffer, Pattern.compile(JDHexUtils.getHexString("Location: ") + "(.*?)" + JDHexUtils.REGEX_HTTP_NEWLINE, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0));
        if (location != null) {
            if (new AwReg(location, "https?://(.*?)/?").getMatch(0) != null) {
                final String oldlocation = location;
                location = new AwReg(location, "https?://.*?/(.+)", Pattern.DOTALL).getMatch(0);
                if (!oldlocation.startsWith("https")) {
                    if (location != null) {
                        location = "http://localhost:" + SubConfiguration.getConfig("ReconnectRecorder").getIntegerProperty(ReconnectRecorder.PROPERTY_PORT, 8972) + "/" + location;
                    } else {
                        location = "http://localhost:" + SubConfiguration.getConfig("ReconnectRecorder").getIntegerProperty(ReconnectRecorder.PROPERTY_PORT, 8972) + "/";
                    }
                } else {
                    if (location != null) {
                        location = "https://localhost:" + SubConfiguration.getConfig("ReconnectRecorder").getIntegerProperty(ReconnectRecorder.PROPERTY_PORT, 8972) + "/" + location;
                    } else {
                        location = "https://localhost:" + (SubConfiguration.getConfig("ReconnectRecorder").getIntegerProperty(ReconnectRecorder.PROPERTY_PORT, 8972) + 1) + "/";
                    }
                }
                instance.buffer = instance.buffer.replaceAll(JDHexUtils.getHexString("Location: " + oldlocation), JDHexUtils.getHexString("Location: " + location));
                instance.renewbuffer = true;
            }
        }
    }

    public static void rewriteHostHeader(final ProxyThread instance) {
        String host = JDHexUtils.toString(new AwReg(instance.buffer, Pattern.compile(JDHexUtils.getHexString("Host: ") + "(.*?)" + JDHexUtils.REGEX_HTTP_NEWLINE, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0));
        if (host != null) {
            if (new AwReg(host, "(.*?):?").getMatch(0) != null) {
                final String oldhost = host;
                host = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);
                instance.buffer = instance.buffer.replaceAll(JDHexUtils.getHexString("Host: " + oldhost), JDHexUtils.getHexString("Host: " + host));
                instance.renewbuffer = true;
            }
        }
    }

    public static void rewriteConnectionHeader(final ProxyThread instance) {
        String con = JDHexUtils.toString(new AwReg(instance.buffer, Pattern.compile(JDHexUtils.getHexString("Connection: ") + "(.*?)" + JDHexUtils.REGEX_HTTP_NEWLINE, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0));
        if (con != null) {
            final String type = new AwReg(con, "(.+)").getMatch(0);
            if (type != null && !type.equalsIgnoreCase("close")) {
                final String oldcon = con;
                con = "close";
                instance.buffer = instance.buffer.replaceAll(JDHexUtils.getHexString("Connection: " + oldcon), JDHexUtils.getHexString("Connection: " + con));
                instance.renewbuffer = true;
            }
        }
    }

    public static void rewriteRefererHeader(final ProxyThread instance) {
        String ref = JDHexUtils.toString(new AwReg(instance.buffer, Pattern.compile(JDHexUtils.getHexString("Referer: ") + "(.*?)" + JDHexUtils.REGEX_HTTP_NEWLINE, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0));
        if (ref != null) {
            if (new AwReg(ref, "https?://(.*?)/?").getMatch(0) != null) {
                String oldref = ref;
                String ref2 = new AwReg(ref, "https?://.*?/(.+)", Pattern.DOTALL).getMatch(0);
                if (!oldref.startsWith("https")) {
                    if (ref2 != null) {
                        ref = "http://" + JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null) + "/" + ref2.trim();
                    } else {
                        if (new AwReg(ref, "https?://.*?/", Pattern.DOTALL).matches()) {
                            ref = "http://" + JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null) + "/";
                        } else {
                            ref = "http://" + JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);
                        }
                    }
                } else {
                    if (ref2 != null) {
                        ref = "https://" + JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null) + "/" + ref2.trim();
                    } else {
                        if (new AwReg(ref, "https?://.*?/", Pattern.DOTALL).matches()) {
                            ref = "https://" + JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null) + "/";
                        } else {
                            ref = "https://" + JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);
                        }
                    }
                }
                instance.buffer = instance.buffer.replaceAll(JDHexUtils.getHexString("Referer: " + oldref), JDHexUtils.getHexString("Referer: " + ref));
                instance.renewbuffer = true;
            }
        }
    }
}
