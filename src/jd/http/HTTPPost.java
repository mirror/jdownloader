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

package jd.http;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import jd.utils.JDUtilities;

/**
 * Diese Klasse unterstützt bei Http Post requests
 * 
 * @author JD-Team
 */
public class HTTPPost {
    /**
     * Deliminator für post parameter
     */
    private String boundary = "-----------------------------333227e09c";

    /**
     * Gibt an ob eine Veribindung aufgebaut ist.
     */
    private boolean connected = false;
    /**
     * Interne Connection
     */
    private HTTPConnection connection;
    /**
     * Redirects automatisch folgen
     */
    private boolean followRedirects;
    /**
     * Name der verwendeten Inputfrom (html)
     */
    private String form = "file_1";
    /**
     * Host
     */
    private String ip;
    /**
     * logger
     */
    private Logger logger = JDUtilities.getLogger();
    private OutputStream output;

    /**
     * Bytewriter wird zum versenden von bytes (upload) benötigt
     */
    private BufferedOutputStream outputByteWriter = null;

    /**
     * StreamWroter wird zum versenden von text verwendet
     */
    private OutputStreamWriter outputwriter;
    /**
     * Path (Pfad zur datei)
     */
    private String path;
    /**
     * POrt
     */
    private int port = 80;
    /**
     * Query STring
     */
    private String query;
    private int readTimeout = 10000;
    /**
     * Vollständige Request URL
     */

    private int requestTimeout = 10000;
    /**
     * Gibt an ob ein Uploadvorgang durchgeführt wird
     */
    private boolean upload = false;
    private URL url;

    private JDProxy proxy;

