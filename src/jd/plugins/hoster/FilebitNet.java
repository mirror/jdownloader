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

import java.io.IOException;
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

    private String getAPI() {
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
        PostRequest request = brc.createJSonPostRequest(getAPI() + "/app/addspeedkey.json", payLoad);
        request = brc.createJSonPostRequest(getAPI() + "/app/licence/add.json", payLoad);
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
        final PostRequest request = brc.createJSonPostRequest(getAPI() + apiRequest, payLoad);
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
                // TODO: Add errorhandling e.g. {"servertime":"2022-12-09 11:38:43","tickets":{"<ticketID>":{"error":"ticket not found or
                // expired"}}}
                final AccountInfo ai = new AccountInfo();
                ai.setTrafficLeft(((Number) ticket.get("traffic")).longValue());
                final String validUntilDate = ticket.get("validuntil").toString();
                ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntilDate, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH), br);
                // return ai;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /*
             * Check username + password field for entered "license key". This improves usability for myjdownloader/headless users as we
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
            login(br, account);
        }
        return super.fetchAccountInfo(account);
    }

    public Map<String, Object> requestFileInformation(final Account account, final DownloadLink link) throws Exception {
        final String fid = getFID(link);
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(fid);
        }
        final Request request = doAPIRequest(br, "/storage/bucket/info.json", new Object[][] { { "file", fid } });
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
        if (requestFileInformation(null, link) != null) {
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
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (true) {
            /* 2022-10-05: This plugin is unfinished */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!attemptStoredDownloadurlDownload(link, directlinkproperty)) {
            requestFileInformation(link);
            String dllink = br.getRegex("").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                logger.warning("Failed to find final downloadurl");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, true, 1);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            link.removeProperty(directlinkproperty);
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new FilebitNetAccountFactory(callback);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}