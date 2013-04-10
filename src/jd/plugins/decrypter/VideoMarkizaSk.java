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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

/**
 * supported: archiv doma, archiv markiza, fun tv, music tv (live stream capture is not supported)
 * 
 * @author butkovip
 * 
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, urls = { "http://video\\.markiza\\.sk/archiv\\-tv\\-markiza/[\\-a-z0-9]+/[0-9]+", "http://doma\\.markiza\\.sk/archiv\\-doma/[\\-a-z0-9]+/[0-9]+", "http://video\\.markiza\\.sk/(mini\\-music\\-tv|fun\\-tv)/[0-9]+/[\\-a-z0-9]+/[0-9]+", "http://(www\\.)?markiza\\.sk/clanok/aktualne/[^<>\"]*?\\.html" }, flags = { 0, 0, 0, 0 }, names = { "video.markiza.sk", "doma.markiza.sk", "video.markiza.sk", "video.markiza.sk" })
public class VideoMarkizaSk extends PluginForDecrypt {

    public VideoMarkizaSk(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * AES CTR(Counter) Mode for Java ported from AES-CTR-Mode implementation in JavaScript by Chris Veness
     * 
     */
    private String AESCounterModeDecrypt(final String cipherText, final String key, int nBits) {
        if (!(nBits == 128 || nBits == 192 || nBits == 256)) { return "Error: Must be a key mode of either 128, 192, 256 bits"; }
        String res = null;
        nBits = nBits / 8;
        final byte[] data = Base64.decode(cipherText.toCharArray());
        final byte[] k = Arrays.copyOf(key.getBytes(), nBits);
        try {
            final Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            final SecretKey secretKey = generateSecretKey(k, nBits);
            final byte[] nonceBytes = Arrays.copyOf(Arrays.copyOf(data, 8), nBits);
            final IvParameterSpec nonce = new IvParameterSpec(nonceBytes);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, nonce);
            res = new String(cipher.doFinal(data, 8, data.length - 8));
        } catch (final Throwable e) {
        }
        return res;
    }

    private String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink cryptedLink, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.clearCookies(getHost());
        br.setFollowRedirects(true);
        final String parameter = cryptedLink.getCryptedUrl();
        br.getPage(parameter);
        if (true) {
            logger.info("Cannot decrypt link because only registered or premium users can watch videos: " + parameter);
            return decryptedLinks;
        }
        DownloadLink dl;
        if (parameter.matches("http://(video\\.markiza\\.sk/archiv\\-tv\\-markiza/[\\-a-z0-9]+/[0-9]+|doma\\.markiza\\.sk/archiv\\-doma/[\\-a-z0-9]+/[0-9]+|video\\.markiza\\.sk/(mini\\-music\\-tv|fun\\-tv)/[0-9]+/[\\-a-z0-9]+/[0-9]+)")) {
            // retrieve playlist first
            final String playlistId = br.getRegex("div onclick=\"flowplayerPlayerOnThumb\\[\\(\\]\\'video_player_(.*?)\\'").getMatch(0);
            final String playlistUrl = br.getRegex("var flowplayerJSConfigScript = \\'(.*?)\\'").getMatch(0);
            if (null == playlistUrl || playlistUrl.trim().length() == 0 || null == playlistId || playlistId.trim().length() == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String playlist = playlistUrl + "media=" + playlistId;
            br.getPage(playlist);

            // parse playlist for valid links
            final String[][] links = br.getRegex("\"url\":\"http://vid1.markiza.sk/(.*?)[.]mp4\"").getMatches();
            if (null != links && 0 < links.length) {
                for (final String[] link : links) {
                    // we want valid entries only + no commercials
                    if (null != link && 1 == link.length && null != link[0] && 0 < link[0].trim().length()) {
                        decryptedLinks.add(createDownloadlink("http://vid1.markiza.sk/" + link[0] + ".mp4"));
                    }
                }
            }
        } else {
            final String media = br.getRegex("id=\"article_player_(\\d+)\"").getMatch(0);
            final String sectionID = br.getRegex("var ut_section_id = \"(\\d+)\"").getMatch(0);
            final String siteID = br.getRegex("var site_id = \"(\\d+)\"").getMatch(0);
            if (media == null || sectionID == null || siteID == null) {
                logger.warning("Decrypter broken for link or there is no video: " + parameter);
                return null;
            }
            br.getPage("http://www.markiza.sk/bin/player/flowplayer/config.php?site=" + siteID + "&subsite=&section=" + sectionID + "&media=" + media + "&jsVar=flowplayerJSConfig" + media);
            final String encStr = br.getRegex("var flowplayerJSConfig" + media + "\\s?=\\s?\'(.*?)\'").getMatch(0);
            if (encStr != null) {
                // is encrypted?
                if (encStr.codePointAt(0) != 123) {
                    final String t = AESCounterModeDecrypt(encStr, Encoding.Base64Decode("RWFEVXV0ZzRwcEdZWHdOTUZkUkpzYWRlbkZTbkk2Z0o="), 128);
                    final HashMap<String, String> ret = new HashMap<String, String>();
                    try {
                        for (final String[] r : new Regex(t, "\"(\\w+)\":\"([^\"]+)\"").getMatches()) {
                            ret.put(r[0], r[1]);
                        }
                        final String filename = decodeUnicode(ret.get("title"));
                        String result = ret.get("urlPattern");
                        if (result != null) {
                            result = result.replaceAll("\\{0\\}", ret.get("filename"));
                            result = result.replaceAll("(\\{|\\})", "");
                            result = result.replaceAll("\\\\", "");
                            dl = createDownloadlink(result);
                            if (filename != null) {
                                dl.setFinalFileName(filename.trim() + ".mp4");
                            }
                            decryptedLinks.add(dl);
                        }
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private SecretKey generateSecretKey(byte[] keyBytes, final int nBits) {
        try {
            final SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            keyBytes = cipher.doFinal(keyBytes);
        } catch (final Throwable e1) {
            return null;
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}