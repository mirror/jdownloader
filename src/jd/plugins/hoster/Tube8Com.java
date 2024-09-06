//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.appwork.storage.TypeRef;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class Tube8Com extends PluginForHost {
    /* DEV NOTES */
    /* Porn_plugin */
    private String              dllink = null;
    private static final String mobile = "mobile";

    public Tube8Com(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.tube8.com/signin.html");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "tube8.com", "tube8.es", "tube8.fr" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:(?:www|[a-z]{2})\\.)?" + buildHostsPatternPart(domains) + "/.+/([0-9]+)/?");
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
        return new Regex(link.getPluginPatternMatcher(), "(\\d+)/?$").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        dllink = null;
        this.br.setAllowedResponseCodes(new int[] { 500 });
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = this.getFID(link);
        if (fid == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!link.isNameSet()) {
            link.setName(fid + ".mp4");
        }
        if (account != null) {
            this.login(account, br);
        }
        /* 2020-03-18: Do this to avoid redirectloop */
        br.getPage("https://www." + this.getHost() + "/porn-video/" + fid + "/");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(fid)) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getRequest().getHtmlCode().length() <= 100) {
            /* Empty page */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String verifyAge = br.getRegex("(<div class=\"enter-btn\">)").getMatch(0);
        if (verifyAge != null) {
            br.postPage(link.getDownloadURL(), "processdisclaimer=");
        }
        if (br.containsHTML("class=\"video-removed-div\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.getHttpConnection().getResponseCode() == 500) {
            return AvailableStatus.UNCHECKABLE;
        } else if (br.containsHTML("class=\"geo-blocked-container\"")) {
            if (isDownload) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked");
            } else {
                return AvailableStatus.TRUE;
            }
        }
        String title = br.getRegex("\"video_title\":\"([^\"]+)").getMatch(0);
        if (title == null) {
            title = br.getRegex("<span class=\"item\">(.*?)</span>").getMatch(0);
            if (title == null) {
                title = br.getRegex("<title>([^<]+)<").getMatch(0);
            }
        }
        boolean failed = true;
        boolean preferMobile = getPluginConfig().getBooleanProperty(mobile, false);
        String videoDownloadUrls = "";
        /* streaming link */
        findStreamingLink();
        if (dllink != null && requestVideo(link)) {
            failed = false;
        }
        /* decrease HTTP requests */
        if (failed || preferMobile) {
            videoDownloadUrls = findDownloadurlStandardAndMobile(link);
        }
        /* normal link */
        if (failed) {
            findNormalLink(this.br.toString());
        }
        if (failed && dllink != null && requestVideo(link)) {
            failed = false;
        }
        /* 3gp link */
        if (failed || preferMobile) {
            findMobileLink(videoDownloadUrls);
        }
        if ((failed || preferMobile) && dllink != null && requestVideo(link)) {
            failed = false;
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            /* Remove irrelevant parts */
            title = title.replaceFirst(" Porn Videos - Tube8", "");
            if (dllink.contains(".3gp")) {
                link.setFinalFileName((title + ".3gp"));
            } else if (dllink.contains(".mp4")) {
                link.setFinalFileName((title + ".mp4"));
            } else {
                link.setFinalFileName(title + ".flv");
            }
        }
        if (failed) {
            return AvailableStatus.UNCHECKABLE;
        }
        return AvailableStatus.TRUE;
    }

    private boolean requestVideo(final DownloadLink link) throws Exception {
        URLConnectionAdapter con = null;
        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        try {
            con = br2.openGetConnection(dllink);
            if (looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setDownloadSize(con.getCompleteContentLength());
                }
                return true;
            } else {
                try {
                    br2.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                if (con.getResponseCode() == 401) {
                    link.setProperty("401", 401);
                    return true;
                } else {
                    return false;
                }
            }
        } finally {
            try {
                if (con != null) {
                    con.disconnect();
                }
            } catch (final Throwable e) {
            }
        }
    }

    @SuppressWarnings("deprecation")
    private String findDownloadurlStandardAndMobile(final DownloadLink link) throws Exception {
        final String hash = br.getRegex("videoHash\\s*=\\s*\"([a-z0-9]+)\"").getMatch(0);
        if (hash == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.getPage("https://www." + this.getHost() + "/ajax/getVideoDownloadURL.php?hash=" + hash + "&video=" + new Regex(link.getDownloadURL(), ".*?(\\d+)$").getMatch(0) + "&download_cdn=true&_=" + System.currentTimeMillis());
        String ret = br2.getRegex("^(.*?)$").getMatch(0);
        return ret != null ? ret.replace("\\", "") : "";
    }

    private void findMobileLink(final String correctedBR) throws Exception {
        dllink = new Regex(correctedBR, "\"mobile_url\":\"(https?:.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = new Regex(correctedBR, "\"(https?://cdn\\d+\\.mobile\\.tube8\\.com/.*?)\"").getMatch(0);
        }
    }

    private void findNormalLink(final String correctedBR) throws Exception {
        dllink = new Regex(correctedBR, "\"standard_url\":\"(http.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = new Regex(correctedBR, "\"(https?://cdn\\d+\\.public\\.tube8\\.com/.*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = new Regex(correctedBR, "page_params\\.videoUrlJS = \"(http[^<>\"]*?)\";").getMatch(0);
        }
    }

    private void findStreamingLink() throws Exception {
        String flashVars = br.getRegex("var flashvars\\s*=\\s*(\\{.*?\\});").getMatch(0);
        if (flashVars == null) {
            return;
        }
        final Map<String, Object> entries = restoreFromString(flashVars, TypeRef.MAP);
        final String[] quals = new String[] { "quality_2160p", "quality_1440p", "quality_720p", "quality_480p", "quality_240p", "quality_180p" };
        for (final String qual : quals) {
            final Object qualO = entries.get(qual);
            if (qualO instanceof String) {
                this.dllink = qualO.toString();
            }
        }
        final boolean isEncrypted = ((Boolean) entries.get("encrypted")).booleanValue();
        if (isEncrypted) {
            final String decrypted = (String) entries.get("video_url");
            String key = (String) entries.get("video_title");
            /* Dirty workaround, needed for links with cyrillic titles/filenames. */
            if (key == null) {
                key = "";
            }
            try {
                dllink = new BouncyCastleAESCounterModeDecrypt().decrypt(decrypted, key, 256);
            } catch (Throwable e) {
                /* Fallback for stable version */
                dllink = AESCounterModeDecrypt(decrypted, key, 256);
            }
            if (dllink != null && (dllink.startsWith("Error:") || !dllink.startsWith("http"))) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, dllink);
            }
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.tube8.com/info.html#terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (link.getIntegerProperty("401", -1) == 401) {
                link.removeProperty("401");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        this.login(account, this.br);
        /* only support for free accounts at the moment */
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    private void login(final Account account, final Browser br) throws IOException, PluginException {
        this.setBrowserExclusive();
        boolean follow = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            br.getPage("https://www." + this.getHost());
            final PostRequest postRequest = new PostRequest("https://www.tube8.com/ajax2/login/");
            postRequest.addVariable("username", Encoding.urlEncode(account.getUser()));
            postRequest.addVariable("password", Encoding.urlEncode(account.getPass()));
            postRequest.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            postRequest.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            postRequest.addVariable("rememberme", "NO");
            br.getPage(postRequest);
            if (br.containsHTML("invalid") || br.containsHTML("0\\|")) { // || br.getCookie(getHost(), "ubl") == null) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } finally {
            br.setFollowRedirects(follow);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    /**
     * AES CTR(Counter) Mode for Java ported from AES-CTR-Mode implementation in JavaScript by Chris Veness
     *
     * @see <a href="http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf">
     *      "Recommendation for Block Cipher Modes of Operation - Methods and Techniques"</a>
     */
    private String AESCounterModeDecrypt(String cipherText, String key, int nBits) throws Exception {
        if (!(nBits == 128 || nBits == 192 || nBits == 256)) {
            return "Error: Must be a key mode of either 128, 192, 256 bits";
        }
        if (cipherText == null || key == null) {
            return "Error: cipher and/or key equals null";
        }
        String res = null;
        nBits = nBits / 8;
        byte[] data = Base64.decode(cipherText.toCharArray());
        /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
        byte[] k = Arrays.copyOf(key.getBytes(), nBits);
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        SecretKey secretKey = generateSecretKey(k, nBits);
        byte[] nonceBytes = Arrays.copyOf(Arrays.copyOf(data, 8), nBits / 2);
        IvParameterSpec nonce = new IvParameterSpec(nonceBytes);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, nonce);
        /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
        res = new String(cipher.doFinal(data, 8, data.length - 8));
        return res;
    }

    private SecretKey generateSecretKey(byte[] keyBytes, int nBits) throws Exception {
        try {
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            keyBytes = cipher.doFinal(keyBytes);
        } catch (InvalidKeyException e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unlimited Strength JCE Policy Files needed!", e);
        } catch (Throwable e1) {
            return null;
        }
        System.arraycopy(keyBytes, 0, keyBytes, nBits / 2, nBits / 2);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private class BouncyCastleAESCounterModeDecrypt {
        private String decrypt(String cipherText, String key, int nBits) throws Exception {
            if (!(nBits == 128 || nBits == 192 || nBits == 256)) {
                return "Error: Must be a key mode of either 128, 192, 256 bits";
            }
            if (cipherText == null || key == null) {
                return "Error: cipher and/or key equals null";
            }
            byte[] decrypted;
            nBits = nBits / 8;
            byte[] data = Base64.decode(cipherText.toCharArray());
            /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
            byte[] k = Arrays.copyOf(key.getBytes(), nBits);
            /* AES/CTR/NoPadding (SIC == CTR) */
            org.bouncycastle.crypto.BufferedBlockCipher cipher = new org.bouncycastle.crypto.BufferedBlockCipher(new org.bouncycastle.crypto.modes.SICBlockCipher(new org.bouncycastle.crypto.engines.AESEngine()));
            cipher.reset();
            SecretKey secretKey = generateSecretKey(k, nBits);
            byte[] nonceBytes = Arrays.copyOf(Arrays.copyOf(data, 8), nBits / 2);
            IvParameterSpec nonce = new IvParameterSpec(nonceBytes);
            /* true == encrypt; false == decrypt */
            cipher.init(true, new org.bouncycastle.crypto.params.ParametersWithIV(new org.bouncycastle.crypto.params.KeyParameter(secretKey.getEncoded()), nonce.getIV()));
            decrypted = new byte[cipher.getOutputSize(data.length - 8)];
            int decLength = cipher.processBytes(data, 8, data.length - 8, decrypted, 0);
            cipher.doFinal(decrypted, decLength);
            /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
            return new String(decrypted);
        }

        private SecretKey generateSecretKey(byte[] keyBytes, int nBits) throws Exception {
            try {
                SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
                /* AES/ECB/NoPadding */
                org.bouncycastle.crypto.BufferedBlockCipher cipher = new org.bouncycastle.crypto.BufferedBlockCipher(new org.bouncycastle.crypto.engines.AESEngine());
                cipher.init(true, new org.bouncycastle.crypto.params.KeyParameter(secretKey.getEncoded()));
                keyBytes = new byte[cipher.getOutputSize(secretKey.getEncoded().length)];
                int decLength = cipher.processBytes(secretKey.getEncoded(), 0, secretKey.getEncoded().length, keyBytes, 0);
                cipher.doFinal(keyBytes, decLength);
            } catch (Throwable e) {
                return null;
            }
            System.arraycopy(keyBytes, 0, keyBytes, nBits / 2, nBits / 2);
            return new SecretKeySpec(keyBytes, "AES");
        }
    }
}