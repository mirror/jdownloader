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

import java.awt.Color;
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
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.formatter.TimeFormatter;
import org.bouncycastle.util.Arrays;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;

import jd.PluginWrapper;
import jd.http.Browser;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/f/([A-Za-z0-9]+)(\\?i=[^#]+)?#([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final boolean      FREE_RESUME       = true;
    private final int          FREE_MAXCHUNKS    = 0;
    private final int          FREE_MAXDOWNLOADS = -1;
    public static final String API_BASE          = "https://filebit.net";

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

    private String getKey(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
    }

    public String getAPI() {
        if (true) {
            return "https://clientapi.filebit.net";
        } else {
            return "https://" + getHost();
        }
    }

    private Browser prepBrowser(final Browser br) {
        br.getHeaders().put(HTTPConstants.HEADER_REQUEST_USER_AGENT, "jdownloader-" + getVersion());
        return br;
    }

    public String getKeyType(final Browser br, final String unknownKey) throws Exception {
        final Browser brc = br.cloneBrowser();
        prepBrowser(brc);
        final Map<String, Object> payLoad = new HashMap<String, Object>();
        payLoad.put("key", unknownKey);
        final PostRequest request = brc.createJSonPostRequest(getAPI() + "/app/keytype.json", payLoad);
        final String responseRaw = brc.getPage(request);
        final Map<String, Object> response = restoreFromString(responseRaw, TypeRef.MAP);
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

    public String addSpeedKey(final Browser br, final String speedKey) throws Exception {
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

    Map<String, Object> checkSpeedKey(final Browser br, final String speedKey) throws Exception {
        final Browser brc = br.cloneBrowser();
        prepBrowser(br);
        final Map<String, Object> payLoad = new HashMap<String, Object>();
        payLoad.put("key", speedKey);
        payLoad.put("skc", true);
        final PostRequest request = brc.createJSonPostRequest(getAPI() + "/app/checkspeedkey.json", payLoad);
        final String responseRaw = brc.getPage(request);
        final Map<String, Object> response = restoreFromString(responseRaw, TypeRef.MAP);
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
            final String userCorrected = correctLicenseKey(account.getUser());
            final String pwCorrected = correctLicenseKey(account.getPass());
            String licenseKey = null;
            if (isLicenseKey(userCorrected)) {
                licenseKey = userCorrected;
            } else if (isLicenseKey(pwCorrected)) {
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        prepBrowser(br);
        br.postPageRaw("https://" + this.getHost() + "/storage/bucket/info.json", "{\"file\":\"" + this.getFID(link) + "\"}");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        final String error = (String) entries.get("error");
        if (error != null) {
            /* E.g. {"error":"invalid file"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> hash = (Map<String, Object>) entries.get("hash");
        if (hash != null) {
            final String hashType = hash.get("type").toString();
            if (hashType.equalsIgnoreCase("sha256")) {
                link.setSha256Hash(hash.get("value").toString());
            }
        }
        final String filename = entries.get("filename").toString();
        final Number filesize = (Number) entries.get("filesize");
        if (filename != null) {
            // TODO: Decrypt filename
            link.setFinalFileName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(filesize.longValue());
        }
        return AvailableStatus.TRUE;
    }

    public static void main(String[] args) throws Exception {
        final String hash = "TEST";
        final String fileName = "TEST";
        byte key[][] = unmerge_key_iv(hash);
        byte[] name = Base64.decodeFast(fileName.replace("-", "+").replace("_", "/"));
        final SecretKey aesKey = new SecretKeySpec(key[0], "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(key[1]));
        byte[] test = cipher.doFinal(name);
        final String m = new String(unpad(test));
        System.out.println(m);
    }

    public static byte[] unpad(byte[] data) throws PluginException {
        if (data == null || data.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (data.length % 16 > 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return Arrays.copyOfRange(data, 0, data.length - data[data.length - 1]);
        }
    }

    public static byte[][] unmerge_key_iv(final String filebit_key) throws PluginException {
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

    public byte[][] getFileKey(final DownloadLink link) {
        final String mergedKey = new Regex(link.getPluginPatternMatcher(), "#([a-zA-Z0-9\\-_)").getMatch(0);
        return null;
    }

    /** See https://filebit.net/docs/#/?id=multi-file-informations */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            prepBrowser(this.br);
            br.setCookiesExclusive(true);
            final List<String> fileIDs = new ArrayList<String>();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* 2022-12-08: Tested with 100 items max. */
                    if (index == urls.length || links.size() == 100) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                fileIDs.clear();
                for (final DownloadLink link : links) {
                    fileIDs.add(this.getFID(link));
                }
                final Map<String, Object> payLoad = new HashMap<String, Object>();
                payLoad.put("files", fileIDs);
                PostRequest request = br.createJSonPostRequest(getAPI() + "/storage/multiinfo.json", payLoad);
                br.getPage(request);
                final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                for (final DownloadLink link : links) {
                    final String fid = this.getFID(link);
                    if (!link.isNameSet()) {
                        link.setName(fid);
                    }
                    final Map<String, Object> info = (Map<String, Object>) entries.get(fid);
                    if (info == null) {
                        /* This should never happen! */
                        link.setAvailable(false);
                        continue;
                    }
                    // TODO: Decrypt filename
                    final String state = info.get("state").toString();
                    final String filename = (String) info.get("name");
                    final Number filesize = (Number) info.get("size");
                    if (!StringUtils.isEmpty(filename)) {
                        link.setFinalFileName(filename);
                    }
                    if (filesize != null) {
                        link.setVerifiedFileSize(filesize.longValue());
                    }
                    if (state.equalsIgnoreCase("ONLINE")) {
                        link.setAvailable(true);
                    } else {
                        /* E.g. {"state":"ERROR"} */
                        link.setAvailable(false);
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
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
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
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, FREE_RESUME, FREE_MAXCHUNKS);
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

    private static String correctLicenseKey(final String key) {
        if (key == null) {
            return null;
        } else {
            return key.trim().replace(" ", "");
        }
    }

    private static boolean isLicenseKey(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches("[A-Za-z0-9]+")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new FilebitNetAccountFactory(callback);
    }

    public static class FilebitNetAccountFactory extends MigPanel implements AccountBuilderInterface {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private final String      APIKEYHELP       = "Enter your Licence key";
        private final JLabel      apikeyLabel;

        private String getPassword() {
            if (this.pass == null) {
                return null;
            } else {
                return correctLicenseKey(new String(this.pass.getPassword()));
            }
        }

        public boolean updateAccount(Account input, Account output) {
            if (!StringUtils.equals(input.getUser(), output.getUser())) {
                output.setUser(input.getUser());
                return true;
            } else if (!StringUtils.equals(input.getPass(), output.getPass())) {
                output.setPass(input.getPass());
                return true;
            } else {
                return false;
            }
        }

        private final ExtPasswordField pass;

        public FilebitNetAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(new JLabel("Enter license key."));
            add(new JLabel("You can find it in your PDF file."));
            add(apikeyLabel = new JLabel("Licence key:"));
            add(this.pass = new ExtPasswordField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            }, "");
            pass.setHelpText(APIKEYHELP);
        }

        @Override
        public JComponent getComponent() {
            return this;
        }

        @Override
        public void setAccount(Account defaultAccount) {
            if (defaultAccount != null) {
                // name.setText(defaultAccount.getUser());
                pass.setText(defaultAccount.getPass());
            }
        }

        @Override
        public boolean validateInputs() {
            final String pw = getPassword();
            if (isLicenseKey(pw)) {
                apikeyLabel.setForeground(Color.BLACK);
                return true;
            } else {
                apikeyLabel.setForeground(Color.RED);
                return false;
            }
        }

        @Override
        public Account getAccount() {
            return new Account(null, getPassword());
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}