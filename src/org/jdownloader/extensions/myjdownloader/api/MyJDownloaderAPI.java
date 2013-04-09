package org.jdownloader.extensions.myjdownloader.api;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

import jd.nutils.encoding.Encoding;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.BasicHTTP.BasicHTTP;
import org.appwork.utils.net.httpconnection.HTTPConnection;
import org.jdownloader.extensions.myjdownloader.MyDownloaderExtensionConfig;
import org.jdownloader.extensions.myjdownloader.MyJDownloaderExtension;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;
import org.jdownloader.myjdownloader.client.exceptions.ExceptionResponse;

public class MyJDownloaderAPI extends AbstractMyJDClient {

    private BasicHTTP br;
    private LogSource logger;

    @Override
    protected byte[] base64decode(String base64encodedString) {

        return Base64.decode(base64encodedString);

    }

    @Override
    protected String base64Encode(byte[] encryptedBytes) {
        return Base64.encodeToString(encryptedBytes, false);
    }

    @Override
    public String urlencode(String text) {
        return Encoding.urlEncode(text);
    }

    @Override
    protected String objectToJSon(final Object payload) {
        return JSonStorage.toString(payload);
    }

    @Override
    protected <T> T jsonToObject(final String dec, final Type clazz) {
        return JSonStorage.restoreFromString(dec, new TypeRef<T>(clazz) {
        });
    }

    @Override
    protected String post(String query, String object) throws ExceptionResponse {
        try {

            logger.info(object + "");
            final String ret = br.postPage(new URL(getServerRoot() + query), object == null ? "" : object);
            logger.info(br.getConnection() + "");
            logger.info(ret);
            final HTTPConnection con = br.getConnection();

            if (con != null && con.getResponseCode() > 0 && con.getResponseCode() != 200) {

            throw new ExceptionResponse(ret, con.getResponseCode());

            }

            return ret;
        } catch (final ExceptionResponse e) {
            throw e;
        } catch (final Exception e) {
            throw new ExceptionResponse(e);

        } finally {

        }
    }

    protected AtomicLong                        TIMESTAMP    = new AtomicLong(System.currentTimeMillis());
    protected volatile String                   connectToken = null;

    protected final MyDownloaderExtensionConfig config;

    private MyJDownloaderExtension              extension;

    public MyJDownloaderAPI(MyJDownloaderExtension myJDownloaderExtension) {
        super("JD");
        extension = myJDownloaderExtension;

        this.config = extension.getSettings();
        setServerRoot("http://" + config.getAPIServerURL() + ":" + config.getAPIServerPort());
        logger = extension.getLogger();
        br = new BasicHTTP();
        br.setAllowedResponseCodes(200, 503, 401, 407, 403, 500, 429);
        br.putRequestHeader("Content-Type", "application/json; charset=utf-8");

    }

    public LogSource getLogger() {
        return logger;
    }

    // protected String getConnectToken(String username, String password) throws IOException, InvalidKeyException, NoSuchAlgorithmException,
    // NoSuchPaddingException, InvalidAlgorithmParameterException {
    // logger.info("Login " + username + ":" + password);
    // String url = "/my/deviceconnect?email=" + Encoding.urlEncode(config.getUsername()) + "&deviceID=" +
    // Encoding.urlEncode(config.getUniqueDeviceID()) + "&type=JD&name=" + Encoding.urlEncode(config.getDeviceName());
    // Browser br = new Browser();
    // long timeStamp = TIMESTAMP.incrementAndGet();
    // url = url + "&rid=" + timeStamp;
    // byte[] loginSecret = getLoginSecret(config.getUsername(), config.getPassword());
    // String signature = getSignature(loginSecret, url.getBytes("UTF-8"));
    // String completeurl = "http://" + config.getAPIServerURL() + ":" + config.getAPIServerPort() + url + "&signature=" + signature;
    // URLConnectionAdapter con = null;
    // try {
    // logger.info("GET " + completeurl);
    // con = br.openGetConnection(completeurl);
    // if (con.getResponseCode() == 403) throw new InvalidConnectException();
    // if (con.isOK()) {
    // byte[] response = null;
    //
    // final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    // final byte[] IV = Arrays.copyOfRange(loginSecret, 0, 16);
    // final IvParameterSpec ivSpec = new IvParameterSpec(IV);
    // final byte[] KEY = Arrays.copyOfRange(loginSecret, 16, 32);
    // final SecretKeySpec skeySpec = new SecretKeySpec(KEY, "AES");
    // cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
    //
    // response = IO.readStream(-1, new CipherInputStream(new Base64InputStream(con.getInputStream()), cipher));
    //
    // String ret = new String(response, "UTF-8");
    // DeviceConnectResponse responseObject = JSonStorage.restoreFromString(ret, DeviceConnectResponse.class);
    // config.setUniqueDeviceID(responseObject.getDeviceid());
    //
    // logger.info("RESPONSE(plain): " + completeurl + "\r\n" + ret);
    // String token = new Regex(ret, "\"token\"\\s*?:\\s*?\"([a-fA-F0-9]+)\"").getMatch(0);
    // if (token == null) throw new IOException("Unknown Response: " + response);
    // logger.info("Login OK: " + token);
    // return token;
    // // }
    // }
    // } finally {
    // try {
    // con.disconnect();
    // } catch (final Throwable e) {
    // }
    // }
    // throw new IOException("Unknown IOException");
    //
    // }

    // protected String getSignature(byte[] secret, byte[] content) throws InvalidKeyException, NoSuchAlgorithmException {
    // return HexFormatter.byteArrayToHex(AWSign.HMACSHA256(secret, content));
    // }
}