    public HTTPPost(String urlString, boolean followRedirects) {
        boundary = "--------------------" + Long.toString(System.currentTimeMillis(), 16);
        URL u;
        try {
            logger.info(urlString);
            u = new URL(urlString);

            ip = u.getHost();
            port = u.getPort();
            path = u.getPath();
            query = u.getQuery();
            this.followRedirects = followRedirects;

            boolean follow = HttpURLConnection.getFollowRedirects();
            HttpURLConnection.setFollowRedirects(followRedirects);
            url = u;
            logger.fine("POST " + url);
            if (proxy != null) {
                connection = new HTTPConnection(url.openConnection(proxy));

            } else {
                connection = new HTTPConnection(url.openConnection());

            }
            connection.setRequestMethod("POST");
            // Wird wieder zurückgestellt weil der Static Wert beim
            // instanzieren
            // in die INstanz übernommen wird
            HttpURLConnection.setFollowRedirects(follow);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * @param ip
     * @param port
     * @param path
     * @param query
     * @param followRedirects
     */
    public HTTPPost(String ip, int port, String path, String query, boolean followRedirects) {
        if (port <= 0) {
            port = 80;
        }
        this.ip = ip;
        this.port = port;
        this.path = path;
        this.query = query;
        this.followRedirects = followRedirects;

        try {
            boolean follow = HttpURLConnection.getFollowRedirects();
            HttpURLConnection.setFollowRedirects(followRedirects);
            url = new URL("http://" + ip + ":" + port + path + query);
            logger.fine("POST " + url);
            if (proxy != null) {
                connection = new HTTPConnection(url.openConnection(proxy));
            } else {
                connection = new HTTPConnection(url.openConnection());
            }
            connection.setRequestMethod("POST");
            // Wird wieder zurückgestellt weil der Static Wert beim
            // instanzieren
            // in die INstanz übernommen wird
            HttpURLConnection.setFollowRedirects(follow);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Fügt einen Boundary String zum Post hinzu
     */
    public void addBoundary() {
        write(boundary);
    }

    /**
     * Schreibt den String zum POst
     * 
     * @param arg
     */
    public void addToRequest(String arg) {
        write(arg);
    }

    /**
     * Schließt alle Streams
     */
    public void close() {
        try {
            addBoundary();
            addToRequest("--\r\n");
            if (outputByteWriter != null) {
                outputByteWriter.close();
            }
            outputwriter.close();
            if (output != null) {
                output.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * baut die Outputstreams auf
     */
    public void connect() {
        try {
            connection.setDoOutput(true);
            outputwriter = new OutputStreamWriter(output = connection.getOutputStream());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Muss bei einem Upload aufgerufen werden
     */
    public void doUpload() {
        setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary.substring(2));
        upload = true;
    }

    /**
     * @return the boundary
     */
    public String getBoundary() {
        return boundary;
    }

    /**
     * @return the connection
     */
    public HTTPConnection getConnection() {
        return connection;
    }

    /**
     * @return the form
     */
    public String getForm() {
        return form;
    }

    /**
     * Gibt den Inputstream zurück
     * 
     * @return Inputstream
     */
    public InputStream getInputStream() {
        InputStream ret = null;
        connection.setConnectTimeout(requestTimeout);
        connection.setReadTimeout(readTimeout);
        try {
            ret = connection.getInputStream();
        } catch (IOException e) {
            logger.severe(url + " : 500 Internal Server Error");
            // e.printStackTrace();
        }
        connected = true;
        return ret;
    }

    /**
     * @return the ip
     */
    public String getIp() {
        return ip;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @return the readTimeout
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * @return the requestTimeout
     */
    public int getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * @return the url
     */
    public URL getUrl() {
        return url;
    }

    /**
     * @return the connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * @return the followRedirects
     */
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    /**
     * @return the upload
     */
    public boolean isUpload() {
        return upload;
    }

    /**
     * Fügt eine neue zeile an den Post
     * 
     * @param value
     */
    public void post(String value) {
        addToRequest("\r\n" + value);

    }

    /**
     * Liest vom internet INputstream
     * 
     * @return Inhalt
     */
    public String read() {
        InputStream input;

        input = getInputStream();
        if (!isConnected()) { return null; }
        Scanner r = new Scanner(input).useDelimiter("\\Z");
        String ret = "";
        while (r.hasNext()) {
            ret += r.next();
        }
        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;

    }

    /**
     * Liest über einenGzipreader vom Iinputstream
     * 
     * @return Inhalt
     */
    public String readGZIP() {
        GZIPInputStream input;

        try {
            input = new GZIPInputStream(getInputStream());
            if (!isConnected()) { return null; }
            Scanner r = new Scanner(input).useDelimiter("\\Z");
            String ret = "";
            while (r.hasNext()) {
                ret += r.next();

            }
            input.close();
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

    }

    /**
     * Verschickt ein Byte Array über post (datei)
     * 
     * @param fileName
     *            Dateiname
     * @param b
     *            Byte Array
     * @param length
     *            Länge von b
     */
    public void sendBytes(String fileName, byte[] b, int length) {
        try {
            addBoundary();
            addToRequest("\r\nContent-Disposition: form-data; name=\"" + form + "\"; filename=\"" + fileName + "\"");
            addToRequest("\r\nContent-Type: " + "application/octet-stream" + "\r\n\r\n");
            outputwriter.flush();
            if (outputByteWriter != null) {
                outputByteWriter.close();
            }
            outputByteWriter = new BufferedOutputStream(output);
            outputByteWriter.write(b, 0, length);
            outputwriter.write("\r\n");
            outputByteWriter.flush();
            outputwriter.flush();
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    /**
     * Verschickt die Datei path über Post
     * 
     * @param path
     * @param fileName
     */
    public void sendFile(String path, String fileName) {
        try {
            addBoundary();
            File f = new File(path);
            // MimetypesFileTypeMap ft = new MimetypesFileTypeMap();
            // String ct = ft.getContentType(f);

            addToRequest("\r\nContent-Disposition: form-data; name=\"" + form + "\"; filename=\"" + fileName + "\"");
            addToRequest("\r\nContent-Type: image/jpeg\r\n\r\n");
            outputwriter.flush();
            byte[] b = new byte[1024];
            InputStream in;

            in = new FileInputStream(f);
            if (outputByteWriter != null) {
                outputByteWriter.close();
            }
            outputByteWriter = new BufferedOutputStream(output);
            int n;
            while ((n = in.read(b)) > -1) {
                outputByteWriter.write(b, 0, n);
            }
            in.close();
            outputByteWriter.flush();
            outputwriter.flush();
            write("\r\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Hängt einen Post Parameter an
     * 
     * @param key
     * @param value
     */
    public void sendVariable(String key, String value) {
        addBoundary();
        addToRequest("\r\nContent-Disposition: form-data; name=\"" + key + "\"");
        addToRequest("\r\n\r\n");
        addToRequest(value + "\r\n");

    }

    /**
     * @param boundary
     *            the boundary to set
     */
    public void setBoundary(String boundary) {
        this.boundary = boundary;
    }

    /**
     * @param form
     *            the form to set
     */
    public void setForm(String form) {
        this.form = form;
    }

    /**
     * @param readTimeout
     *            the readTimeout to set
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * setzt ein requestproperty
     * 
     * @param key
     * @param value
     */
    public void setRequestProperty(String key, String value) {
        connection.setRequestProperty(key, value);

    }

    /**
     * @param requestTimeout
     *            the requestTimeout to set
     */
    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Schreibt String zum POst
     * 
     * @param arg
     */
    public void write(String arg) {
        try {
            outputwriter.write(arg);
            outputwriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setProxy(JDProxy proxy) {
        this.proxy = proxy;

    }

    public JDProxy getProxy() {
        return proxy;
    }

}
