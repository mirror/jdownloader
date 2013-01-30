package jd.plugins.hoster;

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
import jd.gui.UserIO;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornhub.com" }, urls = { "http://(www\\.)?((de|fr|it|es|pt)\\.)?pornhub\\.com/(view_video\\.php\\?viewkey=|embed/)\\d+" }, flags = { 0 })
public class PornHubCom extends PluginForHost {

    private String dlUrl = null;

    public PornHubCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("((de|fr|it|es|pt)\\.)pornhub\\.com/", "pornhub.com/"));
        link.setUrlDownload(link.getDownloadURL().replaceAll("embed/", "view_video.php?viewkey="));
    }

    @Override
    public String getAGBLink() {
        return "http://www.pornhub.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestVideo(link);
        if (dlUrl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlUrl, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        requestVideo(downloadLink);
        setBrowserExclusive();
        br.setFollowRedirects(true);
        try {
            if (!br.openGetConnection(dlUrl).getContentType().contains("html")) {
                downloadLink.setDownloadSize(br.getHttpConnection().getLongContentLength());
                br.getHttpConnection().disconnect();
                return AvailableStatus.TRUE;
            }
        } finally {
            if (br.getHttpConnection() != null) {
                br.getHttpConnection().disconnect();
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    private AvailableStatus requestVideo(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setCookie("http://pornhub.com/", "age_verified", "1");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().equals("http://www.pornhub.com/") || !br.containsHTML("\\.swf")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String file_name = br.getRegex("<title>([^<>]*?) \\- Pornhub\\.com</title>").getMatch(0);
        if (file_name == null) file_name = br.getRegex("\"section_title overflow\\-title overflow\\-title\\-width\">([^<>]*?)</h1>").getMatch(0);

        getVideoLink(downloadLink.getDownloadURL());

        if (file_name == null || dlUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setFinalFileName(file_name.replace("\"", "'") + ".mp4");
        return AvailableStatus.TRUE;
    }

    private void getVideoLink(String dllink) throws Exception {
        String flashVars = br.getRegex("var flashvars = \\{([^\\}]+)").getMatch(0);
        if (flashVars == null) return;
        flashVars = flashVars.replaceAll("\"", "");
        Map<String, String> values = new HashMap<String, String>();

        for (String s : flashVars.split(",")) {
            if (!s.matches(".+:.+")) continue;
            values.put(s.split(":")[0], Encoding.htmlDecode(s.split(":", 2)[1]));
        }

        String isEncrypted = values.get("encrypted");
        dlUrl = values.get("video_url");
        if ("1".equals(isEncrypted) || Boolean.parseBoolean(isEncrypted)) {
            String encrypted = values.get("video_url");
            String key = values.get("video_title");
            try {
                dlUrl = new BouncyCastleAESCounterModeDecrypt().decrypt(encrypted, key, 256);
            } catch (Throwable e) {
                /* Fallback for stable version */
                dlUrl = AESCounterModeDecrypt(encrypted, key, 256);
            }
            if (dlUrl != null && (dlUrl.startsWith("Error:") || !dlUrl.startsWith("http"))) {
                logger.warning("pornhub.com: " + dlUrl);
                dlUrl = null;
            }
        }
    }

    /**
     * AES CTR(Counter) Mode for Java ported from AES-CTR-Mode implementation in
     * JavaScript by Chris Veness
     * 
     * @see <a
     *      href="http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf">"Recommendation for Block Cipher Modes of Operation - Methods and Techniques"</a>
     */
    private String AESCounterModeDecrypt(String cipherText, String key, int nBits) throws Exception {
        if (!(nBits == 128 || nBits == 192 || nBits == 256)) return "Error: Must be a key mode of either 128, 192, 256 bits";
        if (cipherText == null || key == null) return "Error: cipher and/or key equals null";
        String res = null;
        nBits = nBits / 8;
        byte[] data = Base64.decode(cipherText.toCharArray());
        byte[] k = Arrays.copyOf(key.getBytes(), nBits);

        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        SecretKey secretKey = generateSecretKey(k, nBits);
        byte[] nonceBytes = Arrays.copyOf(Arrays.copyOf(data, 8), nBits / 2);
        IvParameterSpec nonce = new IvParameterSpec(nonceBytes);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, nonce);
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
            if (e.getMessage().contains("Illegal key size")) getPolicyFiles();
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unlimited Strength JCE Policy Files needed!");
        } catch (Throwable e1) {
            return null;
        }
        System.arraycopy(keyBytes, 0, keyBytes, nBits / 2, nBits / 2);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private class BouncyCastleAESCounterModeDecrypt {
        private String decrypt(String cipherText, String key, int nBits) throws Exception {
            if (!(nBits == 128 || nBits == 192 || nBits == 256)) return "Error: Must be a key mode of either 128, 192, 256 bits";
            if (cipherText == null || key == null) return "Error: cipher and/or key equals null";
            byte[] decrypted;
            nBits = nBits / 8;
            byte[] data = Base64.decode(cipherText.toCharArray());
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}