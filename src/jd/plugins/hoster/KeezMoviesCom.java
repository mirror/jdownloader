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

import java.net.URL;
import java.security.InvalidKeyException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "keezmovies.com" }, urls = { "http://(www\\.)?(keezmovies\\.com/embed_player\\.php\\?v?id=\\d+|keezmoviesdecrypted\\.com/video/[\\w\\-]+)" })
public class KeezMoviesCom extends antiDDoSForHost {

    private String dllink         = null;
    private String FLASHVARS      = null;
    private String FLASHVARS_JSON = null;

    public KeezMoviesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("keezmoviesdecrypted.com/", "keezmovies.com/"));
    }

    private static final String default_extension = ".mp4";

    @Override
    protected boolean useRUA() {
        return true;
    }

    /*
     * IMPORTANT: If this plugin fails and we have problems with the encryption there are 2 ways around: 1. Add account support --> Download
     * button is available then AND 2. Use a mobile User-Agent so we can get non encrypted final links. Keep in mind that we might get lower
     * quality then - last tested 29.01.15, quality was the same as via normal stream (480p) though not sure if 720p or higher is also
     * available for mobile. Also, it might happen that not all videos are also available for mobile devices!
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        // DEV NOTE: you can get the DLLINK from the embed page without need for crypto!
        setBrowserExclusive();
        br.setFollowRedirects(false);
        /* Offline links should also get nice filenames. */
        if (!downloadLink.isNameSet()) {
            downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "([\\w\\-]+)$").getMatch(0));
        }
        String filename = null;
        // embed corrections
        if (downloadLink.getDownloadURL().contains(".com/embed_player.php")) {
            Browser br2 = br.cloneBrowser();
            getPage(br2, downloadLink.getDownloadURL());
            if (br2.containsHTML("<share>N/A</share>") || br2.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String realurl = br2.getRegex("<share>(http://[^<>\"]*?)</share>").getMatch(0);
            if (realurl != null) {
                br2.getPage(realurl);
                downloadLink.setUrlDownload(br2.getURL());
            } else {
                dllink = br2.getRegex("<flv_url>(http://[^<>\"]*?)</flv_url>").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                filename = downloadLink.getFinalFileName();
                if (filename == null) {
                    filename = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
                }
            }
        }
        if (downloadLink.getDownloadURL().matches("(?i-)http://(www\\.)?keezmovies\\.com/video/[\\w\\-]+")) {
            // Set cookie so we can watch all videos ;)
            br.setCookie("http://www.keezmovies.com/", "age_verified", "1");
            getPage(downloadLink.getDownloadURL());
            if (br.getRedirectLocation() != null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML(">This video has been removed<") || br.containsHTML("class=\"removed_video\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<span class=\"fn\" style=\"display:none\">([^<>\"]*?)</span>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\- KeezMovies\\.com</title>").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("<h1 class=\"title_video_page\">([^<>\"]*?)</h1>").getMatch(0);
            }
            FLASHVARS = br.getRegex("<param name=\"flashvars\" value=\"(.*?)\"").getMatch(0);
            FLASHVARS_JSON = br.getRegex("var flashvars\\s*=\\s*\\{(.*?)\\};").getMatch(0);
            if (FLASHVARS == null && FLASHVARS_JSON == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            FLASHVARS = Encoding.htmlDecode(FLASHVARS);
            String isEncrypted = getValue("encrypted");
            if ("1".equals(isEncrypted) || Boolean.parseBoolean(isEncrypted)) {
                String decrypted = getValue("video_url");
                if (decrypted == null) {
                    final String[] qualities = { "1080p", "720p", "480p", "360p", "320p", "240p", "180p" };
                    for (final String quality : qualities) {
                        decrypted = getValue("quality_" + quality);
                        if (decrypted != null) {
                            break;
                        }
                    }
                }
                /* Workaround for bad encoding */
                String key = new Regex(FLASHVARS, "video_title=([^<>]*?)\\&dislikeJs=").getMatch(0);
                if (key == null) {
                    key = getValue("video_title");
                }
                try {
                    dllink = new BouncyCastleAESCounterModeDecrypt().decrypt(decrypted, key, 256);
                } catch (Throwable e) {
                    /* Fallback for stable version */
                    dllink = AESCounterModeDecrypt(decrypted, key, 256);
                }
                if (dllink != null && (dllink.startsWith("Error:") || !dllink.startsWith("http"))) {
                    dllink = null;
                }
            } else {
                dllink = getValue("video_url");
            }

            /* All failed? Try to get html5 videourl. */
            if (dllink == null) {
                dllink = br.getRegex("src=\"(http[^<>\"]*?\\.mp4[^<>\"]*?)\"").getMatch(0);
            }

            if (filename == null || dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        String ext;
        if (dllink.contains(".flv")) {
            ext = ".flv";
        } else {
            ext = default_extension;
        }
        filename = Encoding.htmlDecode(filename).trim();
        downloadLink.setFinalFileName(filename + ext);
        dllink = Encoding.htmlDecode(dllink);
        URLConnectionAdapter con = br.openGetConnection(dllink);
        try {
            con = br.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
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
        /*
         * CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset!
         */
        byte[] k = Arrays.copyOf(key.getBytes(), nBits);

        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        SecretKey secretKey = generateSecretKey(k, nBits);
        byte[] nonceBytes = Arrays.copyOf(Arrays.copyOf(data, 8), nBits / 2);
        IvParameterSpec nonce = new IvParameterSpec(nonceBytes);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, nonce);
        /*
         * CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8
         */
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
            if (!(nBits == 128 || nBits == 192 || nBits == 256)) {
                return "Error: Must be a key mode of either 128, 192, 256 bits";
            }
            if (cipherText == null || key == null) {
                return "Error: cipher and/or key equals null";
            }
            byte[] decrypted;
            nBits = nBits / 8;
            byte[] data = Base64.decode(cipherText.toCharArray());
            /*
             * CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset!
             */
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
            /*
             * CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8
             */
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

    @Override
    public String getAGBLink() {
        return "http://www.keezmovies.com/information";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    private void getPolicyFiles() throws Exception {
        int ret = -100;
        UserIO.setCountdownTime(120);
        ret = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_LARGE, "Java Cryptography Extension (JCE) Error: 32 Byte keylength is not supported!", "At the moment your Java version only supports a maximum keylength of 16 Bytes but the keezmovies plugin needs support for 32 byte keys.\r\nFor such a case Java offers so called \"Policy Files\" which increase the keylength to 32 bytes. You have to copy them to your Java-Home-Directory to do this!\r\nExample path: \"jre6\\lib\\security\\\". The path is different for older Java versions so you might have to adapt it.\r\n\r\nMake sure to download the files that match your current Java version!\r\n\r\nBy clicking on CONFIRM a browser instance will open which leads to the downloadpage of the file.\r\n\r\nThanks for your understanding.", null, "CONFIRM", "Cancel");
        if (ret != -100) {
            if (UserIO.isOK(ret)) {
                LocalBrowser.openDefaultURL(new URL("http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html"));
                LocalBrowser.openDefaultURL(new URL("http://h10.abload.de/img/jcedp50.png"));
            } else {
                return;
            }
        }
    }

    private String getValue(String s) {
        if (FLASHVARS != null) {
            return new Regex(FLASHVARS, "&" + s + "=(.*?)(&|$)").getMatch(0);
        }
        if (FLASHVARS_JSON != null) {
            return PluginJSonUtils.getJsonValue(FLASHVARS_JSON, s);
        }
        return null;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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

}