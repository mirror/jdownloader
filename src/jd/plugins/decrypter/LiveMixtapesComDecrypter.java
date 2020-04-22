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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livemixtapes.com" }, urls = { "https?://(?:www\\.)?livemixtap\\.es/[a-z0-9]+|https?://(\\w+\\.)?livemixtapes\\.com/((download(/mp3)?|mixtapes)/\\d+/[a-z0-9\\-]+\\.html|player\\.php\\?album_id=\\d+.*?)" })
public class LiveMixtapesComDecrypter extends antiDDoSForDecrypt {
    public LiveMixtapesComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String REDIRECTLINK           = "https?://(www\\.)?livemixtap\\.es/[a-z0-9]+";
    private static final String MUSTBELOGGEDIN         = ">You must be logged in to access this page";
    private static final String ONLYREGISTEREDUSERTEXT = "Download is only available for registered users";
    private static final Object LOCK                   = new Object();
    public static final String  TYPE_DOWNLOAD          = "https?://(?:\\w+\\.)?livemixtapes\\.com/download/.+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(TYPE_DOWNLOAD)) {
            /* Add URL --> Host plugin */
            decryptedLinks.add(this.createDownloadlink(parameter));
            return decryptedLinks;
        }
        /* 2020-04-22: Convert embed URLs --> Normal URLs */
        final UrlQuery query = new UrlQuery().parse(parameter);
        final String album_id = query.get("album_id");
        if (album_id != null) {
            parameter = String.format("https://www.%s/mixtapes/%s/.html", this.getHost(), album_id);
        }
        br.getHeaders().put("Accept-Encoding", "gzip,deflate");
        synchronized (LOCK) {
            /* 2020-04-22: Save- and restore cookies to avoid captchas */
            try {
                final Object cookiesO = this.getPluginConfig().getProperty("cookies");
                final Cookies cookies = (Cookies) cookiesO;
                br.setCookies(this.getHost(), cookies);
            } catch (final Throwable e) {
            }
            /** If link is a short link correct it */
            if (parameter.matches(REDIRECTLINK)) {
                br.setFollowRedirects(false);
                getPage(parameter);
                String correctLink = br.getRedirectLocation();
                if (correctLink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                getPage(correctLink);
                correctLink = br.getRedirectLocation();
                if (correctLink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                parameter = correctLink;
                br.setFollowRedirects(true);
            }
            getUserLogin();
            br.setFollowRedirects(true);
            getPage(parameter);
            if (CaptchaHelperCrawlerPluginRecaptchaV2.containsRecaptchaV2Class(br)) {
                final Form captchaform = br.getForm(0);
                if (captchaform == null) {
                    logger.warning("Failed to find captchaform");
                    return null;
                }
                captchaform.put("g-recaptcha-response", Encoding.urlEncode(new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken()));
                submitForm(captchaform);
            }
            this.getPluginConfig().setProperty("cookies", br.getCookies(br.getHost()));
        }
        if (br.getURL().contains("error/login.html")) {
            logger.info("Login needed to decrypt link: " + parameter);
            return decryptedLinks;
        }
        /* Check for (external) embedded video(s) e.g. instagram */
        if (br.containsHTML("function videoEmbed")) {
            final String finallink = br.getRegex("videoEmbed\\(\\'(https?://[^<>\"]*?)\\'").getMatch(0);
            if (finallink != null) {
                logger.info("Found embedded content");
                decryptedLinks.add(createDownloadlink(finallink));
                /* 2020-04-22: Do not stop here anymore. */
                // return decryptedLinks;
            }
        }
        String fpName = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)").getMatch(0);
        if (fpName == null) {
            /* Fallback attempt */
            fpName = new Regex(br.getURL(), "mixtapes/\\d+/(.*?)\\.html$").getMatch(0);
            if (fpName != null) {
                fpName = fpName.replace("-", " ");
            }
        }
        FilePackage fp = null;
        if (fpName != null) {
            fpName = Encoding.htmlDecode(fpName);
            fp = FilePackage.getInstance();
            fp.setName(fpName);
        }
        /* 2020-04-22: New */
        final String[] trackInfo = br.getRegex("<li class=\"[^\"]*?track-streamable[^\"]*?\"(.*?)showPlaylistPopup").getColumn(0);
        final String[] songIDs = br.getRegex("onclick=\"showPlaylistPopup\\((\\d+)\\)\"").getColumn(0);
        final boolean hasAdditionalInfo = trackInfo.length == songIDs.length;
        for (int i = 0; i < songIDs.length; i++) {
            final String songID = songIDs[i];
            final DownloadLink dl = this.createDownloadlink("https://club.livemixtapes.com/play/" + songID);
            String filename = null;
            if (hasAdditionalInfo) {
                final String src = trackInfo[i];
                final String tracknum = new Regex(src, "class=\"track-num\">(\\d+)").getMatch(0);
                String trackname = new Regex(src, "<h3[^>]*?>(.*?)(\\s+\\(\\d+:\\d{1,2}\\s*\\)\\s*)?<").getMatch(0);
                if (tracknum != null && trackname != null) {
                    trackname = Encoding.htmlDecode(trackname);
                    trackname = trackname.trim();
                    filename = tracknum + "." + trackname;
                }
            }
            if (filename == null) {
                /* Fallback */
                filename = songID;
            }
            if (!filename.endsWith(".mp3")) {
                filename += ".mp3";
            }
            dl.setFinalFileName(filename);
            dl.setAvailable(true);
            if (fp != null) {
                dl._setFilePackage(fp);
            }
            decryptedLinks.add(dl);
        }
        if (decryptedLinks.size() > 0) {
            return decryptedLinks;
        }
        /* TODO: Fix official download handling e.g. */
        final DownloadLink mainlink = createDownloadlink(parameter.replace("livemixtapes.com/", "livemixtapesdecrypted.com/"));
        String filename = null, filesize = null;
        if (br.containsHTML(MUSTBELOGGEDIN)) {
            final Regex fileInfo = br.getRegex("<td height=\"35\"><div style=\"padding\\-left: 8px\">([^<>\"]*?)</div></td>[\t\n\r ]+<td align=\"center\">([^<>\"]*?)</td>");
            filename = fileInfo.getMatch(0);
            filesize = fileInfo.getMatch(1);
            if (filename == null || filesize == null) {
                // mainlink.getLinkStatus().setStatusText(ONLYREGISTEREDUSERTEXT);
                mainlink.setAvailable(true);
            }
        } else {
            final String timeRemaining = br.getRegex("TimeRemaining = (\\d+);").getMatch(0);
            if (timeRemaining != null) {
                // mainlink.getLinkStatus().setStatusText("Not yet released, cannot download");
                mainlink.setName(Encoding.htmlDecode(br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0)));
                mainlink.setAvailable(true);
                decryptedLinks.add(mainlink);
                return decryptedLinks;
            }
            final Regex fileInfo = br.getRegex("<td height=\"35\"><div[^>]+>(.*?)</div></td>[\t\n\r ]+<td align=\"center\">((\\d+(\\.\\d+)? ?(KB|MB|GB)))</td>");
            filename = fileInfo.getMatch(0);
            filesize = fileInfo.getMatch(1);
        }
        if (filename == null || filesize == null) {
            mainlink.setAvailable(false);
        } else {
            mainlink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            mainlink.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        decryptedLinks.add(mainlink);
        return decryptedLinks;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private boolean getUserLogin() throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("livemixtapes.com");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.info("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.LiveMixTapesCom) hostPlugin).login(this.br, aa);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }
}
