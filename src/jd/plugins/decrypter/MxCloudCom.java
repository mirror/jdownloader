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

package jd.plugins.decrypter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDHexUtils;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mixcloud.com" }, urls = { "http://(www\\.)?mixcloud\\.com/[\\w\\-]+/[\\w\\-]+/" }, flags = { 0 })
public class MxCloudCom extends PluginForDecrypt {

    private String MAINPAGE = "http://www.mixcloud.com";

    public MxCloudCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "http://(www\\.)?mixcloud\\.com/((developers|categories|media|competitions|tag)/.+|[\\w\\-]+/playlists.+)";

    private byte[] AESdecrypt(final byte[] plain, final byte[] key, final byte[] iv) throws Exception {
        final KeyParameter keyParam = new KeyParameter(key);
        final CipherParameters cipherParams = new ParametersWithIV(keyParam, iv);

        // Prepare the cipher (AES, CBC, no padding)
        final BufferedBlockCipher cipher = new BufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        cipher.reset();
        cipher.init(false, cipherParams);

        // Perform the decryption
        final byte[] decrypted = new byte[cipher.getOutputSize(plain.length)];
        int decLength = cipher.processBytes(plain, 0, plain.length, decrypted, 0);
        cipher.doFinal(decrypted, decLength);

        return decrypted;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setReadTimeout(3 * 60 * 1000);
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.getRedirectLocation() != null) {
            logger.info("Unsupported or offline link: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("<title>404 Error page")) {
            logger.info("Offline link: " + parameter);
            return decryptedLinks;
        }
        String theName = br.getRegex("class=\"cloudcast\\-name\" itemprop=\"name\">(.*?)</h1>").getMatch(0);
        if (theName == null) {
            theName = br.getRegex("data-resourcelinktext=\"(.*?)\"").getMatch(0);
        }
        if (theName == null) { return null; }

        final String playResource = parameter.replace(MAINPAGE, "");
        String playerUrl = br.getRegex("playerUrl:\'(.*?)\'").getMatch(0);
        playerUrl = playerUrl == null ? MAINPAGE + "/player/" : MAINPAGE + playerUrl;
        br.setCookie(MAINPAGE, "play-uri", Encoding.urlEncode(playResource));
        br.getPage(playerUrl);
        String playInfoUrl = br.getRegex("playinfo: ?\'(.*?)\'").getMatch(0);
        final String playModuleSwfUrl = br.getRegex("playerModuleSwfUrl: ?\'(.*?)\'").getMatch(0);
        if (playInfoUrl == null || playModuleSwfUrl == null) { return null; }

        playInfoUrl = playInfoUrl + "?key=" + playResource + "&module=" + playModuleSwfUrl + "&page=" + playerUrl;

        byte[] enc = null;
        try {
            br.setKeepResponseContentBytes(true);
            br.getPage(playInfoUrl);
            /* will throw Exception in stable <=9581 */
            if (br.getRequest().isContentDecoded()) {
                enc = br.getRequest().getResponseBytes();
            } else {
                throw new Exception("use fallback");
            }
        } catch (final Throwable e) {
            /* fallback */
            String encryptedContent = "";
            try {
                final URL url = new URL(playInfoUrl);
                final InputStream page = url.openStream();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(page));
                try {
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        encryptedContent = line;
                    }
                } finally {
                    try {
                        page.close();
                    } catch (final Throwable e3) {
                    }
                    try {
                        reader.close();
                    } catch (final Throwable e2) {
                    }
                }
            } catch (final Throwable e3) {
                return null;
            }
            enc = jd.crypt.Base64.decode(encryptedContent);
        }
        final byte[] key = JDHexUtils.getByteArray(Encoding.Base64Decode("NjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MQ=="));
        final byte[] iv = new byte[16];
        System.arraycopy(enc, 0, iv, 0, 16);
        String result = null;
        try {
            /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
            result = new String(AESdecrypt(enc, key, iv)).substring(16);
        } catch (final InvalidKeyException e) {
            if (e.getMessage().contains("Illegal key size")) {
                getPolicyFiles();
                return null;
            }
        } catch (final Throwable e) {
        }

        URLConnectionAdapter con = null;
        final String[] links = new Regex(result, "\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) { return null; }
        HashMap<String, Long> alreadyFound = new HashMap<String, Long>();
        for (final String dl : links) {
            if (!dl.endsWith(".mp3") && !dl.endsWith(".m4a")) {
                continue;
            }
            final DownloadLink dlink = createDownloadlink("directhttp://" + dl);
            dlink.setFinalFileName(Encoding.htmlDecode(theName).trim() + new Regex(dl, "(\\..{3}$)").getMatch(0));
            /* Nicht alle Links im Array sets[] sind verfügbar. */
            try {
                con = br.openGetConnection(dl);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    continue;
                }
                if (alreadyFound.get(dlink.getName()) != null && alreadyFound.get(dlink.getName()) == con.getLongContentLength()) {
                    continue;
                } else {
                    alreadyFound.put(dlink.getName(), con.getLongContentLength());
                    dlink.setAvailable(true);
                    dlink.setDownloadSize(con.getLongContentLength());
                    decryptedLinks.add(dlink);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(theName));
        fp.addLinks(decryptedLinks);
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private void getPolicyFiles() throws Exception {
        int ret = -100;
        UserIO.setCountdownTime(120);
        ret = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_LARGE, "Java Cryptography Extension (JCE) Error: 32 Byte keylength is not supported!", "At the moment your Java version only supports a maximum keylength of 16 Bytes but the veoh plugin needs support for 32 byte keys.\r\nFor such a case Java offers so called \"Policy Files\" which increase the keylength to 32 bytes. You have to copy them to your Java-Home-Directory to do this!\r\nExample path: \"jre6\\lib\\security\\\". The path is different for older Java versions so you might have to adapt it.\r\n\r\nBy clicking on CONFIRM a browser instance will open which leads to the downloadpage of the file.\r\n\r\nThanks for your understanding.", null, "CONFIRM", "Cancel");
        if (ret != -100) {
            if (UserIO.isOK(ret)) {
                LocalBrowser.openDefaultURL(new URL("http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html"));
            } else {
                return;
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}