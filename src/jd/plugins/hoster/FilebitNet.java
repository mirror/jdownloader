//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.formatter.TimeFormatter;
import org.bouncycastle.util.Arrays;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.components.FilebitNetAccountFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FilebitNet extends PluginForHost {
    public FilebitNet(PluginWrapper wrapper) {
        super(wrapper);
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            this.enablePremium("https://filebit.net/plans");
        }
    }

    @Override
    public String getAGBLink() {
        return "https://filebit.net/tos";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "filebit.net" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/f/([A-Za-z0-9]{7,})(\\?i=[^#]+)?#([A-Za-z0-9_\\-]{44})");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getAPIBase() {
        if (true) {
            return "https://clientapi.filebit.net";
        } else {
            return "https://filebit.net";
        }
    }

    private Browser prepBrowser(final Browser br) {
        br.getHeaders().put(HTTPConstants.HEADER_REQUEST_USER_AGENT, "jdownloader-" + getVersion());
        return br;
    }

    private String getKeyType(final Browser br, final String unknownKey) throws Exception {
        final Map<String, Object> response = callApi(br, "/app/keytype.json", new Object[][] { { "key", unknownKey } });
        final String keytype = response != null ? (String) response.get("keytype") : null;
        if (keytype == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if ("unknown".equals(keytype)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "unknown keytype");
        } else if ("st".equals(keytype)) {
            // st = "speedticket"
            return keytype;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "unsupported keytype:" + keytype);
        }
    }

    private String addSpeedKey(final Browser br, final String speedKey) throws Exception {
        final Browser brc = br.cloneBrowser();
        prepBrowser(brc);
        final Map<String, Object> payLoad = new HashMap<String, Object>();
        payLoad.put("key", speedKey);
        payLoad.put("skc", true);
        payLoad.put("skc", 1);
        PostRequest request = brc.createJSonPostRequest(getAPIBase() + "/app/addspeedkey.json", payLoad);
        request = brc.createJSonPostRequest(getAPIBase() + "/app/licence/add.json", payLoad);
        final String responseRaw = brc.getPage(request);
        final Map<String, Object> response = restoreFromString(responseRaw, TypeRef.MAP);
        final String key = response != null ? (String) response.get("key") : null;
        if (key != null) {
            return key;
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private Map<String, Object> toPayLoad(Object[][] params) {
        final Map<String, Object> ret = new HashMap<String, Object>();
        for (final Object[] param : params) {
            ret.put(StringUtils.valueOfOrNull(param[0]), param[1]);
        }
        return ret;
    }

    private Request doAPIRequest(final Browser br, final String apiRequest, Object[][] params) throws Exception {
        return doAPIRequest(br, apiRequest, toPayLoad(params));
    }

    private Request doAPIRequest(final Browser br, final String apiRequest, Map<String, Object> payLoad) throws Exception {
        final Browser brc = br.cloneBrowser();
        prepBrowser(brc);
        final PostRequest request = brc.createJSonPostRequest(getAPIBase() + apiRequest, payLoad);
        br.getPage(request);
        return request;
    }

    private Map<String, Object> callApi(final Browser br, final String apiRequest, Map<String, Object> payLoad) throws Exception {
        return restoreFromString(doAPIRequest(br, apiRequest, payLoad).getHtmlCode(), TypeRef.MAP);
    }

    private Map<String, Object> callApi(final Browser br, final String apiRequest, final Object[][] params) throws Exception {
        return callApi(br, apiRequest, toPayLoad(params));
    }

    private Map<String, Object> checkSpeedKey(final Browser br, final String speedKey) throws Exception {
        final Map<String, Object> response = callApi(br, "/app/checkspeedkey.json", new Object[][] { { "key", speedKey }, { "skc", true } });
        final Map<String, Object> tickets = response != null ? (Map<String, Object>) response.get("tickets") : null;
        final Map<String, Object> ticket = tickets != null ? (Map<String, Object>) tickets.get(speedKey) : null;
        if (ticket != null) {
            return ticket;
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private final String PROPERTY_KEYTYPE = "PROPERTY_KEYTYPE";
    private final String PROPERTY_KEY     = "PROPERTY_KEY";

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /*
             * Check username and password field for entered "license key". This improves usability for myjdownloader/headless users as we
             * can't display custom login masks for them yet.
             */
            final String userCorrected = FilebitNetAccountFactory.correctLicenseKey(account.getUser());
            final String pwCorrected = FilebitNetAccountFactory.correctLicenseKey(account.getPass());
            String licenseKey = null;
            if (FilebitNetAccountFactory.isLicenseKey(userCorrected)) {
                licenseKey = userCorrected;
            } else if (FilebitNetAccountFactory.isLicenseKey(pwCorrected)) {
                licenseKey = pwCorrected;
            }
            if (licenseKey == null) {
                throw new AccountInvalidException("Invalid license key format");
            }
            account.setUser(licenseKey);
            final boolean useNewHandling = true;
            if (useNewHandling) {
                return loginAndGetAccountInfo(br, account);
            } else {
                login(br, account);
            }
        }
        return super.fetchAccountInfo(account);
    }

    @Deprecated
    public void login(final Browser br, final Account account) throws Exception {
        synchronized (account) {
            String keyType = account.getStringProperty(PROPERTY_KEYTYPE);
            String key = account.getStringProperty(PROPERTY_KEY);
            if (keyType == null || key == null) {
                final String userKey = account.getUser();
                keyType = getKeyType(br, userKey);
                key = addSpeedKey(br, userKey);
                account.setProperty(PROPERTY_KEYTYPE, keyType);
                account.setProperty(PROPERTY_KEY, key);
            }
            if ("st".equals(keyType)) {
                final Map<String, Object> ticket = checkSpeedKey(br, key);
                // This is missing errorhandling e.g. {"servertime":"2022-12-09 11:38:43","tickets":{"<ticketID>":{"error":"ticket not found
                // or
                // expired"}}}
                final AccountInfo ai = new AccountInfo();
                ai.setTrafficLeft(((Number) ticket.get("traffic")).longValue());
                final String validUntilDate = ticket.get("validuntil").toString();
                ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntilDate, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH), br);
                // return ai;
            }
        }
    }

    private AccountInfo loginAndGetAccountInfo(final Browser br, final Account account) throws Exception {
        synchronized (account) {
            int attempt = 0;
            Map<String, Object> lastTicket = null;
            do {
                attempt++;
                String keyType = account.getStringProperty(PROPERTY_KEYTYPE);
                String key = account.getStringProperty(PROPERTY_KEY);
                /* Key of most important cookie: "y_bid" */
                Cookies storedCookies = account.loadCookies("");
                if (keyType == null || key == null || storedCookies == null) {
                    checkLicenseKey(br, account);
                    keyType = account.getStringProperty(PROPERTY_KEYTYPE);
                    key = account.getStringProperty(PROPERTY_KEY);
                    storedCookies = null;
                }
                if ("st".equals(keyType)) {
                    if (storedCookies != null) {
                        br.setCookies(storedCookies);
                    }
                    lastTicket = checkSpeedKey(br, key);
                    final String ticketError = (String) lastTicket.get("error");
                    // E.g. {"servertime":"2022-12-09 11:38:43","tickets":{"<ticketID>":{"error":"ticket not found
                    // or
                    // expired"}}}
                    if (ticketError != null) {
                        logger.info("Ticket/Session expired? Error: " + ticketError);
                        account.removeProperty(PROPERTY_KEYTYPE);
                        account.removeProperty(PROPERTY_KEY);
                        account.clearCookies("");
                        continue;
                    }
                    final AccountInfo ai = new AccountInfo();
                    ai.setTrafficLeft(((Number) lastTicket.get("traffic")).longValue());
                    final String validUntilDate = lastTicket.get("validuntil").toString();
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntilDate, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH), br);
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return ai;
                }
                break;
            } while (attempt <= 1);
            this.checkErrors(null, account, lastTicket);
        }
        return null;
    }

    private void checkLicenseKey(final Browser br, final Account account) throws Exception {
        final String userKey = account.getUser();
        final String keyType = getKeyType(br, userKey);
        final String key = addSpeedKey(br, userKey);
        account.setProperty(PROPERTY_KEYTYPE, keyType);
        account.setProperty(PROPERTY_KEY, key);
    }

    private void checkErrors(final DownloadLink link, final Account account, final Map<String, Object> map) throws PluginException {
        /* TODO: Add support for more/specific errormessages */
        if (map == null) {
            return;
        }
        final String errorMsg = (String) map.get("error");
        if (errorMsg != null) {
            if (link == null) {
                /* Error during login */
                throw new AccountInvalidException(errorMsg);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorMsg);
            }
        }
    }

    public Map<String, Object> requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        final String fid = getFID(link);
        if (!link.isNameSet()) {
            /* Fallback (weak filename) */
            link.setName(fid);
        }
        /* Do not set this to null, API doesn't like that! */
        String speedTicket = "";
        if (account != null) {
            speedTicket = account.getStringProperty(PROPERTY_KEY, "");
        }
        final Request request = doAPIRequest(br, "/storage/bucket/info.json", new Object[][] { { "file", fid }, { "st", speedTicket } });
        if (request.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> bucketResponse = restoreFromString(request.getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> hash = (Map<String, Object>) bucketResponse.get("hash");
        if (hash != null) {
            final String hashType = hash.get("type").toString();
            if (hashType.equalsIgnoreCase("sha256")) {
                link.setSha256Hash(hash.get("value").toString());
            }
        }
        String filename = bucketResponse.get("filename").toString();
        if (filename != null) {
            filename = decryptString(link, filename);
            link.setFinalFileName(filename);
        }
        final Number filesize = (Number) bucketResponse.get("filesize");
        if (filesize != null) {
            link.setDownloadSize(filesize.longValue());
        }
        final String error = (String) bucketResponse.get("error");
        if (error != null) {
            /* E.g. {"error":"invalid file"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String state = StringUtils.valueOfOrNull(bucketResponse.get("state"));
        if ("ONLINE".equals(state)) {
            return bucketResponse;
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "state:" + state);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (requestFileInformation(link, null) != null) {
            return AvailableStatus.TRUE;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private byte[] decrypt(byte[][] keyMaterial, byte[] encryptedData) throws Exception {
        final SecretKey aesKey = new SecretKeySpec(keyMaterial[0], "AES");
        final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(keyMaterial[1]));
        byte[] decryptedData = cipher.doFinal(encryptedData);
        return unpad(decryptedData);
    }

    private String decryptString(final DownloadLink link, String encryptedString) throws Exception {
        final byte[] encryptedBytes = Base64.decodeFast(encryptedString.replace("-", "+").replace("_", "/"));
        final byte[] decryptedBytes = decrypt(getFileKey(link), encryptedBytes);
        final String ret = new String(decryptedBytes, "UTF-8");
        return ret;
    }

    private byte[] unpad(byte[] data) throws PluginException {
        if (data == null || data.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (data.length % 16 > 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return Arrays.copyOfRange(data, 0, data.length - data[data.length - 1]);
        }
    }

    public byte[][] unmerge_key_iv(final String filebit_key) throws PluginException {
        final byte[] mergedKey = Base64.decodeFast(filebit_key.replace("-", "+").replace("_", "/"));
        if (mergedKey == null || mergedKey.length != 33) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "unknown key:" + filebit_key);
        } else {
            final byte version = mergedKey[0];
            if (version != 1) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "unknown version:" + version);
            }
            final byte key[] = new byte[16];
            final byte iv[] = new byte[16];
            for (int i = 0; i < 16; i++) {
                iv[i] = mergedKey[1 + (2 * i)];
                key[i] = mergedKey[2 + (2 * i)];
            }
            return new byte[][] { key, iv };
        }
    }

    private void verifyLink(final DownloadLink link) throws PluginException {
        if (getFID(link) == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            getFileKey(link);
        }
    }

    private byte[][] getFileKey(final DownloadLink link) throws PluginException {
        final String mergedKey = new Regex(link.getPluginPatternMatcher(), "#([a-zA-Z0-9\\-_]{44})").getMatch(0);
        if (mergedKey == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return unmerge_key_iv(mergedKey);
        }
    }

    /** See https://filebit.net/docs/#/?id=multi-file-informations */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            br.setCookiesExclusive(true);
            int index = 0;
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            while (true) {
                while (true) {
                    /* 2022-12-08: Tested with 100 items max. */
                    if (index == urls.length || links.size() == 100) {
                        break;
                    } else {
                        final DownloadLink link = urls[index++];
                        try {
                            verifyLink(link);
                            links.add(link);
                        } catch (Exception e) {
                            logger.log(e);
                            link.setAvailable(false);
                        }
                    }
                }
                final List<String> fileIDs = new ArrayList<String>();
                for (final DownloadLink link : links) {
                    fileIDs.add(this.getFID(link));
                }
                final Map<String, Object> response = callApi(br, "/storage/multiinfo.json", new Object[][] { { "files", fileIDs }, { "hash", 1 } });
                for (final DownloadLink link : links) {
                    final String fid = this.getFID(link);
                    if (!link.isNameSet()) {
                        link.setName(fid);
                    }
                    final Map<String, Object> info = (Map<String, Object>) response.get(fid);
                    if (info == null) {
                        /* This should never happen! */
                        logger.info("info missing for:" + fid);
                        link.setAvailable(false);
                        continue;
                    } else {
                        final String sha256 = StringUtils.valueOfOrNull(info.get("sha256"));
                        if (sha256 != null) {
                            link.setSha256Hash(sha256);
                        }
                        final Number filesize = (Number) info.get("size");
                        if (filesize != null) {
                            link.setVerifiedFileSize(filesize.longValue());
                        }
                        final String state = StringUtils.valueOfOrNull(info.get("state"));
                        if ("ONLINE".equals(state)) {
                            link.setAvailable(true);
                        } else {
                            /* E.g. {"state":"ERROR"} */
                            link.setAvailable(false);
                        }
                        String name = (String) info.get("name");
                        if (!StringUtils.isEmpty(name)) {
                            name = decryptString(link, name);
                            link.setFinalFileName(name);
                        }
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            logger.log(e);
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> entries = this.requestFileInformation(link, account);
        this.checkErrors(link, account, entries);
        final Map<String, Object> slot = (Map<String, Object>) entries.get("slot");
        /*
         * TODO: Update/improve errorhandling for this as such errors may result in free download although premium is expected to be used:
         * "st":{"ticket":"","enabled":false,"state":"invalid","message":"your entered ticket is invalid or expired","trafficAvailable":0,
         * "directDownload":null}
         */
        final Map<String, Object> st = (Map<String, Object>) entries.get("st");
        final boolean speedTicketEnabled = ((Boolean) st.get("enabled")).booleanValue();
        if (account != null && !speedTicketEnabled) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Speedticket given but not usable, API error: " + st.get("message"));
        }
        if (account != null) {
            /* TODO: Refresh trafficleft value of account each time a download is attempted. */
            final Number trafficAvailable = (Number) st.get("trafficAvailable");
            // TODO: What is this?
            // final Object directDownload = st.get("directDownload");
        }
        final int waitSeconds = ((Number) slot.get("wait")).intValue();
        final String ticketID = slot.get("ticket").toString();
        if (waitSeconds > 0) {
            /* Usually only needed for downloads without account */
            this.sleep(waitSeconds * 1000l, link);
        }
        doAPIRequest(br, "/file/slot.json", new Object[][] { { "slot", ticketID } });
        /*
         * Example response:
         * {"success":true,"slot":"CENSORED","config":{"mode":"PRO","parallel":9999,"chunks":9999,"fast":{"available":false,"bytes":0}}}
         */
        final Map<String, Object> slotData = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        this.checkErrors(link, account, slotData);
        if (!(slotData.get("success").equals(Boolean.TRUE))) {
            /* This should never happen(?) */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Slot has not been confirmed");
        }
        final Map<String, Object> config = (Map<String, Object>) slotData.get("config");
        final Map<String, Object> fast = (Map<String, Object>) config.get("fast");
        doAPIRequest(br, "/storage/bucket/contents.json", new Object[][] { { "id", this.getFID(link) } });
        final Map<String, Object> chunk_info = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        this.checkErrors(link, account, chunk_info);
        /* TODO: Add download functionality */
        final List<List<Object>> chunks = (List<List<Object>>) chunk_info.get("chunks");
        int index = 0;
        // TODO: See https://github.com/filebitnet/filebit-python/blob/72cd530dba4dfb9d6d1dbad642879e4b464bafdb/filebit/download.py#L162
        for (final List<Object> chunk : chunks) {
            final int chunk_id = ((Number) chunk.get(0)).intValue();
            final long offset0 = ((Number) chunk.get(1)).longValue();
            // final long undefined = ((Number) chunk.get(2)).longValue();
            final long length = ((Number) chunk.get(3)).longValue();
            final long crc32 = ((Number) chunk.get(4)).longValue();
            final String downloadid = chunk.get(5).toString();
            final String chunkURL = getAPIBase() + "/download/" + downloadid + "?slot=" + ticketID;
            System.out.println("chunk[" + index + "] | " + chunk);
            System.out.println(chunkURL);
            index++;
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new FilebitNetAccountFactory(callback);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}