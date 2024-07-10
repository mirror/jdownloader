package jd.plugins.hoster;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Base64;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
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

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Exceptions;
import org.appwork.utils.JDK8BufferHelper;
import org.appwork.utils.JVMVersion;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.InputDialog;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.jdownloader.controlling.FileStateManager;
import org.jdownloader.controlling.FileStateManager.FILESTATE;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.components.config.MegaConzConfig;
import org.jdownloader.plugins.components.config.MegaConzConfig.LimitMode;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.LazyPlugin.FEATURE;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.SIZEUNIT;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.translate._JDT;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mega.co.nz" }, urls = { "(https?://(www\\.)?mega\\.(co\\.)?nz/.*?(#!?N?|\\$)|chrome://mega/content/secure\\.html#)(!|%21|\\?)[a-zA-Z0-9]+(!|%21)[a-zA-Z0-9_,\\-%]{16,}((=###n=|!)[a-zA-Z0-9]+)?|mega:/*#(?:!|%21)[a-zA-Z0-9]+(?:!|%21)[a-zA-Z0-9_,\\-%]{16,}" })
public class MegaConz extends PluginForHost {
    private final String       USED_PLUGIN = "usedPlugin";
    private final String       encrypted   = ".encrypted";
    public final static String MAIN_DOMAIN = "mega.nz";

    @Override
    public String[] siteSupportedNames() {
        return new String[] { MAIN_DOMAIN, "mega.co.nz", "mega.io", "mega" };
    }

    public MegaConz(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://mega.nz/#register");
    }

    @Override
    public String getAGBLink() {
        return "https://mega.co.nz/#terms";
    }

    private Number getNumber(Map<String, Object> map, final String key, final Number defaultValue) {
        final Number ret = map.containsKey(key) ? getNumber(map, key) : null;
        if (ret == null) {
            return defaultValue;
        } else {
            return ret;
        }
    }

    private Number getNumber(Map<String, Object> map, final String key) {
        final Object ret = map.get(key);
        if (ret instanceof Number) {
            return (Number) ret;
        } else if (ret instanceof String) {
            return Long.parseLong(ret.toString());
        } else {
            return 0l;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (account) {
            final String sid = apiLogin(account);
            final Map<String, Object> uq = apiRequest(account, sid, null, "uq"/* userQuota */, new Object[] { "xfer"/* xfer */, 1 }, new Object[] { "pro"/* pro */, 1 });
            // mxfer - maximum transfer allowance
            // caxfer - PRO transfer quota consumed by yourself
            // csxfer - PRO transfer quota served to others
            // tuo - Transfer usage by the owner on quotad which hasn't yet been committed back to the API DB. Supplements caxfer
            // tua - Transfer usage served to other users which hasn't yet been committed back to the API DB. Supplements csxfer
            // srvratio - The ratio of your PRO transfer quota that is able to be served to others
            // utype - PRO type. 0 means Free; 4 is Pro Lite as it was added late; 100 indicates a business.
            // stype - Flag indicating if this is a recurring subscription or one-off. "O" is one off, "R" is recurring.
            // suntil - Time the last active PRO plan will expire (may be different from current one)
            // srenew - Only provided for recurring subscriptions to indicate the best estimate of when the subscription will renew
            // scycle - subscription_cycle
            // bt - "Base time age", this is number of seconds since the start of the current quota buckets
            // tah - the free IP-based quota buckets, 6 entries for 6 hours
            // tar - IP transfer reserved
            // rua - Actor reserved quota
            // ruo - Owner reserved quota
            // cstrg - Your total account storage usage
            // mstrg - maximum storage allowance
            // uslw - The percentage (in 1000s) indicating the limit at which you are 'nearly' over. Currently 98% for PRO, 90% for free.
            // balance - Balance of your account
            // rtt - ?
            // sgw - subscription ?
            // https://github.com/meganz/sdk/blob/master/src/commands.cpp
            // https://github.com/meganz/sdk/blob/master/bindings/ios/MEGAAccountDetails.h
            String statusAddition = "";
            if (false) {
                // TODO: what are the elements of tah? maybe last 15/30/60 minutes?
                try {
                    final List<Object> trafficInfo = (List<Object>) uq.get("tah");
                    long bandwidthUsed = ((Number) trafficInfo.get(trafficInfo.size() - 2)).longValue();
                    if (bandwidthUsed == 0) {
                        bandwidthUsed = ((Number) trafficInfo.get(trafficInfo.size() - 1)).longValue();
                    }
                    statusAddition += " | Bandwidth used: " + SIZEUNIT.formatValue((SIZEUNIT) CFG_GUI.MAX_SIZE_UNIT.getValue(), bandwidthUsed);
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
            if (uq == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Number utype = (Number) uq.get("utype");
            if (utype == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String subscriptionCycle;
            if (uq.containsKey("scycle")) {
                subscriptionCycle = " (Subscription cycle: " + String.valueOf(uq.get("scycle")) + ")";
            } else {
                subscriptionCycle = "";
            }
            final AccountInfo ai = new AccountInfo();
            boolean isPro = false;
            String accountStatus = null;
            // https://docs.mega.nz/sdk/api/classmega_1_1_mega_account_details.html
            final int accountType = utype.intValue();
            switch (accountType) {
            case 1:
                accountStatus = "Pro I Account" + subscriptionCycle;
                isPro = true;
                break;
            case 2:
                accountStatus = "Pro II Account" + subscriptionCycle;
                isPro = true;
                break;
            case 3:
                accountStatus = "Pro III Account" + subscriptionCycle;
                isPro = true;
                break;
            case 4:
                accountStatus = "Pro Lite Account" + subscriptionCycle;
                isPro = true;
                break;
            case 100:
                accountStatus = "Business Account" + subscriptionCycle;
                isPro = true;
                break;
            case 0:
                accountStatus = "Free Account";
                isPro = false;
                break;
            default:
                accountStatus = "Unknown Accounttype:" + accountType;
                isPro = false;
                break;
            }
            if (isPro && uq.containsKey("suntil")) {
                final Number expire = getNumber(uq, "suntil");
                ai.setValidUntil(expire.longValue() * 1000);
                if (ai.isExpired()) {
                    accountStatus += "(expired)";
                    isPro = false;
                }
            } else {
                isPro = false;
            }
            if (isPro && uq.containsKey("srvratio")) {
                final Number srvratio = getNumber(uq, "srvratio");
                final long transfer_srv_reserved = getNumber(uq, "ruo", 0l).longValue();// 3rd party
                if (srvratio.intValue() > 0) {
                    statusAddition += " | TrafficShare Ratio: " + String.format("%.02f", srvratio.floatValue()) + "% (" + SIZEUNIT.formatValue((SIZEUNIT) CFG_GUI.MAX_SIZE_UNIT.getValue(), transfer_srv_reserved) + ")";
                }
            }
            ai.setStatus(accountStatus + statusAddition);
            if (isPro) {
                if (uq.containsKey("mxfer")) {
                    final Number max = getNumber(uq, "mxfer");
                    final Number usedOwner = getNumber(uq, "caxfer", 0l);
                    final Number uncommitedOwner = getNumber(uq, "tuo", 0l);
                    final long transfer_own_used = usedOwner.longValue() + uncommitedOwner.longValue();
                    final Number usedServed = getNumber(uq, "csxfer", 0l);
                    final Number uncommitedServed = getNumber(uq, "tua", 0l);
                    final long transfer_srv_used = usedServed.longValue() + uncommitedServed.longValue();
                    final long reserved;
                    if (PluginJsonConfig.get(MegaConzConfig.class).isCheckReserverTraffic()) {
                        // Reserved transfer quota for ongoing transfers (currently ignored by clients)
                        final long transfer_srv_reserved = getNumber(uq, "rua", 0l).longValue();// own account
                        final long transfer_own_reserved = getNumber(uq, "ruo", 0l).longValue(); // 3rd party
                        // final long transfer_reserved = getNumber(uq, "tar", 0l).longValue(); //free IP-based
                        reserved = transfer_own_reserved + transfer_srv_reserved;
                    } else {
                        reserved = 0;
                    }
                    ai.setTrafficMax(max.longValue());
                    ai.setTrafficLeft(max.longValue() - (transfer_own_used + transfer_srv_used + reserved));
                }
                account.setType(AccountType.PREMIUM);
            } else {
                ai.setValidUntil(-1);
                account.setType(AccountType.FREE);
            }
            account.setRefreshTimeout(2 * 60 * 60 * 1000l);
            return ai;
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
                logger.log(ignore);
            }
        }
    }

    // // unfinished
    // private boolean queryBandwidthQuota(final Account account, final long fileSize) throws Exception {
    // apiRequest(account, getSID(account), null, "qbq"/* queryBandwidthQuota */, new Object[] { "s", fileSize });
    // return true;
    // }
    private String apiLogin(final Account account) throws Exception {
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
                long[] password_aes_aLong = null;
                if (response == null || !response.containsKey("privk")) {
                    /* fresh login */
                    final String lowerCaseEmail = email.toLowerCase(Locale.ENGLISH);
                    response = apiRequest(account, null, null, "us0"/* preLogIn */, new Object[] { "user"/* email */, lowerCaseEmail });
                    final Number v = (Number) response.get("v");
                    final String uh;
                    if (v.intValue() == 1) {
                        password_aes_aLong = prepare_key_aLong(new long[] { 0x93C467E3, 0x7DB0C7A4, 0xD1BE3F81, 0x0152CB56 }, String_to_aLong(password));
                        uh = calcuate_login_hash_uh(lowerCaseEmail, password_aes_aLong);
                    } else if (v.intValue() == 2) {
                        final String saltString = (String) response.get("s");
                        if (StringUtils.isEmpty(saltString)) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        byte[] pbkdf2Data = null;
                        final byte[] saltBytes = aLong_to_aByte(Base64_to_aLong(saltString));
                        if (JVMVersion.isMinimum(JVMVersion.JAVA18)) {
                            // java >=1.8
                            try {
                                final SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
                                final PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, 100000, 256);
                                pbkdf2Data = skf.generateSecret(spec).getEncoded();
                            } catch (NoSuchAlgorithmException e) {
                                getLogger().log(e);
                            }
                        }
                        if (pbkdf2Data == null) {
                            // bouncy castle
                            final PBEParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA512Digest());
                            generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password.toCharArray()), saltBytes, 100000);
                            KeyParameter params = (KeyParameter) generator.generateDerivedParameters(256);
                            pbkdf2Data = params.getKey();
                        }
                        final byte[] uhBytes = new byte[16];
                        System.arraycopy(pbkdf2Data, 16, uhBytes, 0, 16);
                        uh = aLong_to_Base64(aByte_to_aLong(uhBytes));
                        final byte[] passwordBytes = new byte[16];
                        System.arraycopy(pbkdf2Data, 0, passwordBytes, 0, 16);
                        password_aes_aLong = aByte_to_aLong(passwordBytes);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    try {
                        response = apiRequest(account, null, null, "us"/* logIn */, new Object[] { "user"/* email */, lowerCaseEmail }, new Object[] { "uh"/* emailHash */, uh });
                    } catch (PluginException e) {
                        if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM && e.getValue() == PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE && account.getBooleanProperty("mfa", Boolean.FALSE)) {
                            try {
                                final InputDialog mfaDialog = new InputDialog(UIOManager.LOGIC_COUNTDOWN, "Account is 2fa protected!", "Please enter the pin for your mega.nz account(" + account.getUser() + "):", null, null, _GUI.T.lit_continue(), null);
                                mfaDialog.setTimeout(5 * 60 * 1000);
                                final InputDialogInterface handler = UIOManager.I().show(InputDialogInterface.class, mfaDialog);
                                handler.throwCloseExceptions();
                                response = apiRequest(account, null, null, "us"/* logIn */, new Object[] { "user"/* email */, lowerCaseEmail }, new Object[] { "uh"/* emailHash */, uh }, new Object[] { "mfa"/* ping */, handler.getText() });
                            } catch (DialogNoAnswerException e2) {
                                throw Exceptions.addSuppressed(e, e2);
                            }
                        } else {
                            throw e;
                        }
                    }
                }
                if (response != null && (response.containsKey("k") && response.containsKey("privk") && response.containsKey("csid"))) {
                    try {
                        final String k = (String) response.get("k");
                        final String privk = (String) response.get("privk");
                        if (password_aes_aLong == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
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
            } catch (BrowserException e) {
                throw e;
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    apiLogoff(account);
                }
                throw e;
            }
        }
    }

    private Map<String, Object> apiRequest(Account account, final String sid, final UrlQuery additionalUrlQuery, final String action, final Object[]... postParams) throws Exception {
        if (postParams == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final UrlQuery query = new UrlQuery();
        query.add("id", UniqueAlltimeID.create());
        if (StringUtils.isNotEmpty(sid)) {
            query.add("sid", sid);
        }
        if (additionalUrlQuery != null) {
            query.addAll(additionalUrlQuery.list());
        }
        final PostRequest request = new PostRequest(getAPI() + "/cs?" + query);
        if (PluginJsonConfig.get(MegaConzConfig.class).isHideApplication()) {
            request.getHeaders().put(new HTTPHeader("User-Agent", null, false));
            request.getHeaders().put(new HTTPHeader("Accept", null, false));
            request.getHeaders().put(new HTTPHeader("Accept-Language", null, false));
            request.getHeaders().put(new HTTPHeader("Accept-Encoding", null, false));
            request.getHeaders().put(new HTTPHeader("Cache-Control", null, false));
        } else {
            request.getHeaders().put("APPID", "JDownloader");
        }
        request.setContentType("text/plain;charset=UTF-8");
        Integer errorCode = null;
        String requestResponseString = null;
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
            requestResponseString = br.getPage(request);
            if (requestResponseString.matches("^\\s*-?\\d+\\s*$")) {
                errorCode = Integer.parseInt(requestResponseString);
            } else if (requestResponseString.matches("^\\s*\\[.*")) {
                final List<Object> requestResponse = restoreFromString(requestResponseString, TypeRef.LIST);
                if (requestResponse != null && requestResponse.size() == 1) {
                    final Object responseObject = requestResponse.get(0);
                    if (responseObject instanceof Map) {
                        return (Map<String, Object>) responseObject;
                    } else if (responseObject instanceof Number) {
                        errorCode = ((Number) responseObject).intValue();
                    }
                }
            }
        }
        if (errorCode == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Response:" + requestResponseString);
        } else if (errorCode == -26 && "us".equalsIgnoreCase(action)) {
            // API_EMFAREQUIRED = -26, // Multi-factor authentication required
            synchronized (account) {
                account.setProperty("mfa", Boolean.TRUE);
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "2FA required", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        } else if (errorCode == -16 && ("us".equalsIgnoreCase(action) || "uq".equalsIgnoreCase(action))) {
            // API_EBLOCKED (-16): User blocked
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "User blocked", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (errorCode == -9 && "us".equalsIgnoreCase(action)) {
            // API_EOENT (-9): Object (typically, node or user) not found
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "User not found", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (errorCode == -15) {
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
        } else if (errorCode == -3 || errorCode == -4) {
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
                    throw new AccountUnavailableException("A temporary congestion or server malfunction prevented your request from being processed", 5 * 60 * 1000l);
                }
            } else {
                if (errorCode == -3) {
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

    public static long[] prepare_key_aLong(long[] salt, long[] aLong) throws Exception {
        long[] result_aLong = salt.clone();
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
            JDK8BufferHelper.clear(byteBuffer);
        }
        return aByte.toByteArray();
    }

    public static String aLong_to_String(final long... aLong) throws IOException {
        return new String(aLong_to_aByte(aLong), "ISO-8859-1");
    }

    @Override
    public Object getFavIcon(String host) throws IOException {
        // host/https://mega.co.nz redirect via javascript to https://mega.io/
        return "https://mega.io/";
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
                link.setUrlDownload("https://" + jd.plugins.hoster.MegaConz.MAIN_DOMAIN + "/#!" + fileID + "!" + keyString);
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
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (downloadLink != null) {
            final boolean isPremium = account != null && AccountType.PREMIUM.equals(account.getType());
            if (!StringUtils.equals((String) downloadLink.getProperty(USED_PLUGIN, getHost()), getHost())) {
                return false;
            } else if (!isPremium && downloadLink.getVerifiedFileSize() > 5368709120l && PluginJsonConfig.get(MegaConzConfig.class).is5GBFreeLimitEnabled()) {
                /**
                 * 2024-07-02: Skip files over 5GB as we cannot download them in free mode, see: </br>
                 * https://board.jdownloader.org/showthread.php?t=75268
                 */
                throw new AccountRequiredException();
            }
        }
        return super.canHandle(downloadLink, account);
    }

    private static String getFolderID(final String url) {
        return new Regex(url, "#F\\!([a-zA-Z0-9]+)\\!").getMatch(0);
    }

    private static final boolean isPublic(final DownloadLink downloadLink) {
        return downloadLink.getBooleanProperty("public", true);
    }

    private String getPreviewURL(final DownloadLink link) throws Exception {
        if (false) {
            // decryption still missing, see ticket 90021
            // type 0 = thumbnail
            // type 1 = preview
            final String fa = link.getStringProperty("fa");
            final String preview = new Regex(fa, "\\d+:1\\*([\\w-]+)").getMatch(0);
            if (preview != null) {
                try {
                    final Map<String, Object> response = apiRequest(null, null, null, "ufa", new Object[] { "fah", preview }, /* chunked */new Object[] { "r", "1" }, new Object[] { "ssl", "1" });
                    if (response != null && StringUtils.valueOrEmpty(StringUtils.valueOfOrNull(response.get("p"))).matches("(?i)https?://.+")) {
                        return StringUtils.valueOfOrNull(response.get("p"));
                    }
                } catch (IOException e) {
                    logger.log(e);
                }
            }
        }
        return null;
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
            /* Broken item or invalid URL -> File can only be offline. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> response = null;
        Account account = null;
        final String parentNode;
        try {
            parentNode = getParentNodeID(link);
            if (Thread.currentThread() instanceof SingleDownloadController) {
                account = ((SingleDownloadController) (Thread.currentThread())).getAccount();
                if (account != null && !getHost().equals(account.getHosterByPlugin())) {
                    account = null;
                }
            }
            if (account != null) {
                response = apiRequest(account, getSID(account), parentNode != null ? (UrlQuery.parse("n=" + parentNode)) : null, "g", new Object[] { isPublic(link) ? "p" : "n", fileID });
            } else {
                response = apiRequest(null, null, parentNode != null ? (UrlQuery.parse("n=" + parentNode)) : null, "g", new Object[] { isPublic(link) ? "p" : "n", fileID });
            }
        } catch (IOException e) {
            logger.log(e);
            checkServerBusy(br.getHttpConnection(), e);
            throw e;
        }
        if (response == null) {
            // https://github.com/meganz/sdk/blob/master/include/mega/types.h
            final String error = getError(br);
            if ("-2".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Invalid URL format");
            } else if ("-6".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Too many requests for this resource");
            } else if ("-7".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Resource access out of range");
            } else if ("-8".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Resource expired");
            } else if ("-9".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Resource does not exist");
            } else if ("-11".equals(error)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Access denied");
            } else if ("-16".equals(error)) {
                // file offline, maybe preview is still available
                if (getPreviewURL(link) != null) {
                    try {
                        if (link.getInternalTmpFilename() == null) {
                            link.setInternalTmpFilenameAppend(encrypted);
                        }
                    } catch (final Throwable e) {
                    }
                    if (link.getFinalFileName() != null) {
                        link.setProperty("fallbackFilename", link.getFinalFileName());
                    } else {
                        link.setFinalFileName(link.getStringProperty("fallbackFilename", null));
                    }
                    return AvailableStatus.TRUE;
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Resource administratively blocked");
                }
            }
            checkServerBusy(br.getHttpConnection(), null);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unhandled error code: " + error);
        }
        final String fileSize = valueOf(response.get("s"));
        if (fileSize == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            link.setDownloadSize(Long.parseLong(fileSize));
            link.setVerifiedFileSize(Long.parseLong(fileSize));
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
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, null, e);
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

    private void apiDownload(final DownloadLink link, final Account account) throws Exception {
        boolean resume = true;
        if (link.getDownloadCurrent() > 0 && !StringUtils.equalsIgnoreCase(getHost(), link.getStringProperty(USED_PLUGIN, getHost()))) {
            logger.info("Resume impossible due to previous multihoster download:" + link.getDownloadCurrent() + "/" + link.getKnownDownloadSize() + "|" + link.getStringProperty(USED_PLUGIN));
            if (PluginJsonConfig.get(MegaConzConfig.class).isAllowStartFromZeroIfDownloadWasStartedViaMultihosterBefore()) {
                logger.info("Auto-download from scratch = true");
                resume = false;
            } else {
                logger.info("Auto-download from scratch = false --> Throwing Exception");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Resume impossible! Either resume via previously used multihost or reset and start from scratch! See MEGA plugin settings if you want to auto-start from scratch!");
            }
        }
        final AvailableStatus available = requestFileInformation(link);
        if (AvailableStatus.FALSE == available) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (AvailableStatus.TRUE != available) {
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

            @Override
            public Object getOwner() {
                return MegaConz.this;
            }

            @Override
            public LogInterface getLogger() {
                return MegaConz.this.getLogger();
            }
        };
        final AtomicBoolean successfulFlag = new AtomicBoolean(false);
        try {
            checkAndReserve(link, reservation);
            if (src.exists() && src.length() == link.getVerifiedFileSize()) {
                // ready for decryption
                decrypt(path, encryptionDone, successfulFlag, link, keyString);
                link.getLinkStatus().setStatus(LinkStatus.FINISHED);
                return;
            }
            successfulFlag.set(false);
            try {
                if (fileID == null || keyString == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String parentNode = getParentNodeID(link);
                final boolean unsupportedRaidDownloadV2 = false;
                final Map<String, Object> response = apiRequest(account, sid, parentNode != null ? (UrlQuery.parse("n=" + parentNode)) : null, "g", new Object[] { "g", "1" }, new Object[] { "v", unsupportedRaidDownloadV2 ? "2" : "1" }, new Object[] { "ssl", useSSL() }, new Object[] { isPublic(link) ? "p" : "n", fileID });
                final String downloadURL;
                String previewURL = null;
                if (response != null) {
                    final Object g = response.get("g");
                    if (g instanceof List) {
                        // https://github.com/meganz/sdk/blob/7c2eedb755f560098c360edde7b40aa2cb57c12e/src/commands.cpp#L619
                        // now that we are requesting v2, the reply will be an array of 6 URLs for a raid download, or a single URL for the
                        // original
                        // direct download
                        // TODO: file is split in 6 parts -> unsupported
                        final List<Object> gList = (List<Object>) g;
                        if (gList.size() == 1) {
                            downloadURL = valueOf(gList.get(0));
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unsupported raid download");
                        }
                    } else if (g instanceof String) {
                        downloadURL = valueOf(g);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
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
                    } else if ("-4".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "You have exceeded your command weight per time quota. Retry again later", 5 * 60 * 1000l);
                    } else if ("-6".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Retry again later", 5 * 60 * 1000l);
                    } else if ("-11".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Access violation", 5 * 60 * 1000l);
                    } else if ("-16".equals(error)) {
                        previewURL = getPreviewURL(link);
                        if (previewURL == null) {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "User blocked");
                        }
                    } else if ("-17".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Request over quota", 60 * 60 * 1000l);
                    } else if ("-18".equals(error)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Resource temporarily not available, please try again later", 5 * 60 * 1000l);
                    } else {
                        logger.info("Unhandled error code: " + error);
                        checkServerBusy(br.getHttpConnection(), null);
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unhandled error code: " + error);
                    }
                }
                if (PluginJsonConfig.get(MegaConzConfig.class).isHideApplication()) {
                    br.setRequest(null);
                    br.getHeaders().put(new HTTPHeader("User-Agent", null, false));
                    br.getHeaders().put(new HTTPHeader("Accept", null, false));
                    br.getHeaders().put(new HTTPHeader("Accept-Language", null, false));
                    br.getHeaders().put(new HTTPHeader("Accept-Encoding", null, false));
                    br.getHeaders().put(new HTTPHeader("Cache-Control", null, false));
                }
                /* MEGA does not like much connections! */
                if (downloadURL != null) {
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadURL, resume, -10);
                } else if (previewURL != null) {
                    final PostRequest request = br.createPostRequest(previewURL + "/1", (UrlQuery) null, null);
                    request.setContentType(null);
                    final String fa = link.getStringProperty("fa");
                    final String preview = new Regex(fa, "\\d+:1\\*([\\w-]+)").getMatch(0);
                    if (preview != null) {
                        final byte[] data = Base64.decodeFast(preview.replace("-", "+").replace("_", "/"));
                        request.setPostBytes(data);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    link.setVerifiedFileSize(-1);
                    dl.setAllowFilenameFromURL(false);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, request, false, 1);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (dl.getConnection().getResponseCode() == 503) {
                    br.followConnection(true);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many connections", 5 * 60 * 1000l);
                }
                if (dl.getConnection().getResponseCode() == 509 || StringUtils.containsIgnoreCase(dl.getConnection().getResponseMessage(), "Bandwidth Limit Exceeded")) {
                    br.followConnection(true);
                    final String timeLeftString = dl.getConnection().getHeaderField("X-MEGA-Time-Left");
                    final long minTimeLeft = 30 * 60 * 1000l;
                    final long timeLeft;
                    if (timeLeftString != null && timeLeftString.matches("^\\d+$")) {
                        timeLeft = Long.parseLong(timeLeftString) * 1000l;
                    } else {
                        /* Fallback */
                        timeLeft = minTimeLeft;
                    }
                    this.fileOrIPDownloadlimitReached(account, "509 Bandwidth Limit Exceeded", Math.max(minTimeLeft, timeLeft));
                }
                if (StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "html")) {
                    br.followConnection(true);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (dl.startDownload()) {
                    if (link.getLinkStatus().hasStatus(LinkStatus.FINISHED) && link.getDownloadCurrent() > 0) {
                        decrypt(path, encryptionDone, successfulFlag, link, keyString);
                    }
                }
            } catch (IOException e) {
                if (dl != null) {
                    checkServerBusy(dl.getConnection(), e);
                }
                throw e;
            } finally {
                if (!successfulFlag.get() && link.getDownloadCurrent() > 0) {
                    link.setProperty(USED_PLUGIN, getHost());
                }
            }
        } finally {
            free(link, reservation);
        }
    }

    /**
     * MEGA limits can be tricky: They can sit on specific files, on IP ("global limit") or also quota based (also global) e.g. 5GB per day
     * per IP or per Free-Account. For these reasons the user can define the max wait time. The wait time given by MEGA must not be true.
     * </br> 2021-01-21 TODO: Use this for ALL limit based errors
     */
    private void fileOrIPDownloadlimitReached(final Account account, final String msg, final long waitMilliseconds) throws PluginException {
        final long userDefinedMaxWaitMilliseconds = PluginJsonConfig.get(MegaConzConfig.class).getMaxWaittimeOnLimitReachedMinutes() * 60 * 1000;
        if (waitMilliseconds == 0) {
            /* Special handling for zero given serverside waittime -> Old stuff - no idea whether this is still relevant */
            /** 2021-01-21: TODO: Re-Check this account limit handling */
            // I guess that 0 means not possible even after waiting. For example filesize larger than available/possible quota
            if (account != null && Account.AccountType.PREMIUM.equals(account.getType())) {
                throw new AccountUnavailableException("Bandwidth Limit Exceeded", 24 * 60 * 60 * 1000l);
            } else {
                throw new AccountRequiredException("File larger than available quota");
            }
        } else if (account != null) {
            throw new AccountUnavailableException("Bandwidth Limit Exceeded", Math.min(userDefinedMaxWaitMilliseconds, waitMilliseconds));
        } else {
            final LimitMode mode = PluginJsonConfig.get(MegaConzConfig.class).getLimitMode();
            if (mode == LimitMode.GLOBAL_RECONNECT) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, msg, Math.min(userDefinedMaxWaitMilliseconds, waitMilliseconds));
            } else if (mode == LimitMode.GLOBAL_WAIT) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, msg, Math.min(userDefinedMaxWaitMilliseconds, waitMilliseconds));
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, Math.min(userDefinedMaxWaitMilliseconds, waitMilliseconds));
            }
        }
    }

    public class MegaDownloadLinkDownloadable extends DownloadLinkDownloadable {
        final private Browser br;

        public MegaDownloadLinkDownloadable(final DownloadLink link, final Browser br) {
            super(link);
            this.br = br;
        }

        @Override
        public Browser getContextBrowser() {
            return br.cloneBrowser();
        }

        @Override
        public boolean isHashCheckEnabled() {
            return false;
        }
    }

    @Override
    public Downloadable newDownloadable(final DownloadLink downloadLink, final Browser br) {
        return new MegaDownloadLinkDownloadable(downloadLink, br);
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
    public void checkServerBusy(URLConnectionAdapter con, IOException e) throws PluginException {
        if (con != null && con.getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is Busy", 10 * 60 * 1000l, e);
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
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
        byte[] unPadded = b64decode(input);
        int len = 16 - ((unPadded.length - 1) & 15) - 1;
        byte[] payLoadBytes = new byte[unPadded.length + len];
        System.arraycopy(unPadded, 0, payLoadBytes, 0, unPadded.length);
        payLoadBytes = cipher.doFinal(payLoadBytes);
        String ret = new String(payLoadBytes, "UTF-8");
        ret = new Regex(ret, "MEGA(\\{.+\\})").getMatch(0);
        if (ret == null) {
            /* verify if the keyString is correct */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            return restoreFromString(ret, TypeRef.MAP);
        }
    }

    public String getError(final Browser br) {
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

    private volatile DownloadLink decryptingDownloadLink = null;

    private void decrypt(final String path, AtomicLong encryptionDone, AtomicBoolean successFulFlag, final DownloadLink link, String keyString) throws Exception {
        final ShutdownVetoListener vetoListener = new ShutdownVetoListener() {
            @Override
            public void onShutdownVetoRequest(ShutdownRequest request) throws ShutdownVetoException {
                throw new ShutdownVetoException(getHost() + " decryption in progress:" + link.getName(), this);
            }

            @Override
            public void onShutdownVeto(ShutdownRequest request) {
            }

            @Override
            public void onShutdown(ShutdownRequest request) {
            }

            @Override
            public long getShutdownVetoPriority() {
                return 0;
            }
        };
        ShutdownController.getInstance().addShutdownVetoListener(vetoListener);
        decryptingDownloadLink = link;
        try {
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
                if (PluginJsonConfig.get(MegaConzConfig.class).isUseTmpDecryptingFile()) {
                    tmp = new File(path2 + ".decrypted");
                }
                dst = new File(path2);
            } else {
                src = new File(path);
                tmp = new File(path + ".decrypted");
                dst = new File(path);
            }
            if (tmp != null && tmp.exists() && tmp.delete() == false) {
                throw new IOException("Could not delete temp:" + tmp);
            }
            if (dst.exists() && dst.delete() == false) {
                throw new IOException("Could not delete dest:" + dst);
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
                    synchronized (getDecryptionLock(link)) {
                        message.set("Decrypting");
                        fis = new FileInputStream(src);
                        try {
                            FileStateManager.getInstance().requestFileState(outputFile, FILESTATE.WRITE_EXCLUSIVE, this);
                            fos = new FileOutputStream(outputFile);
                            final InputStream is;
                            final OutputStream os;
                            if (false) {
                                is = new DigestInputStream(fis, MessageDigest.getInstance("SHA-256"));
                                os = new DigestOutputStream(fos, MessageDigest.getInstance("SHA-256"));
                            } else {
                                is = fis;
                                os = fos;
                            }
                            try {
                                final Cipher cipher = Cipher.getInstance("AES/CTR/nopadding");
                                cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
                                final CipherOutputStream cos = new CipherOutputStream(new BufferedOutputStream(os, 4096 * 1024), cipher);
                                int read = 0;
                                final byte[] buffer = new byte[2048 * 1024];
                                while ((read = is.read(buffer)) != -1) {
                                    if (read > 0) {
                                        progress.updateValues(progress.getCurrent() + read, total);
                                        cos.write(buffer, 0, read);
                                        encryptionDone.addAndGet(-read);
                                    }
                                }
                                cos.close();
                                if (is instanceof DigestInputStream) {
                                    logger.info("Decryption-Input-SHA256:" + HexFormatter.byteArrayToHex(((DigestInputStream) is).getMessageDigest().digest()));
                                    logger.info("Decryption-Output-SHA256:" + HexFormatter.byteArrayToHex(((DigestOutputStream) os).getMessageDigest().digest()));
                                }
                            } finally {
                                fos.close();
                            }
                        } finally {
                            fis.close();
                        }
                        if (tmp == null) {
                            src.delete();
                        } else {
                            if (tmp.renameTo(dst)) {
                                src.delete();
                            } else {
                                throw new IOException("Could not rename:" + tmp + "->" + dst);
                            }
                        }
                        deleteDst = false;
                        link.getLinkStatus().setStatusText("Finished");
                        link.removeProperty(USED_PLUGIN);
                        successFulFlag.set(true);
                        try {
                            link.setInternalTmpFilenameAppend(null);
                            link.setInternalTmpFilename(null);
                        } catch (final Throwable e) {
                        }
                        new MegaHashCheck(link, dst).finalHashResult();
                    }
                } finally {
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
        } finally {
            ShutdownController.getInstance().removeShutdownVetoListener(vetoListener);
            decryptingDownloadLink = null;
        }
    }

    private static Object GLOBAL_DECRYPTION_LOCK = new Object();

    private Object getDecryptionLock(final DownloadLink link) {
        if (PluginJsonConfig.get(MegaConzConfig.class).isAllowConcurrentDecryption()) {
            return new AtomicReference<DownloadLink>(link);
        } else {
            return GLOBAL_DECRYPTION_LOCK;
        }
    }

    @Override
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        if (decryptingDownloadLink != null && link == decryptingDownloadLink) {
            return false;
        } else {
            return super.isSpeedLimited(link, account);
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

    private static String getPublicFileID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "#(!|%21)([a-zA-Z0-9]+)(!|%21)").getMatch(1);
    }

    private static String getPublicFileKey(DownloadLink link) {
        String ret = new Regex(link.getPluginPatternMatcher(), "#(!|%21)[a-zA-Z0-9]+(!|%21)([a-zA-Z0-9_,\\-%]+)").getMatch(2);
        if (ret != null && ret.contains("%20")) {
            ret = ret.replace("%20", "");
        }
        if (ret != null && ret.length() >= 43) {
            // fileKey is 43 base64 chars
            return ret.substring(0, 43);
        } else {
            return null;
        }
    }

    private static String getNodeFileID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "#!?N(!|%21|\\?)([a-zA-Z0-9]+)(!|%21)").getMatch(1);
    }

    private static String getParentNodeID(final DownloadLink link) {
        final String ret = new Regex(link.getPluginPatternMatcher(), "(=###n=|!|%21)([a-zA-Z0-9]+)$").getMatch(1);
        final String publicFileKey = getPublicFileKey(link);
        final String nodeFileKey = getNodeFileKey(link);
        if (ret != null && !StringUtils.startsWithCaseInsensitive(ret, publicFileKey) && !StringUtils.startsWithCaseInsensitive(ret, nodeFileKey)) {
            return ret;
        } else {
            return link.getStringProperty("pn", null);
        }
    }

    private static String getNodeFileKey(DownloadLink link) {
        String ret = new Regex(link.getDownloadURL(), "#!?N(!|%21|\\?)[a-zA-Z0-9]+(!|%21)([a-zA-Z0-9_,\\-%]+)").getMatch(2);
        if (ret != null && ret.contains("%20")) {
            ret = ret.replace("%20", "");
        }
        return ret;
    }

    public static byte[] b64decode(String data) {
        data = data.replace(",", "");
        data += "==".substring((2 - data.length() * 3) & 3);
        data = data.replace("-", "+").replace("_", "/");
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
        if (PluginJsonConfig.get(MegaConzConfig.class).isUseSSL()) {
            if (PluginJsonConfig.get(MegaConzConfig.class).isHideApplication()) {
                return "2";// can also be 2, see meganz/webclient/blob/master/js/crypto.js
            } else {
                return "1";
            }
        } else {
            return "0";
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
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.FAVICON };
    }

    private boolean isMULTIHOST(PluginForHost plugin) {
        return plugin != null && plugin.hasFeature(LazyPlugin.FEATURE.MULTIHOST);
    }

    @Override
    public String buildExternalDownloadURL(DownloadLink downloadLink, PluginForHost buildForThisPlugin) {
        if (isPublic(downloadLink)) {
            return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
        } else {
            if (StringUtils.equals(getHost(), buildForThisPlugin.getHost())) {
                return super.buildExternalDownloadURL(downloadLink, buildForThisPlugin);
            } else if (isMULTIHOST(buildForThisPlugin)) {
                String fileID = getPublicFileID(downloadLink);
                String keyString = getPublicFileKey(downloadLink);
                if (fileID == null) {
                    fileID = getNodeFileID(downloadLink);
                    keyString = getNodeFileKey(downloadLink);
                }
                final String parentNodeID = getParentNodeID(downloadLink);
                if (StringUtils.equals("linksnappy.com", buildForThisPlugin.getHost())) {
                    // legacy special internal URL format
                    /* 2024-07-10: This format is still mandatory! */
                    return "https://" + jd.plugins.hoster.MegaConz.MAIN_DOMAIN + "/#N!" + fileID + "!" + keyString + "!" + parentNodeID;
                } else if (StringUtils.equals("alldebrid.com", buildForThisPlugin.getHost())) {
                    // legacy special internal URL format
                    /* 2024-07-10: This format is still mandatory! */
                    return "https://" + jd.plugins.hoster.MegaConz.MAIN_DOMAIN + "/#!" + fileID + "!" + keyString + "=~~" + parentNodeID;
                } else {
                    return buildFileLink(downloadLink);
                }
            } else {
                return null;
            }
        }
    }

    /* 2024-07-10: This returns the URL for "single public file" or "file as part of a folder". */
    public static String buildFileLink(DownloadLink downloadLink) {
        if (isPublic(downloadLink)) {
            return downloadLink.getPluginPatternMatcher();
        } else {
            String fileID = getPublicFileID(downloadLink);
            if (fileID == null) {
                fileID = getNodeFileID(downloadLink);
            }
            final String parentNodeID = getParentNodeID(downloadLink);
            final String folderMasterKey = getFolderMasterKey(downloadLink);
            if (parentNodeID != null && folderMasterKey != null) {
                return "https://" + jd.plugins.hoster.MegaConz.MAIN_DOMAIN + "/folder/" + parentNodeID + "#" + folderMasterKey + "/file/" + fileID;
            }
        }
        return null;
    }

    @Override
    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        if (!PluginJsonConfig.get(MegaConzConfig.class).isAllowMultihostUsage() && !plugin.getHost().equals(this.getHost())) {
            /* Disabled by user */
            return false;
        } else if (link != null) {
            if (plugin != null) {
                if (!StringUtils.equals(link.getStringProperty(USED_PLUGIN, plugin.getHost()), plugin.getHost())) {
                    return false;
                }
            }
            /* Only allow download if an URL can be built for use with given PluginForHost. */
            if (buildExternalDownloadURL(link, plugin) != null) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private static String getFolderMasterKey(final DownloadLink link) {
        final String ret = jd.plugins.decrypter.MegaConz.getFolderMasterKey(link.getContainerUrl());
        if (ret != null) {
            return ret;
        } else {
            return link.getStringProperty("fmk", null);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            link.removeProperty(USED_PLUGIN);
        }
    }

    @Override
    public Class<? extends MegaConzConfig> getConfigInterface() {
        return MegaConzConfig.class;
    }
}
