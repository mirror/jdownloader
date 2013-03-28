package org.jdownloader.extensions.myjdownloader.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.crypto.AWSign;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.Base64InputStream;
import org.jdownloader.extensions.myjdownloader.MyDownloaderExtensionConfig;
import org.jdownloader.logging.LogController;

public class MyJDownloaderAPI {

    private final int                           APIVersion   = 1;
    protected AtomicLong                        TIMESTAMP    = new AtomicLong(System.currentTimeMillis());
    protected volatile String                   connectToken = null;

    protected final MyDownloaderExtensionConfig config;
    private LogSource                           logger;

    public MyJDownloaderAPI(MyDownloaderExtensionConfig config) {
        this.config = config;
        logger = LogController.getInstance().getLogger(getClass().getName());
    }

    public LogSource getLogger() {
        return logger;
    }

    protected byte[] getServerSecret(String username, String password) throws IOException, NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest((username + password + "server").getBytes("UTF-8"));
    }

    protected byte[] getJDSecret(String username, String password) throws IOException, NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest((username + password + "jd").getBytes("UTF-8"));
    }

    protected byte[] getAESSecret(byte[] jdSecret, byte[] requestConnectToken) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(jdSecret);
        md.update(requestConnectToken);
        return md.digest();
    }

    public byte[] getServerSecret() throws NoSuchAlgorithmException, IOException {
        return getServerSecret(config.getUsername(), config.getPassword());
    }

    public byte[] getJDSecret() throws NoSuchAlgorithmException, IOException {
        return getJDSecret(config.getUsername(), config.getPassword());
    }

    public byte[] getAESSecret(String requestConnectToken) throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException {
        if (requestConnectToken == null) return null;
        return getAESSecret(getJDSecret(), HexFormatter.hexToByteArray(requestConnectToken));
    }

    public String getConnectToken() throws InvalidKeyException, NoSuchAlgorithmException, IOException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        if (connectToken != null) return connectToken;
        connectToken = getConnectToken(config.getUsername(), config.getPassword());
        return connectToken;
    }

    public void invalidateConnectToken() {
        connectToken = null;
    }

    protected String getRequest(String url) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        Browser br = new Browser();
        long timeStamp = TIMESTAMP.incrementAndGet();
        url = url + "&timestamp=" + timeStamp + "&apiverson=" + APIVersion;
        String signature = getSignature(getServerSecret(config.getUsername(), config.getPassword()), url.getBytes("UTF-8"));
        String completeurl = "http://" + config.getAPIServerURL() + ":" + config.getAPIServerPort() + url + "&signature=" + signature;
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(completeurl);
            if (con.getResponseCode() == 403) throw new InvalidConnectException();
            if (con.isOK()) {
                byte[] response = null;
                final String type = con.getHeaderField(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE);
                if (new Regex(type, "(application/aesjson-)").matches()) {
                    byte[] serverSecret = getServerSecret(config.getUsername(), config.getPassword());
                    final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    final byte[] IV = Arrays.copyOfRange(serverSecret, 0, 16);
                    final IvParameterSpec ivSpec = new IvParameterSpec(IV);
                    final byte[] KEY = Arrays.copyOfRange(serverSecret, 16, 32);
                    final SecretKeySpec skeySpec = new SecretKeySpec(KEY, "AES");
                    cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);

                    response = IO.readStream(-1, new CipherInputStream(new Base64InputStream(con.getInputStream()), cipher));
                    String responseSTRING = new String(response, "UTF-8");
                    String timestampJSON = new Regex(responseSTRING, "\"timestamp\"\\s*?:\\s*?(\\d+)").getMatch(0);
                    if (timestampJSON == null || timeStamp != Long.parseLong(timestampJSON)) throw new InvalidConnectException();
                    return responseSTRING;
                } else {
                    response = IO.readStream(-1, con.getInputStream());
                    return new String(response, "UTF-8");
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        throw new IOException("Unknown IOException");
    }

    protected String getConnectToken(String username, String password) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        String url = "/my/jdconnect?user=" + Encoding.urlEncode(config.getUsername());
        String response = getRequest(url);
        String token = new Regex(response, "\"token\"\\s*?:\\s*?\"([a-fA-F0-9]+)\"").getMatch(0);
        if (token == null) throw new IOException("Unknown Response: " + response);
        return token;
    }

    protected String getSignature(byte[] secret, byte[] content) throws InvalidKeyException, NoSuchAlgorithmException {
        return HexFormatter.byteArrayToHex(AWSign.HMACSHA256(secret, content));
    }
}
