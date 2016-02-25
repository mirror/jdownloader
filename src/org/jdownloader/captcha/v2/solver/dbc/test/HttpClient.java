package org.jdownloader.captcha.v2.solver.dbc.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

import com.oracle.javafx.jmx.json.JSONException;

/**
 * Death by Captcha HTTP API client.
 *
 */
public class HttpClient extends Client {
    final static public String CRLF       = "\r\n";
    final static public String SERVER_URL = "http://api.dbcapi.me/api";

    /**
     * Proxy to use, defaults to none.
     */
    public Proxy               proxy      = Proxy.NO_PROXY;

    private class HttpClientCaller {
        final static public String RESPONSE_CONTENT_TYPE = "application/json";

        public String call(Proxy proxy, URL url, byte[] payload, String contentType, Date deadline) throws IOException, Exception {
            String response = null;
            while (deadline.after(new Date()) && null != url && null == response) {
                HttpURLConnection req = null;
                try {
                    req = (HttpURLConnection) url.openConnection(proxy);
                } catch (IOException e) {
                    throw new IOException("API connection failed: " + e.toString());
                }

                req.setRequestProperty("Accept", HttpClientCaller.RESPONSE_CONTENT_TYPE);
                req.setRequestProperty("User-Agent", Client.API_VERSION);
                req.setInstanceFollowRedirects(false);

                if (0 < payload.length) {
                    try {
                        req.setRequestMethod("POST");
                    } catch (java.lang.Exception e) {
                        //
                    }
                    req.setRequestProperty("Content-Type", contentType);
                    req.setRequestProperty("Content-Length", String.valueOf(payload.length));
                    req.setDoOutput(true);
                    OutputStream st = null;
                    try {
                        st = req.getOutputStream();
                        st.write(payload);
                        st.flush();
                    } catch (IOException e) {
                        throw new IOException("Failed sending API request: " + e.toString());
                    } finally {
                        try {
                            st.close();
                        } catch (java.lang.Exception e) {
                            //
                        }
                    }
                    payload = new byte[0];
                } else {
                    try {
                        req.setRequestMethod("GET");
                    } catch (java.lang.Exception e) {
                        //
                    }
                }

                url = null;
                req.setConnectTimeout(3 * Client.POLLS_INTERVAL * 1000);
                req.setReadTimeout(3 * Client.POLLS_INTERVAL * 1000);
                try {
                    req.connect();
                } catch (IOException e) {
                    throw new IOException("API connection failed: " + e.toString());
                }

                try {
                    int responseLength = 0;
                    int i = 1;
                    String k = null;
                    while (null != (k = req.getHeaderFieldKey(i))) {
                        if (k.equals("Content-Length")) {
                            responseLength = Integer.parseInt(req.getHeaderField(i), 10);
                        } else if (k.equals("Location")) {
                            try {
                                url = new URL(req.getHeaderField(i));
                            } catch (java.lang.Exception e) {
                                //
                            }
                        }
                        i++;
                    }

                    switch (req.getResponseCode()) {
                    case HttpURLConnection.HTTP_FORBIDDEN:
                        throw new AccessDeniedException("Access denied, check your credentials and/or balance");

                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        throw new InvalidCaptchaException("CAPTCHA was rejected, check if it's a valid image");

                    case HttpURLConnection.HTTP_UNAVAILABLE:
                        throw new ServiceOverloadException("CAPTCHA was rejected due to service overload, try again later");

                    case HttpURLConnection.HTTP_SEE_OTHER:
                        if (null == url) {
                            throw new IOException("Invalid API redirection response");
                        }
                        break;
                    }

                    InputStream st = null;
                    try {
                        st = req.getInputStream();
                    } catch (IOException e) {
                        st = null;
                    } catch (java.lang.Exception e) {
                        st = req.getErrorStream();
                    }
                    if (null == st) {
                        throw new IOException("Failed receiving API response");
                    }

                    int offset = 0;
                    byte[] buff = new byte[responseLength];
                    try {
                        while (responseLength > offset) {
                            offset += st.read(buff, offset, responseLength - offset);
                        }
                        st.close();
                    } catch (IOException e) {
                        throw new IOException("Failed receiving API response: " + e.toString());
                    }
                    if (0 < buff.length) {
                        response = new String(buff, 0, buff.length);
                    }
                } catch (IOException e) {
                    throw e;
                } catch (Exception e) {
                    throw e;
                } catch (java.lang.Exception e) {
                    throw new IOException("API communication failed: " + e.toString());
                } finally {
                    try {
                        req.disconnect();
                    } catch (java.lang.Exception e) {
                        //
                    }
                }
            }
            return response;
        }
    }

    protected DataObject call(String cmd, byte[] data, String contentType) throws IOException, Exception {
        this.log("SEND", cmd);
        URL url = null;
        try {
            url = new URL(HttpClient.SERVER_URL + '/' + cmd);
        } catch (java.lang.Exception e) {
            throw new IOException("Invalid API command " + cmd);
        }
        String response = (new HttpClientCaller()).call(this.proxy, url, data, (null != contentType ? contentType : "application/x-www-form-urlencoded"), new Date(System.currentTimeMillis() + Client.DEFAULT_TIMEOUT * 1000));
        this.log("RECV", response);
        try {
            return new DataObject(response);
        } catch (JSONException e) {
            throw new IOException("Invalid API response");
        }
    }

