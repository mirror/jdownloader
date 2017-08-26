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
package jd.plugins.decrypter;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.utils.JDHexUtils;
import jd.utils.RazStringBuilder;

import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.multiclickcaptcha.MultiClickedPoint;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.sweetcaptcha.CaptchaHelperCrawlerPluginSweetCaptcha;
import org.jdownloader.plugins.components.RefreshSessionLink;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.components.config.KissanimeToConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

/**
 *
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "kissanime.to", "kissasian.com", "kisscartoon.me", "kissmanga.com" }, urls = { "https?://(?:www\\.)?kissanime\\.(?:com|to|ru)/anime/[a-zA-Z0-9\\-\\_]+/[a-zA-Z0-9\\-\\_]+(?:\\?id=\\d+)?", "http://kissasian\\.(?:com|ch)/[^/]+/[A-Za-z0-9\\-]+/[^/]+(?:\\?id=\\d+)?", "https?://(?:kisscartoon\\.(?:me|io)|kimcartoon\\.me)/[^/]+/[A-Za-z0-9\\-]+/[^/]+(?:\\?id=\\d+)?", "https?://(?:www\\.)?kissmanga\\.com/Manga/.+\\?id=\\d+" })
public class KisAmeCm extends antiDDoSForDecrypt implements RefreshSessionLink {
    public KisAmeCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return KissanimeToConfig.class;
    }

    private enum HostType {
        KISS_ANIME,
        KISS_ASIAN,
        KISS_CARTOON,
        KISS_MANGA,
        KISS_UNKNOWN;
        private static HostType parse(final String link) {
            if (StringUtils.containsIgnoreCase(link, "kissanime")) {
                return KISS_ANIME;
            } else if (StringUtils.containsIgnoreCase(link, "kissasian")) {
                return KISS_ASIAN;
            } else if (StringUtils.containsIgnoreCase(link, "cartoon")) {
                return KISS_CARTOON;
            } else if (StringUtils.containsIgnoreCase(link, "manga")) {
                return KISS_MANGA;
            } else {
                return KISS_UNKNOWN;
            }
        }
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final HostType hostType = HostType.parse(param.toString());
        final String parameter = param.toString().replace("asian.com/", "asian.ch/");
        final KissanimeToConfig pc = PluginJsonConfig.get(KissanimeToConfig.class);
        if (pc == null) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "User has no config!");
        }
        final boolean grabBEST = pc.isGrabBestVideoVersionEnabled();
        final boolean grab1080p = pc.isGrab1080pVideoEnabled();
        final boolean grab720p = pc.isGrab720pVideoEnabled();
        final boolean grab480p = pc.isGrab480pVideoEnabled();
        final boolean grab360p = pc.isGrab360pVideoEnabled();
        br.setFollowRedirects(true);
        getPage(parameter);
        /* Error handling */
        if (isOffline(br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final HashMap<String, DownloadLink> qualities = new HashMap<String, DownloadLink>();
        handleHumanCheck(br);
        String title;
        if (hostType == HostType.KISS_MANGA) {
            // crypto!
            title = br.getRegex("<title>(?:\\s*Read manga\\s*)?(.*?)(?:\\s*online\\s*in\\s*high\\s*quality\\s*)?</title>").getMatch(0);
            if (title != null) {
                title = title.replace("[\r\n]+", " ").replaceAll("\\s+", " ");
            }
            final String skey = getSecretKeyManga();
            final String iv = "a5e8e2e9c2721be0a84ad660c472c1f3";
            final String[] links = br.getRegex("lstImages\\.push\\(wrapKA\\(\"([a-zA-Z0-9\\+/]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String link : links) {
                // decrypt
                final String singleLink = decodeSingleURL(link, skey, iv);
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        } else {
            title = br.getRegex("<title>\\s*(.*?)\\s*- Watch\\s*\\1[^<]*</title>").getMatch(0);
            if (title == null) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            title = title.replaceAll("\\s+", " ");
            final String url_base = br.getURL();
            String[] mirrors = br.getRegex("(\\&s=[A-Za-z0-9\\-_]+)\"").getColumn(0);
            if (mirrors.length == 0) {
                mirrors = new String[] { "dummy" };
            }
            for (int mirror_number = 0; mirror_number <= mirrors.length - 1; mirror_number++) {
                final String mirror_param = mirrors[mirror_number];
                // we are already on mirror 0, but the mirrors list might not be in sequence as without parm it could be entry 2 3 etc...
                if (!"dummy".equals(mirror_param)) {
                    getPage(url_base + mirror_param);
                }
                // we have two things we need to base64decode
                final String[][] quals = getQuals(br);
                if (quals != null) {
                    String skey = null;
                    String iv = null;
                    switch (hostType) {
                    case KISS_ANIME:
                        skey = getSecretKeyAnime();
                        iv = "a5e8d2e9c1721ae0e84ad660c472c1f3";
                        break;
                    case KISS_ASIAN:
                        skey = getSecretKeyAsian();
                        iv = "32b812e9a1321ae0e84af660c4722b3a";
                        break;
                    case KISS_CARTOON:
                        skey = getSecretKeyCartoon();
                        iv = "a5e8d2e9c1721ae0e84ad660c472c1f3";
                    default:
                        break;
                    }
                    for (final String qual[] : quals) {
                        String decode = null;
                        switch (hostType) {
                        case KISS_ANIME:
                        case KISS_ASIAN:
                        case KISS_CARTOON:
                            decode = decodeSingleURL(qual[1], skey, iv);
                            break;
                        default:
                            break;
                        }
                        if (decode == null) {
                            continue;
                        }
                        final String quality = qual[2];
                        final DownloadLink dl = createDownloadlink(decode);
                        dl.setProperty("refresh_url_plugin", getHost());
                        // we need to know the proper mirror id, as refresh will try with out parameter and will fail
                        dl.setProperty("source_url", parameter + (!"dummy".equals(mirror_param) ? mirror_param : ""));
                        dl.setProperty("source_quality", quality);
                        dl.setFinalFileName(title + "-" + quality + ".mp4");
                        dl.setAvailableStatus(AvailableStatus.TRUE);
                        /* Best comes first --> Simply quit the loop if user wants best quality. */
                        if (grabBEST) {
                            decryptedLinks.add(dl);
                            break;
                        }
                        qualities.put(quality, dl);
                    }
                    if (!grabBEST) {
                        if (grab1080p && qualities.containsKey("1080p")) {
                            decryptedLinks.add(qualities.get("1080p"));
                        }
                        if (grab720p && qualities.containsKey("720p")) {
                            decryptedLinks.add(qualities.get("720p"));
                        }
                        if (grab480p && qualities.containsKey("480p")) {
                            decryptedLinks.add(qualities.get("480p"));
                        }
                        if (grab360p && qualities.containsKey("360p")) {
                            decryptedLinks.add(qualities.get("360p"));
                        }
                    }
                } else {
                    /* iframed.. seen openload.. but maybe others */
                    final String link = br.getRegex("\\$\\('#divContentVideo'\\)\\.html\\('<iframe\\s+[^>]* src=\"(.*?)\"").getMatch(0);
                    if (link != null) {
                        decryptedLinks.add(createDownloadlink(link));
                        continue;
                    }
                    // can be obstructed also
                    final String obstruction = br.getRegex("\\$\\('#divContentVideo iframe'\\)\\.attr\\('src',\\s*\\$kissenc\\.decrypt\\('([^']+)'\\)").getMatch(0);
                    if (obstruction != null) {
                        String skey = null;
                        String iv = null;
                        switch (hostType) {
                        case KISS_ANIME:
                            skey = getSecretKeyAnime();
                            iv = "a5e8d2e9c1721ae0e84ad660c472c1f3";
                            break;
                        case KISS_ASIAN:
                            skey = getSecretKeyAsian();
                            iv = "32b812e9a1321ae0e84af660c4722b3a";
                            break;
                        case KISS_CARTOON:
                            skey = getSecretKeyCartoon();
                            iv = "a5e8d2e9c1721ae0e84ad660c472c1f3";
                        default:
                            break;
                        }
                        String decode = null;
                        switch (hostType) {
                        case KISS_ANIME:
                        case KISS_ASIAN:
                        case KISS_CARTOON:
                            decode = decodeSingleURL(obstruction, skey, iv);
                            break;
                        default:
                            break;
                        }
                        if (decode == null) {
                            continue;
                        }
                        final DownloadLink dl = createDownloadlink(decode);
                        dl.setProperty("refresh_url_plugin", getHost());
                        // we need to know the proper mirror id, as refresh will try with out parameter and will fail
                        dl.setProperty("source_url", parameter + (!"dummy".equals(mirror_param) ? mirror_param : ""));
                        dl.setFinalFileName(title + ".mp4");
                        dl.setAvailableStatus(AvailableStatus.TRUE);
                        decryptedLinks.add(dl);
                    }
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    public static String[][] getQuals(final Browser br) {
        String[][] quals = null;
        final String qualityselection = br.getRegex("<select id=\"(?:selectQuality|slcQualix)\">.*?</select").getMatch(-1);
        if (qualityselection != null) {
            quals = new Regex(qualityselection, "<option [^>]*value\\s*=\\s*('|\"|)(.*?)\\1[^>]*>(\\d+p)").getMatches();
        }
        return quals;
    }

    private String getSecretKeyAnime() {
        String match1 = br.getRegex("<script[^>]*>\\s*var.*?\\[\"([^\"]+)\",.*?CryptoJS.SHA256\\(").getMatch(0);
        String skey = "nhasasdbasdtene7230asb";
        if (match1 != null) {
            skey = Encoding.unicodeDecode(match1);
        }
        boolean match2 = br.getRegex("src=\"/Scripts/shal.js\">").matches();
        if (match2) {
            skey += "6n23ncasdln213";
        }
        String match2b = br.getRegex("\\[\"([^\"]+)\"\\]; skH \\+=").getMatch(0);
        if (match2b != null) {
            skey += Encoding.unicodeDecode(match2b);
        }
        boolean match3 = br.getRegex("src=\"/Scripts/file3.js\">").matches();
        if (match3) {
            skey = skey.replace("a", "c");
        }
        String match3b = br.getRegex("\\[\"([^\"]+)\", \"\\\\x72\\\\x65\\\\x70\\\\x6C\\\\x61\\\\x63\\\\x65\"\\]; skH =").getMatch(0);
        if (match3b != null) {
            skey = skey.replace("a", Encoding.unicodeDecode(match3b));
        }
        return skey;
    }

    private String getSecretKeyAsian() throws Exception {
        final Browser ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        postPage(ajax, "/External/RSK", "krsk=665");
        String postData = ajax.toString();
        String skey = postData;
        String varName = br.getRegex("\\\\x67\\\\x65\\\\x74\\\\x53\\\\x65\\\\x63\\\\x72\\\\x65\\\\x74\\\\x4B\\\\x65\\\\x79\"];var ([^=]+)=\\$kissenc").getMatch(0);
        String match1 = br.getRegex("<script[^>]*>\\s*" + varName + " \\+= '([^']+)';\\s*</").getMatch(0);
        if (match1 != null) {
            skey += match1;
        }
        String[][] match2 = br.getRegex("var _x1 = '([^']+)'.*?x352\\('([^']+)',.*?'([^']+)';\\s*</").getMatches();
        if (match2.length != 0) {
            skey = match2[0][1] + "9c" + match2[0][0] + skey + match2[0][2];
        }
        String[][] match3 = br.getRegex("'k', 'l', 'm'];[^']+'([^']+)'[^']+'([^']+)';").getMatches();
        if (match3.length != 0) {
            skey = match3[0][0] + "uef" + skey + match3[0][1];
        }
        return skey;
    }

    /**
     * copy of getSecretKeyAnime with slight mods.
     *
     * @author raztoki
     * @author zt333
     */
    private String getSecretKeyCartoon() throws Exception {
        final Browser ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        postPage(ajax, "/External/RSK", "krsk=1378");
        String skey = ajax.toString();
        String varName = br.getRegex("\\\\x67\\\\x65\\\\x74\\\\x53\\\\x65\\\\x63\\\\x72\\\\x65\\\\x74\\\\x4B\\\\x65\\\\x79\"];var ([^=]+)=\\$kissenc").getMatch(0);
        String match1 = br.getRegex("\\]; " + varName + " \\+= '([^']+)';").getMatch(0);
        if (match1 != null) {
            skey = skey + match1 + skey;
        }
        String[] match2 = br.getRegex(varName + " = x951\\('([^']+)',.*?'([^']+)';\\s*</").getRow(0);
        if (match2 != null && match2.length != 0) {
            skey = match2[0] + "pkr" + skey + "9bs" + match2[1];
        }
        String[] match3 = br.getRegex(varName + " = x952\\('([^']+)',.*?'([^']+)';\\s*</").getRow(0);
        if (match3 != null && match3.length != 0) {
            skey = match3[0] + "n1f" + skey + match3[1];
        }
        return skey;
    }

    private String getSecretKeyManga() throws Exception {
        String skey = "mshsdf832nsdbash20asdm";
        String match1 = br.getRegex("<script[^>]*>\\s*var.*?\\[\"([^\"]+)\"\\]; chko = _.*?CryptoJS.SHA256.*?</script>").getMatch(0);
        if (match1 != null) {
            skey = Encoding.unicodeDecode(match1);
        }
        String match2 = br.getRegex("<script[^>]*>\\s*var.*?\\[\"([^\"]+)\"\\]; chko = chko.*?CryptoJS.SHA256.*?</script>").getMatch(0);
        if (match2 != null) {
            skey += Encoding.unicodeDecode(match2);
        }
        return skey;
    }

    public String decodeSingleURL(final String encodedString, final String skey, final String iv) {
        String decode = null;
        try {
            byte[] encodedArray = Base64.decode(encodedString);
            String hash = Hash.getSHA256(skey);
            // AES256
            final byte[] byteKey = JDHexUtils.getByteArray(hash);
            final byte[] byteIv = JDHexUtils.getByteArray(iv);
            SecretKeySpec skeySpec = new SecretKeySpec(byteKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(byteIv);
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
                byte[] byteResult = cipher.doFinal(encodedArray);
                decode = new String(byteResult, "UTF-8");
            } catch (final Exception e) {
                logger.info(e.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // decode:
            // http://kissasian.com/Play?key=AaKw5OKkuxsCX+g11bWAThZKjXqDGPQWhjJWnuT5dTGj1fd1cpjSu3IoG+BS9DrWjnEmbfIX6hzW21Wg+0x1x2xKpQsxMSf2Qq+Sj0x1x2xXplONQDkgERfg+oOe+8gdidU13A97bfhucXMOZpuH+qEyMsodmaW+HdcTG3FwUxF3b2QSmHTllXLU3LLVSvxWtiICNbHITdp9r9yaf6r9fVHXTvmGgwvQqC7Kn6A2VYb8FDelQLZfIcXr8xP9Gcjp2rZw9UTCZetxz6tPFXxUAOBTNfMirs90x1x2xNgCaH3CY2Dd0CnmDWTetRbccDcEwQ0gwGnXUrtsjcjnRykZZ53lFlyfsoWk8RmJc4QQKF58fRMuZNcB9My7lmIO5km6Y+sr0x1x2xGUJqlPB9YV1GH4PiiR4YS8XYwFg21p0x1x2x1VjNDsCKIVQSwCWw622f5Fh45xaWNzys0x1x2xqPuIeYiXHYReyUodf6hoWtfatwxeenjIV71W7JNfuVaxvpjasM0ahDd3QevYcZa3WJLDmMxKVvFmfXdGu0Mp647bmb4FPOP0x1x2xwQfOiFNWfl4d974FAztZyGzGMNxBDFMR4oqaVKCG+U0x1x2xP3bDAVQjW+1Ig9aIifmHzusAchhcVI8vsa9m2SarsZ4mjVzC3hikmvP6+ojddwd99fJP3tupbAL6sI3FDmf8O4ipGD5jMPUDeR+BvtD90lLA12cQXg0Oj+91gv4FR4JRC5oHybnjkFCUnSRSYiB7AGg5YrpxHq6WV7m2JuEWHlqUpNh+OCZEXriY+SMbNOK+LLobcA9KF10nSA3TarRRyMBPpLZquyeGkKyjxThfpIP1RYppeUJPP6Dcogfc00ESESAK0rGZELE6ahHwpB2eA==
            if (StringUtils.contains(decode, "kissasian.com/") || StringUtils.contains(decode, "blogspot.com/")) {
                // this is redirect bullshit
                final Browser test = br.cloneBrowser();
                test.setFollowRedirects(false);
                getPage(test, decode);
                // timeout:
                // Location: /Message/UnknownError?aspxerrorpath=/Play
                // <html><head><title>Object moved</title></head><body>
                // <h2>Object moved to <a href="/Message/UnknownError?aspxerrorpath=/Play">here</a>.</h2>
                // </body></html>
                // 503:
                decode = test.getRedirectLocation();
            }
        } catch (final Throwable e) {
        }
        return decode;
    }

    public static boolean isOffline(final Browser br) {
        return br.containsHTML("Page Not Found") || br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404;
    }

    private void handleHumanCheck(final Browser br) throws Exception {
        int retries = 5;
        loop: while (retries-- > 0) {
            if (isAbort()) {
                throw new InterruptedException("Aborted");
            }
            final Form ruh = br.getFormbyActionRegex("/Special/AreYouHuman.*");
            // recaptchav2 event can happen here
            if (br.containsHTML("<title>\\s*Are You Human\\s*</title>") || ruh != null) {
                if (ruh == null) {
                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                }
                if (ruh.containsHTML("g-recaptcha")) {
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                    ruh.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    submitForm(br, ruh);
                } else if (ruh.containsHTML("solvemedia\\.com/papi/")) {
                    final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                    final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    final String code = getCaptchaCode("solvemedia", cf, param);
                    if ("".equals(code)) {
                        // refresh (f5) button returns "", but so does a empty response by the user (send button)
                        continue;
                    }
                    final String chid = sm.getChallenge(code);
                    ruh.put("adcopy_response", Encoding.urlEncode(code));
                    ruh.put("adcopy_challenge", Encoding.urlEncode(chid));
                    submitForm(br, ruh);
                    if (br.containsHTML("solvemedia\\.com/papi/")) {
                        continue;
                    }
                    break;
                } else if (ruh.containsHTML("/Special/CapImg\\?")) {
                    /**
                     * select an image from selection that best fits provided phrase
                     *
                     * @date 20170511 select multi from image selection that best fits provided phrase
                     * @date 20170517
                     *
                     * @author raztoki
                     */
                    final String phraseSingle = ruh.getRegex("Choose the most suitable image for: <span style=\".*?\">(.*?)</span></p>").getMatch(0);
                    final String[] phraseMulti = ruh.getRegex("(Choose the)\\s*<b>(\\w+)</b>\\s*(suitable images for\\s*:)\\s*<span style=\"[^\"]*\">\\s*([^<>]+)\\s*</span>(?:(\\s*and\\s*)<span style=\"[^\"]*\">\\s*([^<>]+)\\s*</span>)*").getRow(0);
                    final String[] captchaImages = ruh.getRegex("('|\")(/Special/CapImg\\?f=.*?)\\1").getColumn(1);
                    if (captchaImages == null || captchaImages.length == 0) {
                        throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                    }
                    // single click
                    if (phraseSingle != null) {
                        final BufferedImage[] images = new BufferedImage[captchaImages.length];
                        final File stitchedImageOutput = getStitchedImage(captchaImages, images);
                        final ClickedPoint c = getCaptchaClickedPoint(getHost(), stitchedImageOutput, param, null, phraseSingle);
                        final Integer click = getClickedImageLocation(c, images);
                        ruh.put("answerCap", String.valueOf(click));
                        submitForm(br, ruh);
                        // error checking, they say wrong when answer is correct... happens in browser also.
                        if (br.toString().startsWith("Wrong answer. Click <a href='/Special/AreYouHuman?")) {
                            final String link = br.getRegex("<a href=('|\")(.*?)\\1").getMatch(1);
                            getPage(link);
                            continue loop;
                        }
                        return;
                    }
                    // multi click
                    else if (phraseMulti != null && phraseMulti.length != 0) {
                        final BufferedImage[] images = new BufferedImage[captchaImages.length];
                        final File stitchedImageOutput = getStitchedImage(captchaImages, images);
                        final MultiClickedPoint c = getMultiCaptchaClickedPoint(getHost(), stitchedImageOutput, param, null, RazStringBuilder.buildString(phraseMulti));
                        final Integer[] clicks = getClickedImageLocation(c, images);
                        ruh.put("answerCap", Encoding.urlEncode(RazStringBuilder.buildString(clicks, ",") + ","));
                        submitForm(br, ruh);
                        // error checking, they say wrong when answer is correct... happens in browser also.
                        if (br.toString().startsWith("Wrong answer. Click <a href='/Special/AreYouHuman")) {
                            final String link = br.getRegex("<a href=('|\")(.*?)\\1").getMatch(1);
                            getPage(link);
                            continue loop;
                        }
                        return;
                    } else {
                        throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                    }
                } else if (ruh.containsHTML("sweetcaptcha\\.com/api/v\\d+/apps/")) {
                    new CaptchaHelperCrawlerPluginSweetCaptcha(this, br).setFormValues(ruh);
                    submitForm(br, ruh);
                } else {
                    // unsupported captcha type?
                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                }
            } else {
                break;
            }
        }
        if (br.containsHTML("<title>\\s*Are You Human\\s*</title>") || br.getFormbyActionRegex("/Special/AreYouHuman.*") != null || br.toString().startsWith("Wrong answer. Click <a href='/Special/AreYouHuman")) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
    }

    private Integer[] getClickedImageLocation(final MultiClickedPoint c, final BufferedImage[] images) {
        final Integer[] results = new Integer[c.getX().length];
        // determine cords
        loop: for (int a = 0; a < results.length; a++) {
            int x = c.getX()[a];
            int i = 0;
            int min = 0;
            int max = 0;
            for (final BufferedImage bi : images) {
                // image width
                max = min + widths[i];
                if (x >= min && x <= max) {
                    // return count;
                    results[a] = i;
                    continue loop;
                }
                min = max;
                i++;
            }
        }
        return results;
    }

    private Integer getClickedImageLocation(final ClickedPoint c, final BufferedImage[] images) {
        // determine cords
        final int x = c.getX();
        int i = 0;
        int min = 0;
        int max = 0;
        for (final BufferedImage bi : images) {
            // image width
            max = min + widths[i];
            if (x >= min && x <= max) {
                // return count;
                return i;
            }
            min = max;
            i++;
        }
        return null;
    }

    private Integer[] widths = null;

    private File getStitchedImage(final String[] captchaImages, final BufferedImage[] images) throws Exception {
        int i = -1;
        for (final String ci : captchaImages) {
            // download each image
            URLConnectionAdapter con = null;
            try {
                i++;
                final Browser img = br.cloneBrowser();
                img.getHeaders().put("Accept", "image/webp,*/*;q=0.8");
                images[i] = ImageIO.read(img.openGetConnection(ci).getInputStream());
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        // display each image within single row, merged image, show in dialog with phrase
        // track only the width to determine which image has been clicked.
        widths = new Integer[images.length];
        // analyse the images
        int h = 0;
        int w = 0;
        i = 0;
        for (final BufferedImage bi : images) {
            if (h < bi.getHeight()) {
                h = bi.getHeight();
            }
            // with width we want total
            w += bi.getWidth();
            widths[i++] = bi.getWidth();
        }
        final BufferedImage stichedImageBuffer = IconIO.createEmptyImage(w, h);
        final Graphics graphic = stichedImageBuffer.getGraphics();
        w = 0;
        for (final BufferedImage bi : images) {
            graphic.drawImage(bi, w, 0, null);
            w += bi.getWidth();
        }
        final File stitchedImageOutput = getLocalCaptchaFile(".jpg");
        ImageIO.write(stichedImageBuffer, "jpg", stitchedImageOutput);
        return stitchedImageOutput;
    }

    public String refreshVideoDirectUrl(final DownloadLink dl) throws Exception {
        br = new Browser();
        String directlink = null;
        final String source_url = dl.getStringProperty("source_url", null);
        final String source_quality = dl.getStringProperty("source_quality", null);
        if (source_url == null || source_quality == null) {
            return null;
        }
        getPage(source_url);
        if (isOffline(br)) {
            return null;
        }
        handleHumanCheck(br);
        HostType hostType = HostType.parse(source_url);
        /* Find new directlink for original quality */
        final String[][] quals = getQuals(br);
        if (quals != null) {
            String skey = null;
            String iv = null;
            switch (hostType) {
            case KISS_ANIME:
                skey = getSecretKeyAnime();
                iv = "a5e8d2e9c1721ae0e84ad660c472c1f3";
                break;
            case KISS_ASIAN:
                skey = getSecretKeyAsian();
                iv = "32b812e9a1321ae0e84af660c4722b3a";
                break;
            case KISS_CARTOON:
                skey = getSecretKeyCartoon();
                iv = "a5e8d2e9c1721ae0e84ad660c472c1f3";
            case KISS_MANGA:
                skey = getSecretKeyManga();
                iv = "a5e8d2e9c1721ae0e84ad660c472c1f3";
            default:
                break;
            }
            for (final String qual[] : quals) {
                final String quality = qual[2];
                if (!quality.equalsIgnoreCase(source_quality)) {
                    continue;
                }
                switch (hostType) {
                case KISS_ANIME:
                case KISS_ASIAN:
                case KISS_CARTOON:
                    directlink = decodeSingleURL(qual[1], skey, iv);
                    break;
                default:
                    break;
                }
                break;
            }
        }
        return directlink;
    }

    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}