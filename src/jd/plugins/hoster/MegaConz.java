package jd.plugins.hoster;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.controlling.FileStateManager;
import org.jdownloader.controlling.FileStateManager.FILESTATE;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.translate._JDT;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Base64;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;
import jd.plugins.download.HashResult;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mega.co.nz" }, urls = { "(https?://(www\\.)?mega\\.(co\\.)?nz/(#N?|\\$)|chrome://mega/content/secure\\.html#)(!|%21)[a-zA-Z0-9]+(!|%21)[a-zA-Z0-9_,\\-%]{16,}((=###n=|!)[a-zA-Z0-9]+)?|mega:/*#(?:!|%21)[a-zA-Z0-9]+(?:!|%21)[a-zA-Z0-9_,\\-%]{16,}" })
public class MegaConz extends PluginForHost {

    private final String USE_SSL        = "USE_SSL_V2";
    private final String CHECK_RESERVED = "CHECK_RESERVED";
    private final String USE_TMP        = "USE_TMP_V2";
    private final String HIDE_APP       = "HIDE_APP_V2";
    private final String encrypted      = ".encrypted";

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "mega.co.nz", "mega.nz" };
    }

    public MegaConz(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        enablePremium("https://mega.nz/#register");
    }

    @Override
    public String getAGBLink() {
        return "https://mega.co.nz/#terms";
    }

    private Number getNumber(Map<String, Object> map, final String key) {
        final Object ret = map.get(key);
        if (ret instanceof Number) {
            return (Number) ret;
        } else if (ret instanceof String) {
            return Long.parseLong(ret.toString());
        } else {
            return null;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        synchronized (account) {
            final String sid = apiLogin(account);
            final Map<String, Object> uq = apiRequest(account, sid, null, "uq"/* userQuota */, new Object[] { "xfer"/* xfer */, 1 }, new Object[] { "pro"/* pro */, 1 });
            // https://github.com/meganz/sdk/blob/master/src/commands.cpp
            // https://github.com/meganz/sdk/blob/master/bindings/ios/MEGAAccountDetails.h
            if (uq.containsKey("utype")) {
                final String subscriptionCycle;
                if (uq.containsKey("scycle")) {
                    subscriptionCycle = " (Subscription cycle: " + String.valueOf(uq.get("scycle")) + ")";
                } else {
                    subscriptionCycle = "";
                }
                final AccountInfo ai = new AccountInfo();
                boolean isPro = false;
                final String status;
                switch (getNumber(uq, "utype").intValue()) {
                case 1:
                    status = "Pro I Account" + subscriptionCycle;
                    isPro = true;
                    break;
                case 2:
                    status = "Pro II Account" + subscriptionCycle;
                    isPro = true;
                    break;
                case 3:
                    status = "Pro III Account" + subscriptionCycle;
                    isPro = true;
                    break;
                case 4:
                    status = "Pro Lite Account" + subscriptionCycle;
                    isPro = true;
                    break;
                default:
                case 0:
                    status = "Free Account";
                    isPro = false;
                    break;
                }
                if (isPro && uq.containsKey("suntil")) {
                    final Number expire = getNumber(uq, "suntil");
                    ai.setValidUntil(expire.longValue() * 1000);
                    if (ai.isExpired()) {
                        isPro = false;
                    }
                } else {
                    isPro = false;
                }
                if (isPro) {
                    if (uq.containsKey("mxfer") && uq.containsKey("csxfer")) {
                        final long max = (getNumber(uq, "mxfer")).longValue();
                        final long used = (getNumber(uq, "caxfer")).longValue();
                        final long reserved;
                        if (getPluginConfig().getBooleanProperty(CHECK_RESERVED, true)) {
                            reserved = (getNumber(uq, "rua")).longValue();
                        } else {
                            reserved = 0;
                        }
                        ai.setTrafficMax(max);
                        ai.setTrafficLeft(max - (used + reserved));
                    }
                    ai.setStatus(status);
                    account.setType(AccountType.PREMIUM);
                } else {
                    ai.setValidUntil(-1);
                    ai.setStatus("Free Account");
                    account.setType(AccountType.FREE);
                }
                account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 2 * 60 * 60 * 1000l);
                return ai;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    public static String valueOf(Object o) {
        if (o == null) {
            return null;
        } else {
            return String.valueOf(o);
        }
    }

    private void apiLogoff(final Account account) throws Exception {
        final String sid;
        synchronized (account) {
            sid = account.restoreObject("", TypeRef.STRING);
            account.clearObject("");
        }
        if (sid != null) {
            try {
                apiRequest(account, sid, null, "sml"/* logOut */);
            } catch (final PluginException ignore) {
            }
        }
    }

    // // unfinished
    // private boolean queryBandwidthQuota(final Account account, final long fileSize) throws Exception {
    // apiRequest(account, getSID(account), null, "qbq"/* queryBandwidthQuota */, new Object[] { "s", fileSize });
    // return true;
    // }
    private String apiLogin(Account account) throws Exception {
        synchronized (account) {
            try {
                final String email = account.getUser();
                final String password = account.getPass();
                if (email == null || !email.matches("^.+?@.+?\\.[^\\.]+")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Username must be valid e-mail", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (StringUtils.isEmpty(password)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                String sid = account.restoreObject("", TypeRef.STRING);
                Map<String, Object> response = null;
                if (sid != null) {
                    /* login via sid */
                    response = apiRequest(account, sid, null, "us"/* logIn */);
                    if (response != null && response.containsKey("privk")) {
                        return sid;
                    }
                }
                final long[] password_aes_aLong = prepare_key_aLong(String_to_aLong(password));
                if (response == null || !response.containsKey("privk")) {
                    /* fresh login */
                    final String lowerCaseEmail = email.toLowerCase(Locale.ENGLISH);
                    final String uh = calcuate_login_hash_uh(lowerCaseEmail, password_aes_aLong);
                    response = apiRequest(null, null, null, "us"/* logIn */, new Object[] { "user"/* email */, lowerCaseEmail }, new Object[] { "uh"/* emailHash */, uh });
                }
                if (response != null && (response.containsKey("k") && response.containsKey("privk") && response.containsKey("csid"))) {
                    try {
                        final String k = (String) response.get("k");
                        final String privk = (String) response.get("privk");
                        final long[] masterKey_aLong = decrypt_aLong(password_aes_aLong, Base64_to_aLong(k));
                        final long[] rsa_private_key_aLong = decrypt_aLong(masterKey_aLong, Base64_to_aLong(privk));
                        byte[] rsa_private_key_aBytes = aLong_to_aByte(rsa_private_key_aLong);
                        final BigInteger rsa_private_key_BigInteger[] = new BigInteger[4];
                        // multiple precision integers, first to bytes are length in bits and the following bytes are the number itself, big
                        // endian
                        int rsa_private_key_aBytes_Index = 0;
                        for (int i = 0; i < 4; i++) {
                            final int length = (((rsa_private_key_aBytes[rsa_private_key_aBytes_Index] & 0xFF) * 256 + (rsa_private_key_aBytes[rsa_private_key_aBytes_Index + 1] & 0xFF) + 7) / 8) + 2;
                            rsa_private_key_BigInteger[i] = new BigInteger(HexFormatter.byteArrayToHex(Arrays.copyOfRange(rsa_private_key_aBytes, rsa_private_key_aBytes_Index + 2, rsa_private_key_aBytes_Index + length)), 16);
                            rsa_private_key_aBytes_Index += length;
                        }
                        final PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(rsa_private_key_BigInteger[0].multiply(rsa_private_key_BigInteger[1]), rsa_private_key_BigInteger[2]));
                        final byte[] csid_aByte = b64decode((String) response.get("csid"));
                        final byte[] sid_aByte = rsa_ecb_decrypt_aByte(privateKey, Arrays.copyOfRange(csid_aByte, 2, csid_aByte.length));
                        String sid_hexString = HexFormatter.byteArrayToHex(sid_aByte[0] == 0 ? Arrays.copyOfRange(sid_aByte, 1, sid_aByte.length) : sid_aByte);
                        if (sid_hexString.length() % 2 != 0) {
                            sid_hexString = "0" + sid_hexString;
                        }
                        sid = b64encode(Arrays.copyOfRange(HexFormatter.hexToByteArray(sid_hexString), 0, 43));
                        account.storeObject("", sid);
                        return sid;
                    } catch (final Exception e) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, -1, e);
                    }
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } catch (Exception e) {
                apiLogoff(account);
                throw e;
            }
        }
    }

    private boolean isHideApplication() {
        return getPluginConfig().getBooleanProperty(HIDE_APP, true);
    }

    private Map<String, Object> apiRequest(Account account, final String sid, final UrlQuery additionalUrlQuery, final String action, final Object[]... postParams) throws Exception {
        final UrlQuery query = new UrlQuery();
        query.add("id", UniqueAlltimeID.create());
        if (StringUtils.isNotEmpty(sid)) {
            query.add("sid", sid);
        }
        if (additionalUrlQuery != null) {
            query.addAll(additionalUrlQuery.list());
        }
        final PostRequest request = new PostRequest(getAPI() + "/cs?" + query);
        if (!isHideApplication()) {
            request.getHeaders().put("APPID", "JDownloader");
        } else {
            request.getHeaders().put(new HTTPHeader("User-Agent", null, false));
            request.getHeaders().put(new HTTPHeader("Accept", null, false));
            request.getHeaders().put(new HTTPHeader("Accept-Language", null, false));
            request.getHeaders().put(new HTTPHeader("Accept-Encoding", null, false));
            request.getHeaders().put(new HTTPHeader("Cache-Control", null, false));
        }
        request.setContentType("text/plain;charset=UTF-8");
        if (postParams != null) {
            final HashMap<String, Object> sendParams = new HashMap<String, Object>();
            sendParams.put("a", action);
            for (final Object[] postParam : postParams) {
                if (postParam != null && postParam.length == 2) {
                    sendParams.put(String.valueOf(postParam[0]), postParam[1]);
                }
            }
            if (sendParams.size() > 0) {
                final List<Map<String, Object>> postData = new ArrayList<Map<String, Object>>();
                postData.add(sendParams);
                request.setPostDataString(JSonStorage.toString(postData));
                br.getPage(request);
                final List<Object> requestResponse = JSonStorage.restoreFromString(request.getHtmlCode(), TypeRef.LIST, null);
                if (requestResponse != null && requestResponse.size() == 1) {
                    final Object response = requestResponse.get(0);
                    if (response instanceof Map) {
                        return (Map<String, Object>) response;
                    }
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (request.getHtmlCode().contains("-16") && "us".equalsIgnoreCase(action)) {
            // API_EBLOCKED (-16): User blocked
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "User blocked", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (request.getHtmlCode().contains("-9") && "us".equalsIgnoreCase(action)) {
            // API_EOENT (-9): Object (typically, node or user) not found
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "User not found", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (request.getHtmlCode().contains("-15")) {
            // API_ESID (-15): Invalid or expired user session, please relogin
            if (sid != null && account != null) {
                synchronized (account) {
                    if (sid.equals(account.restoreObject("", TypeRef.STRING))) {
                        account.clearObject("");
                    }
                }
                if ("us".equalsIgnoreCase(action)) {
                    return null;
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid or expired user session,please relogin", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else if (request.getHtmlCode().contains("-3") || request.getHtmlCode().contains("-4")) {
            // API_EAGAIN (-3): A temporary congestion or server malfunction prevented your request from being processed. No data was
            // altered. Retry. Retries must be spaced with exponential backoff.
            // API_ERATELIMIT (-4):You have exceeded your command weight per time quota. Please wait a few seconds, then try again (this
            // should never happen in sane real-life applications).
            if (sid != null && account != null) {
                synchronized (account) {
                    if (sid.equals(account.restoreObject("", TypeRef.STRING))) {
                        account.clearObject("");
                    }
                }
                if ("us".equalsIgnoreCase(action)) {
                    return null;
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
            } else {
                if (request.getHtmlCode().contains("-3")) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "A temporary issue. Retry again later", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "You have exceeded your command weight per time quota. Retry again later", 5 * 60 * 1000l);
                }
            }
        }
        return null;
    }

    public static byte[] rsa_ecb_decrypt_aByte(PrivateKey privateKey, byte[] aByte_data) throws Exception {
        final Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(aByte_data);
    }

    public static long[] decrypt_aLong(final long[] aLong_key, final long[] aLong_data) throws Exception {
        final long[] aLong = new long[aLong_data.length];
        for (int i = 0; i < aLong_data.length; i += 4) {
            final long[] aLong_decrypt = aes_cbc_decrypt_aLong(aLong_key, aLong_data[i], aLong_data[i + 1], aLong_data[i + 2], aLong_data[i + 3]);
            for (int j = i; j < i + 4; j++) {
                aLong[j] = aLong_decrypt[j - i];
            }
        }
        return aLong;
    }

    public static long[] prepare_key_aLong(long[] aLong) throws Exception {
        long[] result_aLong = { 0x93C467E3, 0x7DB0C7A4, 0xD1BE3F81, 0x0152CB56 };
        for (int round = 0; round < 0x10000; round++) {
            for (int j = 0; j < aLong.length; j += 4) {
                final long[] loop_aLong = { 0, 0, 0, 0 };
                for (int i = 0; i < 4; i++) {
                    if (i + j < aLong.length) {
                        loop_aLong[i] = aLong[i + j];
                    }
                }
                result_aLong = aes_cbc_encrypt_aLong(loop_aLong, result_aLong);
            }
        }
        return result_aLong;
    }

    public static String calcuate_login_hash_uh(final String email, final long[] password) throws Exception {
        final long[] email_aLong = String_to_aLong(email);
        long[] hash_aLong = { 0, 0, 0, 0 };
        for (int i = 0; i < email_aLong.length; i++) {
            hash_aLong[i % 4] ^= email_aLong[i];
        }
        for (int r = 0; r < 0x4000; r++) {
            hash_aLong = aes_cbc_encrypt_aLong(password, hash_aLong);
        }
        return aLong_to_Base64(hash_aLong[0], hash_aLong[2]);
    }

    public static String aLong_to_Base64(final long... aLong) {
        return Base64.encodeToString(aLong_to_aByte(aLong), false);
    }

    public static long[] Base64_to_aLong(final String base64) {
        return aByte_to_aLong(b64decode(base64));
    }

    public static byte[] aes_cbc_aByte(final int mode, final byte[] aByte_key, final byte[] aByte_data) throws Exception {
        final IvParameterSpec ivSpec = new IvParameterSpec(aInt_to_aByte(0, 0, 0, 0));
        final SecretKeySpec keySpec = new SecretKeySpec(aByte_key, "AES");
        final Cipher cipher = Cipher.getInstance("AES/CBC/NOPADDING");
        cipher.init(mode, keySpec, ivSpec);
        return cipher.doFinal(aByte_data);
    }

    public static byte[] aes_cbc_encrypt_aByte(final byte[] aByte_key, final byte[] aByte_data) throws Exception {
        return aes_cbc_aByte(Cipher.ENCRYPT_MODE, aByte_data, aByte_data);
    }

    public static byte[] aes_cbc_decrypt_aByte(final byte[] aByte_key, final byte[] aByte_data) throws Exception {
        return aes_cbc_aByte(Cipher.DECRYPT_MODE, aByte_data, aByte_data);
    }

    public static long[] aes_cbc_aLong(final int mode, final long[] aLong_key, final long... aLong_data) throws Exception {
        final byte[] aByte_key = aLong_to_aByte(aLong_key);
        final byte[] aByte_data = aLong_to_aByte(aLong_data);
        final byte[] result = aes_cbc_aByte(mode, aByte_key, aByte_data);
        return aByte_to_aLong(result);
    }

    public static long[] aes_cbc_encrypt_aLong(final long[] aLong_key, final long... aLong_data) throws Exception {
        return aes_cbc_aLong(Cipher.ENCRYPT_MODE, aLong_key, aLong_data);
    }

    public static long[] aes_cbc_decrypt_aLong(final long[] aLong_key, final long... aLong_data) throws Exception {
        return aes_cbc_aLong(Cipher.DECRYPT_MODE, aLong_key, aLong_data);
    }

    public static long[] String_to_aLong(final String string) throws IOException {
        final byte[] stringBytes = string.getBytes("ISO-8859-1");
        return aByte_to_aLong(stringBytes);
    }

    public static long[] aByte_to_aLong(final byte[] aByte) {
        final int padding = aByte.length % 4;
        final byte[] padded_aByte;
        if (padding % 4 != 0) {
            padded_aByte = new byte[aByte.length + (4 - padding)];
            System.arraycopy(aByte, 0, padded_aByte, 0, aByte.length);
        } else {
            padded_aByte = aByte;
        }
        final long[] aLong = new long[padded_aByte.length / 4];
        final byte[] tmp = new byte[8];
        for (int stringBytesIndex = 0; stringBytesIndex < padded_aByte.length; stringBytesIndex += 4) {
            System.arraycopy(padded_aByte, stringBytesIndex, tmp, 4, 4);
            final ByteBuffer bb = ByteBuffer.wrap(tmp);
            aLong[stringBytesIndex / 4] = bb.getLong();
        }
        return aLong;
    }

    public static byte[] aLong_to_aByte(final long... aLong) {
        final ByteArrayOutputStream aByte = new ByteArrayOutputStream(aLong.length * 4);
        final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[8]);
        for (int i = 0; i < aLong.length; i++) {
            byteBuffer.putLong(aLong[i]);
            aByte.write(byteBuffer.array(), 4, 4);
            byteBuffer.clear();
        }
        return aByte.toByteArray();
    }

    public static String aLong_to_String(final long... aLong) throws IOException {
        return new String(aLong_to_aByte(aLong), "ISO-8859-1");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        String url = link.getPluginPatternMatcher();
        final String linkID = link.getSetLinkID();
        if (url.contains(".nz/$!")) {
            url = url.replaceFirst("nz/\\$!", "nz/#!");
            link.setUrlDownload(url);
        }
        final String fileID = getPublicFileID(link);
        if (linkID == null) {
            if (fileID != null) {
                link.setLinkID(getHost() + "F" + fileID);
            } else {
                final String nodeID = getNodeFileID(link);
                if (nodeID != null) {
                    link.setProperty("public", false);
                    link.setLinkID(getHost() + "N" + nodeID);
                }
            }
        }
        if (StringUtils.startsWithCaseInsensitive(url, "chrome:") || StringUtils.startsWithCaseInsensitive(url, "mega:")) {
            final String keyString = getPublicFileKey(link);
            if (fileID != null && keyString != null) {
                link.setUrlDownload("https://mega.co.nz/#!" + fileID + "!" + keyString);
            }
        } else {
            link.setUrlDownload(url.replaceAll("%21", "!").replaceAll("%20", ""));
        }
        String parentNode = link.getStringProperty("pn", null);
        if (parentNode == null) {
            parentNode = getParentNodeID(link);
            if (parentNode != null) {
                link.setProperty("public", false);
                link.setProperty("pn", parentNode);
            }
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink != null) {
            if (!StringUtils.equals((String) downloadLink.getProperty("usedPlugin", getHost()), getHost())) {
                return false;
            }
        }
        return super.canHandle(downloadLink, account);
    }

    private String getFolderID(final String url) {
        return new Regex(url, "#F\\!([a-zA-Z0-9]+)\\!").getMatch(0);
    }

    private final boolean isPublic(final DownloadLink downloadLink) {
        return downloadLink.getBooleanProperty("public", true);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        setBrowserExclusive();
        String fileID = getPublicFileID(link);
        String keyString = getPublicFileKey(link);
        if (fileID == null) {
            fileID = getNodeFileID(link);
            keyString = getNodeFileKey(link);
            if (isPublic(link)) {
                // update existing links
                link.setProperty("public", false);
            }
        }
        if (fileID == null || keyString == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        Map<String, Object> response = null;
        try {
            final String parentNode = getParentNodeID(link);
            if (Thread.currentThread() instanceof SingleDownloadController) {
                final Account account = ((SingleDownloadController) (Thread.currentThread())).getAccount();
                if (account != null && getHost().equals(account.getHosterByPlugin())) {
                    response = apiRequest(account, getSID(account), parentNode != null ? (UrlQuery.parse("n=" + parentNode)) : null, "g", new Object[] { isPublic(link) ? "p" : "n", fileID });
                } else {
                    response = apiRequest(null, null, parentNode != null ? (UrlQuery.parse("n=" + parentNode)) : null, "g", new Object[] { isPublic(link) ? "p" : "n", fileID });
                }
            } else {
                response = apiRequest(null, null, parentNode != null ? (UrlQuery.parse("n=" + parentNode)) : null, "g", new Object[] { isPublic(link) ? "p" : "n", fileID });
            }
        } catch (IOException e) {
            logger.log(e);
            checkServerBusy();
            throw e;
        }
        if (response == null) {
            final String error = getError(br);
            if ("-6".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if ("-9".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if ("-11".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if ("-16".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            checkServerBusy();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unhandled error code: " + error);
        }
        final String fileSize = valueOf(response.get("s"));
        if (fileSize == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            link.setDownloadSize(Long.parseLong(fileSize));
            try {
                link.setVerifiedFileSize(Long.parseLong(fileSize));
            } catch (final Throwable e) {
            }
        }
        final String at = valueOf(response.get("at"));
        if (at == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> fileInfo;
        try {
            fileInfo = decrypt(at, keyString);
        } catch (final StringIndexOutOfBoundsException e) {
            /* key is incomplete */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fileName = valueOf(fileInfo.get("n"));
        if (fileName == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setFinalFileName(fileName.replaceAll("\\\\", ""));
        try {
            if (link.getInternalTmpFilename() == null) {
                link.setInternalTmpFilenameAppend(encrypted);
            }
        } catch (final Throwable e) {
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public long calculateAdditionalRequiredDiskSpace(DownloadLink link) {
        final long finalSize = link.getVerifiedFileSize();
        return finalSize >= 0 ? finalSize : 0;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        apiDownload(link, account);
    }

    private final String getSID(final Account account) throws Exception {
        if (account != null) {
            synchronized (account) {
                final String storedSid = account.restoreObject("", TypeRef.STRING);
                if (storedSid != null) {
                    return storedSid;
                } else {
                    return apiLogin(account);
                }
            }
        } else {
            return null;
        }
    }

    private void apiDownload(DownloadLink link, Account account) throws Exception {
        final AvailableStatus available = requestFileInformation(link);
        if (AvailableStatus.FALSE == available) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (AvailableStatus.TRUE != available) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is Busy", 1 * 60 * 1000l);
        }
        final String sid = getSID(account);
        String fileID = getPublicFileID(link);
        String keyString = getPublicFileKey(link);
        if (fileID == null) {
            fileID = getNodeFileID(link);
            keyString = getNodeFileKey(link);
        }
        // check finished encrypted file. if the decryption interrupted - for whatever reason
        final String path = link.getFileOutput();
        final File src = new File(path);
        final AtomicLong encryptionDone = new AtomicLong(link.getVerifiedFileSize());
        final DiskSpaceReservation reservation = new DiskSpaceReservation() {

            @Override
            public long getSize() {
                return Math.max(0, encryptionDone.get());
            }

            @Override
            public File getDestination() {
                return src;
            }
        };
        try {
            checkAndReserve(link, reservation);
            if (src.exists() && src.length() == link.getVerifiedFileSize()) {
                // ready for decryption
                decrypt(path, encryptionDone, link, keyString);
                link.getLinkStatus().setStatus(LinkStatus.FINISHED);
                return;
            }
            try {
                if (fileID == null || keyString == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String parentNode = getParentNodeID(link);
                final Map<String, Object> response = apiRequest(account, sid, parentNode != null ? (UrlQuery.parse("n=" + parentNode)) : null, "g", new Object[] { "g", "1" }, new Object[] { "ssl", useSSL() }, new Object[] { isPublic(link) ? "p" : "n", fileID });
                final String downloadURL;
                if (response != null) {
                    downloadURL = valueOf(response.get("g"));
                } else {
                    downloadURL = null;
                }
                if (downloadURL == null) {
                    final String error = getError(br);
                    /*
                     * https://mega.co.nz/#doc
                     */
                    if ("-3".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "A temporary issue. Retry again later", 5 * 60 * 1000l);
                    }
                    if ("-4".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "You have exceeded your command weight per time quota. Retry again later", 5 * 60 * 1000l);
                    }
                    if ("-6".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry again later", 5 * 60 * 1000l);
                    }
                    if ("-11".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Access violation", 5 * 60 * 1000l);
                    }
                    if ("-16".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "User blocked");
                    }
                    if ("-17".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Request over quota", 60 * 60 * 1000l);
                    }
                    if ("-18".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Resource temporarily not available, please try again later", 5 * 60 * 1000l);
                    }
                    checkServerBusy();
                    logger.info("Unhandled error code: " + error);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unhandled error code: " + error);
                }
                if (isHideApplication()) {
                    br.setRequest(null);
                    br.getHeaders().put(new HTTPHeader("User-Agent", null, false));
                    br.getHeaders().put(new HTTPHeader("Accept", null, false));
                    br.getHeaders().put(new HTTPHeader("Accept-Language", null, false));
                    br.getHeaders().put(new HTTPHeader("Accept-Encoding", null, false));
                    br.getHeaders().put(new HTTPHeader("Cache-Control", null, false));
                }
                /* mega does not like much connections! */
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadURL, true, -10);
                if (dl.getConnection().getResponseCode() == 503) {
                    dl.getConnection().disconnect();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many connections", 5 * 60 * 1000l);
                }
                if (dl.getConnection().getResponseCode() == 509 || StringUtils.containsIgnoreCase(dl.getConnection().getResponseMessage(), "Bandwidth Limit Exceeded")) {
                    dl.getConnection().disconnect();
                    final String timeLeftString = dl.getConnection().getHeaderField("X-MEGA-Time-Left");
                    final long timeLeft;
                    if (timeLeftString != null && timeLeftString.matches("^\\d+$")) {
                        timeLeft = Math.max(60 * 1000l, Long.parseLong(timeLeftString) * 1000l);
                    } else {
                        timeLeft = 60 * 60 * 1000l;
                    }
                    if (account != null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Bandwidth Limit Exceeded", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Bandwidth Limit Exceeded", timeLeft);
                }
                if (StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "html")) {
                    br.followConnection();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                link.setProperty("usedPlugin", getHost());
                if (dl.startDownload()) {
                    if (link.getLinkStatus().hasStatus(LinkStatus.FINISHED) && link.getDownloadCurrent() > 0) {
                        decrypt(path, encryptionDone, link, keyString);
                    }
                }
            } catch (IOException e) {
                checkServerBusy();
                if (dl != null && dl.getConnection() != null && dl.getConnection().getResponseCode() == 500) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is Busy", 1 * 60 * 1000l);
                }
                throw e;
            }
        } finally {
            free(link, reservation);
        }
    }

    @Override
    public Downloadable newDownloadable(final DownloadLink downloadLink, final Browser br) {
        return new DownloadLinkDownloadable(downloadLink) {

            @Override
            public Browser getContextBrowser() {
                return br.cloneBrowser();
            }

            @Override
            public boolean isHashCheckEnabled() {
                return false;
            }
        };
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        apiDownload(link, null);
    }

    @Override
    public void preHandle(DownloadLink downloadLink, Account account, PluginForHost pluginForHost) throws Exception {
        if (downloadLink != null && pluginForHost != null && !StringUtils.equalsIgnoreCase(getHost(), pluginForHost.getHost())) {
            downloadLink.setInternalTmpFilenameAppend(null);
            downloadLink.setInternalTmpFilename(null);
            downloadLink.setProperty("ALLOW_HASHCHECK", Property.NULL);
        }
        super.preHandle(downloadLink, account, pluginForHost);
    }

    /**
     * @throws PluginException
     */
    public void checkServerBusy() throws PluginException {
        if (br.getRequest() != null && br.getRequest().getHttpConnection() != null && br.getRequest().getHttpConnection() != null && br.getRequest().getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is Busy", 10 * 60 * 1000l);
        }
    }

    private Map<String, Object> decrypt(String input, String keyString) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, PluginException {
        byte[] b64Dec = b64decode(keyString);
        int[] intKey = aByte_to_aInt(b64Dec);
        if (intKey.length < 8) {
            /* key is not given in link */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        byte[] key = aInt_to_aByte(intKey[0] ^ intKey[4], intKey[1] ^ intKey[5], intKey[2] ^ intKey[6], intKey[3] ^ intKey[7]);
        byte[] iv = aInt_to_aByte(0, 0, 0, 0);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/nopadding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
        byte[] unPadded = b64decode(input);
        int len = 16 - ((unPadded.length - 1) & 15) - 1;
        byte[] payLoadBytes = new byte[unPadded.length + len];
        System.arraycopy(unPadded, 0, payLoadBytes, 0, unPadded.length);
        payLoadBytes = cipher.doFinal(payLoadBytes);
        String ret = new String(payLoadBytes, "UTF-8");
        if (ret != null && !ret.startsWith("MEGA{")) {
            /* verify if the keyString is correct */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return JSonStorage.restoreFromString(ret.substring(4), TypeRef.HASHMAP, null);
    }

    public String getError(Browser br) {
        if (br == null) {
            return null;
        }
        String ret = br.getRegex("\"e\"\\s*?:\\s*?(-?\\d+)").getMatch(0);
        if (ret == null) {
            ret = br.getRegex("\\s*\\[\\s*(-?\\d+)\\s*\\]").getMatch(0);
        }
        if (ret == null) {
            ret = br.getRegex("\\s*(-?\\d+)").getMatch(0);
        }
        return ret;
    }

    private final static String USE_GLOBAL_CDN = "USE_GLOBAL_CDN";

    private final String getAPI() {
        if (getPluginConfig().getBooleanProperty(USE_GLOBAL_CDN, true)) {
            return "https://g.api.mega.co.nz";
        } else {
            return "https://eu.api.mega.co.nz";
        }
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CHECK_RESERVED, JDL.L("plugins.hoster.megaconz.checkreserved", "Check reserved traffic?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_SSL, JDL.L("plugins.hoster.megaconz.usessl", "Use SSL?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_TMP, JDL.L("plugins.hoster.megaconz.usetmp", "Use tmp decrypting file?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), HIDE_APP, JDL.L("plugins.hoster.megaconz.hideapp", "Use minimal set of http headers?")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USE_GLOBAL_CDN, JDL.L("plugins.hoster.megaconz.globalcdn", "Use global CDN?")).setDefaultValue(true));
    }

    private static Object DECRYPTLOCK = new Object();

    private void decrypt(final String path, AtomicLong encryptionDone, DownloadLink link, String keyString) throws Exception {
        byte[] b64Dec = b64decode(keyString);
        int[] intKey = aByte_to_aInt(b64Dec);
        int[] keyNOnce = new int[] { intKey[0] ^ intKey[4], intKey[1] ^ intKey[5], intKey[2] ^ intKey[6], intKey[3] ^ intKey[7], intKey[4], intKey[5] };
        byte[] key = aInt_to_aByte(keyNOnce[0], keyNOnce[1], keyNOnce[2], keyNOnce[3]);
        int[] iiv = new int[] { keyNOnce[4], keyNOnce[5], 0, 0 };
        byte[] iv = aInt_to_aByte(iiv);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        File dst = null;
        File src = null;
        File tmp = null;
        if (path.endsWith(encrypted)) {
            src = new File(path);
            String path2 = path.substring(0, path.length() - encrypted.length());
            if (useTMP()) {
                tmp = new File(path2 + ".decrypted");
            }
            dst = new File(path2);
        } else {
            src = new File(path);
            tmp = new File(path + ".decrypted");
            dst = new File(path);
        }
        if (tmp != null) {
            if (tmp.exists() && tmp.delete() == false) {
                throw new IOException("Could not delete " + tmp);
            }
        } else {
            if (dst.exists() && dst.delete() == false) {
                throw new IOException("Could not delete " + dst);
            }
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;
        boolean deleteDst = true;
        final long total = src.length();
        final AtomicReference<String> message = new AtomicReference<String>();
        final PluginProgress progress = new PluginProgress(0, total, null) {

            long lastCurrent    = -1;
            long startTimeStamp = -1;

            public String getMessage(Object requestor) {
                return message.get();
            }

            @Override
            public PluginTaskID getID() {
                return PluginTaskID.DECRYPTING;
            }

            @Override
            public void updateValues(long current, long total) {
                super.updateValues(current, total);
                if (lastCurrent == -1 || lastCurrent > current) {
                    lastCurrent = current;
                    startTimeStamp = System.currentTimeMillis();
                    this.setETA(-1);
                    return;
                }
                long currentTimeDifference = System.currentTimeMillis() - startTimeStamp;
                if (currentTimeDifference <= 0) {
                    return;
                }
                long speed = (current * 10000) / currentTimeDifference;
                if (speed == 0) {
                    return;
                }
                long eta = ((total - current) * 10000) / speed;
                this.setETA(eta);
            }
        };
        progress.setProgressSource(this);
        progress.setIcon(new AbstractIcon(IconKey.ICON_LOCK, 16));
        final File outputFile;
        if (tmp != null) {
            outputFile = tmp;
        } else {
            outputFile = dst;
        }
        try {
            try {
                message.set("Queued for decryption");
                link.addPluginProgress(progress);
                synchronized (DECRYPTLOCK) {
                    message.set("Decrypting");
                    fis = new FileInputStream(src);
                    FileStateManager.getInstance().requestFileState(outputFile, FILESTATE.WRITE_EXCLUSIVE, this);
                    fos = new FileOutputStream(outputFile);
                    final Cipher cipher = Cipher.getInstance("AES/CTR/nopadding");
                    cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
                    final CipherOutputStream cos = new CipherOutputStream(fos, cipher);
                    int read = 0;
                    final byte[] buffer = new byte[32767];
                    while ((read = fis.read(buffer)) != -1) {
                        if (read > 0) {
                            progress.updateValues(progress.getCurrent() + read, total);
                            cos.write(buffer, 0, read);
                            encryptionDone.addAndGet(-read);
                        }
                    }
                    cos.close();
                    try {
                        fos.close();
                    } catch (final Throwable e) {
                    }
                    try {
                        fis.close();
                    } catch (final Throwable e) {
                    }
                    deleteDst = false;
                    link.getLinkStatus().setStatusText("Finished");
                    try {
                        link.setInternalTmpFilenameAppend(null);
                        link.setInternalTmpFilename(null);
                    } catch (final Throwable e) {
                    }
                    if (tmp == null) {
                        src.delete();
                    } else {
                        src.delete();
                        tmp.renameTo(dst);
                    }
                    new MegaHashCheck(link, dst).finalHashResult();
                }
            } finally {
                try {
                    fis.close();
                } catch (final Throwable e) {
                }
                try {
                    fos.close();
                } catch (final Throwable e) {
                }
                link.removePluginProgress(progress);
                if (deleteDst) {
                    if (tmp != null) {
                        tmp.delete();
                    } else {
                        dst.delete();
                    }
                }
            }
        } finally {
            FileStateManager.getInstance().releaseFileState(outputFile, this);
        }
    }

    private class MegaHashCheck extends DownloadInterface {

        private final DownloadLinkDownloadable downloadable;
        private final File                     finalFile;

        private MegaHashCheck(DownloadLink link, final File finalFile) {
            downloadable = new DownloadLinkDownloadable(link) {

                @Override
                public boolean isHashCheckEnabled() {
                    return true;
                }
            };
            this.finalFile = finalFile;
        }

        private void finalHashResult() throws Exception {
            final HashResult hashResult = getHashResult(downloadable, finalFile);
            if (hashResult != null) {
                logger.info(hashResult.toString());
            }
            downloadable.setHashResult(hashResult);
            if (hashResult == null || hashResult.match()) {
                downloadable.setVerifiedFileSize(finalFile.length());
            } else {
                if (hashResult.getHashInfo().isTrustworthy()) {
                    throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.system_download_doCRC2_failed(hashResult.getHashInfo().getType()));
                }
            }
        }

        @Override
        public ManagedThrottledConnectionHandler getManagedConnetionHandler() {
            return null;
        }

        @Override
        public URLConnectionAdapter connect(Browser br) throws Exception {
            return null;
        }

        @Override
        public long getTotalLinkBytesLoadedLive() {
            return 0;
        }

        @Override
        public boolean startDownload() throws Exception {
            return false;
        }

        @Override
        public URLConnectionAdapter getConnection() {
            return null;
        }

        @Override
        public void stopDownload() {
        }

        @Override
        public boolean externalDownloadStop() {
            return false;
        }

        @Override
        public long getStartTimeStamp() {
            return 0;
        }

        @Override
        public void close() {
        }

        @Override
        public Downloadable getDownloadable() {
            return downloadable;
        }

        @Override
        public boolean isResumedDownload() {
            return false;
        }
    }

    private String getPublicFileID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "#(!|%21)([a-zA-Z0-9]+)(!|%21)").getMatch(1);
    }

    private String getPublicFileKey(DownloadLink link) {
        String ret = new Regex(link.getDownloadURL(), "#(!|%21)[a-zA-Z0-9]+(!|%21)([a-zA-Z0-9_,\\-%]+)").getMatch(2);
        if (ret != null && ret.contains("%20")) {
            ret = ret.replace("%20", "");
        }
        return ret;
    }

    private String getNodeFileID(DownloadLink link) {
        return new Regex(link.getDownloadURL(), "#N(!|%21)([a-zA-Z0-9]+)(!|%21)").getMatch(1);
    }

    private String getParentNodeID(DownloadLink link) {
        final String ret = new Regex(link.getDownloadURL(), "(=###n=|!|%21)([a-zA-Z0-9]+)$").getMatch(1);
        final String publicFileKey = getPublicFileKey(link);
        final String nodeFileKey = getNodeFileKey(link);
        if (ret != null && !ret.equals(publicFileKey) && !ret.equals(nodeFileKey)) {
            return ret;
        }
        return link.getStringProperty("pn", null);
    }

    private String getNodeFileKey(DownloadLink link) {
        String ret = new Regex(link.getDownloadURL(), "#N(!|%21)[a-zA-Z0-9]+(!|%21)([a-zA-Z0-9_,\\-%]+)").getMatch(2);
        if (ret != null && ret.contains("%20")) {
            ret = ret.replace("%20", "");
        }
        return ret;
    }

    public static byte[] b64decode(String data) {
        data += "==".substring((2 - data.length() * 3) & 3);
        data = data.replace("-", "+").replace("_", "/").replace(",", "");
        return Base64.decode(data);
    }

    public static String b64encode(byte[] data) {
        final String ret = Base64.encodeToString(data, true);
        return ret.replaceAll("\\+", "-").replaceAll("/", "_").replaceAll("=", "");
    }

    public static byte[] aInt_to_aByte(final int... aInt) {
        final byte[] buffer = new byte[aInt.length * 4];
        final ByteBuffer bb = ByteBuffer.wrap(buffer);
        for (int i = 0; i < aInt.length; i++) {
            bb.putInt(aInt[i]);
        }
        return bb.array();
    }

    public static int[] aByte_to_aInt(byte[] bytes) {
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        final int[] aInt = new int[bytes.length / 4];
        for (int i = 0; i < aInt.length; i++) {
            aInt[i] = bb.getInt(i * 4);
        }
        return aInt;
    }

    private String useSSL() {
        if (getPluginConfig().getBooleanProperty(USE_SSL, false)) {
            if (isHideApplication()) {
                return "2";// can also be 2, see meganz/webclient/blob/master/js/crypto.js
            } else {
                return "1";
            }
        } else {
            return "0";
        }
    }

    private boolean useTMP() {
        if (getPluginConfig().getBooleanProperty(USE_TMP, false)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        return false;
    }

    @Override
    public String buildExternalDownloadURL(DownloadLink downloadLink, PluginForHost buildForThisPlugin) {
        if (isPublic(downloadLink)) {
            return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
        } else {
            if (StringUtils.equals(getHost(), buildForThisPlugin.getHost())) {
                return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
            } else if (StringUtils.equals("real-debrid.com", buildForThisPlugin.getHost())) {
                String fileID = getPublicFileID(downloadLink);
                String keyString = getPublicFileKey(downloadLink);
                if (fileID == null) {
                    fileID = getNodeFileID(downloadLink);
                    keyString = getNodeFileKey(downloadLink);
                }
                return "https://mega.co.nz/#!" + fileID + "!" + keyString + "!" + getParentNodeID(downloadLink);
            } else {
                return null;
            }
        }
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        if (downloadLink != null) {
            if (plugin != null) {
                if (!StringUtils.equals(downloadLink.getStringProperty("usedPlugin", plugin.getHost()), plugin.getHost())) {
                    return false;
                }
            }
            return buildExternalDownloadURL(downloadLink, plugin) != null;
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("usedPlugin", Property.NULL);
    }
}