    protected DataObject call(String cmd, byte[] data) throws IOException, Exception {
        return this.call(cmd, data, null);
    }

    protected DataObject call(String cmd, DataObject args) throws IOException, Exception {
        StringBuilder data = new StringBuilder();
        java.util.Iterator args_keys = args.keys();

        String k = null;
        while (args_keys.hasNext()) {
            k = args_keys.next().toString();
            try {
                data.append(URLEncoder.encode(k, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return new DataObject();
            }
            data.append("=");
            try {
                data.append(URLEncoder.encode(args.optString(k, ""), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return new DataObject();
            }
            if (args_keys.hasNext()) {
                data.append("&");
            }
        }
        return this.call(cmd, data.toString().getBytes());
    }

    protected DataObject call(String cmd) throws IOException, Exception {
        return this.call(cmd, new byte[0]);
    }

    /**
     * @see com.DeathByCaptcha.Client#Client(String, String)
     */
    public HttpClient(String username, String password) {
        super(username, password);
    }

    /**
     * @see com.DeathByCaptcha.Client#close
     */
    public void close() {
    }

    /**
     * @see com.DeathByCaptcha.Client#connect
     */
    public boolean connect() throws IOException {
        return true;
    }

    /**
     * @see com.DeathByCaptcha.Client#getUser
     */
    public User getUser() throws IOException, Exception {
        return new User(this.call("user", this.getCredentials()));
    }

    /**
     * @see com.DeathByCaptcha.Client#upload
     */
    public Captcha upload(byte[] img, String challenge, int type, byte[] banner, String banner_text, String grid) throws IOException, Exception {
        String boundary = null;
        try {
            boundary = (new java.math.BigInteger(1, (java.security.MessageDigest.getInstance("SHA1")).digest((new java.util.Date()).toString().getBytes()))).toString(16);
        } catch (java.security.NoSuchAlgorithmException e) {
            return null;
        }

        String header_data = "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"username\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + this._username.length() + HttpClient.CRLF + HttpClient.CRLF + this._username + HttpClient.CRLF + "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"password\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + this._password.length() + HttpClient.CRLF + HttpClient.CRLF + this._password + HttpClient.CRLF + "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"swid\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + HttpClient.CRLF + Client.SOFTWARE_VENDOR_ID + HttpClient.CRLF + "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"challenge\"" + HttpClient.CRLF
                + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + challenge.length() + HttpClient.CRLF + HttpClient.CRLF + challenge + HttpClient.CRLF + "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"banner_text\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + banner_text.length() + HttpClient.CRLF + HttpClient.CRLF + banner_text + HttpClient.CRLF + "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"grid\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + grid.length() + HttpClient.CRLF + HttpClient.CRLF + grid + HttpClient.CRLF + "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"type\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + Integer.toString(type).length()
                + HttpClient.CRLF + HttpClient.CRLF + type + HttpClient.CRLF + "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"captchafile\"; filename=\"captcha\"" + HttpClient.CRLF + "Content-Type: application/octet-stream" + HttpClient.CRLF + "Content-Length: " + img.length + HttpClient.CRLF + HttpClient.CRLF;
        byte[] hdr = header_data.getBytes();
        byte[] ftr = (HttpClient.CRLF + "--" + boundary + "--").getBytes();
        int data_length = hdr.length + img.length + ftr.length;
        byte[] banner_header_data = null;
        if (banner != null) {
            banner_header_data = ("--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"banner\"; filename=\"banner\"" + HttpClient.CRLF + "Content-Type: application/octet-stream" + HttpClient.CRLF + "Content-Length: " + banner.length + HttpClient.CRLF + HttpClient.CRLF).getBytes();
            data_length = data_length + banner_header_data.length + banner.length + ftr.length;
        }

        byte[] body = new byte[data_length];
        System.arraycopy(hdr, 0, body, 0, hdr.length);
        System.arraycopy(img, 0, body, hdr.length, img.length);
        System.arraycopy(ftr, 0, body, hdr.length + img.length, ftr.length);

        if (banner != null) {
            System.arraycopy(banner_header_data, 0, body, hdr.length + img.length + ftr.length, banner_header_data.length);
            System.arraycopy(ftr, 0, body, hdr.length + img.length + ftr.length + banner_header_data.length, ftr.length);
        }

        Captcha c = new Captcha(this.call("captcha", body, "multipart/form-data; boundary=" + boundary));
        return c.isUploaded() ? c : null;
    }

    public Captcha upload(byte[] img, String challenge, int type, byte[] banner, String banner_text) throws IOException, Exception {
        return this.upload(img, challenge, type, banner, banner_text, "");
    }

    public Captcha upload(byte[] img, int type, byte[] banner, String banner_text) throws IOException, Exception {
        return this.upload(img, type, banner, banner_text);
    }

    public Captcha upload(byte[] img) throws IOException, Exception {
        return this.upload(img, "", 0, null, "");
    }

    /**
     * @see com.DeathByCaptcha.Client#getCaptcha
     */
    public Captcha getCaptcha(int id) throws IOException, Exception {
        return new Captcha(this.call("captcha/" + id));
    }

    /**
     * @see com.DeathByCaptcha.Client#report
     */
    public boolean report(int id) throws IOException, Exception {
        return !(new Captcha(this.call("captcha/" + id + "/report", this.getCredentials()))).isCorrect();
    }
}
