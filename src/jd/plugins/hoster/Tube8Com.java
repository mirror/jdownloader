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
import java.net.URL;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tube8.com" }, urls = { "http://(www\\.)?tube8\\.com/(?!(cat|latest)/)[^/]+/[^/]+/([^/]+/)?[0-9]+" }, flags = { 2 })
public class Tube8Com extends PluginForHost {

    private boolean             setEx  = true;
    private String              dllink = null;
    private static final String mobile = "mobile";

    public Tube8Com(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("http://www.tube8.com/signin.html");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/embed", ""));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (setEx) this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String verifyAge = br.getRegex("(<div class=\"enter\\-btn\">)").getMatch(0);
        if (verifyAge != null) {
            br.postPage(downloadLink.getDownloadURL(), "processdisclaimer=");
        }
        if (br.containsHTML("class=\"video\\-removed\\-div\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) \\- ").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("var videotitle=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("Tube8 bring you(.*?)for all").getMatch(0);
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        boolean failed = true;
        boolean preferMobile = getPluginConfig().getBooleanProperty(mobile, false);
        String videoDownloadUrls = "";

        /* streaming link */
        findStreamingLink();
        if (dllink != null && requestVideo(downloadLink)) failed = false;
        /* decrease HTTP requests */
        if (failed || preferMobile) videoDownloadUrls = standardAndMobile(downloadLink);
        /* normal link */
        if (failed) findNormalLink(videoDownloadUrls);
        if (failed && dllink != null && requestVideo(downloadLink)) failed = false;
        /* 3gp link */
        if (failed || preferMobile) findMobileLink(videoDownloadUrls);
        if ((failed || preferMobile) && dllink != null && requestVideo(downloadLink)) failed = false;

        if (dllink == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        filename = filename.trim();
        if (dllink.contains(".3gp")) {
            downloadLink.setFinalFileName((filename + ".3gp"));
        } else if (dllink.contains(".mp4")) {
            downloadLink.setFinalFileName((filename + ".mp4"));
        } else {
            downloadLink.setFinalFileName(filename + ".flv");
        }
        if (failed) return AvailableStatus.UNCHECKABLE;
        return AvailableStatus.TRUE;
    }

    private boolean requestVideo(final DownloadLink downloadLink) throws Exception {
        URLConnectionAdapter con = null;
        final Browser br2 = br.cloneBrowser();
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else if (br2.getHttpConnection().getResponseCode() == 401) {
                downloadLink.setProperty("401", 401);
                return true;
            } else {
                return false;
            }
            return true;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private String standardAndMobile(DownloadLink downloadLink) throws Exception {
        final String hash = br.getRegex("videoHash[\t\n\r ]+=[\t\n\r ]\"([a-z0-9]+)\"").getMatch(0);
        if (hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.getPage("http://www.tube8.com/ajax/getVideoDownloadURL.php?hash=" + hash + "&video=" + new Regex(downloadLink.getDownloadURL(), ".*?(\\d+)$").getMatch(0) + "&download_cdn=true&_=" + System.currentTimeMillis());
        String ret = br2.getRegex("^(.*?)$").getMatch(0);
        return ret != null ? ret.replace("\\", "") : "";
    }

    private void findMobileLink(final String correctedBR) throws Exception {
        dllink = new Regex(correctedBR, "\"mobile_url\":\"(http:.*?)\"").getMatch(0);
        if (dllink == null) dllink = new Regex(correctedBR, "\"(http://cdn\\d+\\.mobile\\.tube8\\.com/.*?)\"").getMatch(0);
    }

    private void findNormalLink(final String correctedBR) throws Exception {
        dllink = new Regex(correctedBR, "\"standard_url\":\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = new Regex(correctedBR, "\"(http://cdn\\d+\\.public\\.tube8\\.com/.*?)\"").getMatch(0);
    }

    private void findStreamingLink() throws Exception {
        String flashVars = br.getRegex("var flashvars = \\{([^\\}]+)").getMatch(0);
        if (flashVars == null) return;
        flashVars = flashVars.replaceAll("\"", "");
        Map<String, String> values = new HashMap<String, String>();

        for (String s : flashVars.split(",")) {
            if (!s.matches(".+:.+")) continue;
            values.put(s.split(":")[0], s.split(":", 2)[1]);
        }

        String isEncrypted = values.get("encrypted");
        dllink = values.get("video_url");
        if ("1".equals(isEncrypted) || Boolean.parseBoolean(isEncrypted)) {
            String decrypted = values.get("video_url");
            String key = values.get("video_title");
            try {
                dllink = new BouncyCastleAESCounterModeDecrypt().decrypt(decrypted, key, 256);
            } catch (Throwable e) {
                /* Fallback for stable version */
                dllink = AESCounterModeDecrypt(decrypted, key, 256);
            }
            if (dllink != null && (dllink.startsWith("Error:") || !dllink.startsWith("http"))) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, dllink); }
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.tube8.com/info.html#terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (downloadLink.getIntegerProperty("401", -1) == 401) {
                downloadLink.getProperties().remove("401");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
            }
            if (dl.getConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        login(account, br);
        setEx = false;
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (downloadLink.getIntegerProperty("401", -1) == 401) {
                downloadLink.getProperties().remove("401");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            this.login(account, this.br);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        /* only support for free accounts at the moment */
        ai.setUnlimitedTraffic();
        ai.setStatus("Account ok");
        account.setValid(true);
        return ai;
    }

    private void login(Account account, Browser br) throws IOException, PluginException {
        if (br == null) {
            br = new Browser();
        }
        this.setBrowserExclusive();
        boolean follow = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            br.postPage("http://www.tube8.com/ajax/login.php", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            if (br.containsHTML("invalid") || br.containsHTML("0\\|")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } finally {
            br.setFollowRedirects(follow);
        }
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
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
     * @see <a
     *      href="http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf">"Recommendation for Block Cipher Modes of Operation - Methods and Techniques"</a>
     */
    private String AESCounterModeDecrypt(String cipherText, String key, int nBits) throws Exception {
        if (!(nBits == 128 || nBits == 192 || nBits == 256)) { return "Error: Must be a key mode of either 128, 192, 256 bits"; }
        if (cipherText == null || key == null) { return "Error: cipher and/or key equals null"; }
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
            if (e.getMessage().contains("Illegal key size")) {
                getPolicyFiles();
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unlimited Strength JCE Policy Files needed!");
        } catch (Throwable e1) {
            return null;
        }
        System.arraycopy(keyBytes, 0, keyBytes, nBits / 2, nBits / 2);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private class BouncyCastleAESCounterModeDecrypt {
        private String decrypt(String cipherText, String key, int nBits) throws Exception {
            if (!(nBits == 128 || nBits == 192 || nBits == 256)) { return "Error: Must be a key mode of either 128, 192, 256 bits"; }
            if (cipherText == null || key == null) { return "Error: cipher and/or key equals null"; }
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
            cipher.init(true, new org.bouncycastle.crypto.params.ParametersWithIV(new org.bouncycastle.crypto.params.KeyParameter(secretKey.getEncoded()), ((IvParameterSpec) nonce).getIV()));
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

    private void getPolicyFiles() throws Exception {
        int ret = -100;
        UserIO.setCountdownTime(120);
        ret = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_LARGE, "Java Cryptography Extension (JCE) Error: 32 Byte keylength is not supported!", "At the moment your Java version only supports a maximum keylength of 16 Bytes but the keezmovies plugin needs support for 32 byte keys.\r\nFor such a case Java offers so called \"Policy Files\" which increase the keylength to 32 bytes. You have to copy them to your Java-Home-Directory to do this!\r\nExample path: \"jre6\\lib\\security\\\". The path is different for older Java versions so you might have to adapt it.\r\n\r\nBy clicking on CONFIRM a browser instance will open which leads to the downloadpage of the file.\r\n\r\nThanks for your understanding.", null, "CONFIRM", "Cancel");
        if (ret != -100) {
            if (UserIO.isOK(ret)) {
                LocalBrowser.openDefaultURL(new URL("http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html"));
                LocalBrowser.openDefaultURL(new URL("http://h10.abload.de/img/jcedp50.png"));
            } else {
                return;
            }
        }
    }

    private void setConfigElements() {
        ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), mobile, JDL.L("plugins.hoster.Tube8Com.setting.preferVideosForMobilePhones", "Prefer videos for mobile phones (3gp format)")).setDefaultValue(false);
        getConfig().addEntry(cond);
    }

}