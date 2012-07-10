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

import jd.PluginWrapper;
import jd.gui.UserIO;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "keezmovies.com" }, urls = { "http://(www\\.)?keezmovies\\.com/video/[\\w\\-]+" }, flags = { 2 })
public class KeezMoviesCom extends PluginForHost {

    private String DLLINK    = null;
    private String FLASHVARS = null;

    public KeezMoviesCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * AES CTR(Counter) Mode for Java ported from AES-CTR-Mode implementation in
     * JavaScript by Chris Veness
     * 
     * @see <a
     *      href="http://csrc.nist.gov/publications/nistpubs/800-38a/sp800-38a.pdf">"Recommendation for Block Cipher Modes of Operation - Methods and Techniques"</a>
     */
    private String AESCounterModeDecrypt(final String cipherText, final String key, int nBits) {
        if (!(nBits == 128 || nBits == 192 || nBits == 256)) { return "Error: Must be a key mode of either 128, 192, 256 bits"; }
        if (cipherText == null || key == null) { return "Error: cipher and/or key equals null"; }
        String res = null;
        nBits = nBits / 8;
        final byte[] data = Base64.decode(cipherText.toCharArray());
        final byte[] k = Arrays.copyOf(key.getBytes(), nBits);
        try {
            final Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            final SecretKey secretKey = generateSecretKey(k, nBits);
            final byte[] nonceBytes = Arrays.copyOf(Arrays.copyOf(data, 8), nBits / 2);
            final IvParameterSpec nonce = new IvParameterSpec(nonceBytes);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, nonce);
            res = new String(cipher.doFinal(data, 8, data.length - 8));
        } catch (final Throwable e) {
        }
        return res;
    }

    private SecretKey generateSecretKey(byte[] keyBytes, final int nBits) throws Exception {
        try {
            final SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            keyBytes = cipher.doFinal(keyBytes);
        } catch (final InvalidKeyException e) {
            if (e.getMessage().contains("Illegal key size")) {
                getPolicyFiles();
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unlimited Strength JCE Policy Files needed!");
        } catch (final Throwable e1) {
            return null;
        }
        System.arraycopy(keyBytes, 0, keyBytes, nBits / 2, nBits / 2);
        return new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String getAGBLink() {
        return "http://www.keezmovies.com/information";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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

    private String getValue(final String s) {
        return new Regex(FLASHVARS, "\\&" + s + "=(.*?)(\\&|$)").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(false);
        // Set cookie so we can watch all videos ;)
        br.setCookie("http://www.keezmovies.com/", "age_verified", "1");
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<span class=\"fn\" style=\"display:none\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?) - KeezMovies\\.com</title>").getMatch(0);
        }
        FLASHVARS = br.getRegex("<param name=\"flashvars\" value=\"(.*?)\"").getMatch(0);
        if (FLASHVARS == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        FLASHVARS = Encoding.htmlDecode(FLASHVARS);
        final String isEncrypted = getValue("encrypted");
        if ("1".equals(isEncrypted) || Boolean.parseBoolean(isEncrypted)) {
            DLLINK = AESCounterModeDecrypt(getValue("video_url"), getValue("video_title"), 256);
            if (DLLINK != null && DLLINK.startsWith("Error:")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, DLLINK); }
        } else {
            DLLINK = getValue("video_url");
        }

        if (filename == null || DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        downloadLink.setFinalFileName(filename.trim() + ".flv");
        DLLINK = Encoding.htmlDecode(DLLINK);
        URLConnectionAdapter con = br.openGetConnection(DLLINK);
        try {
            con = br.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
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