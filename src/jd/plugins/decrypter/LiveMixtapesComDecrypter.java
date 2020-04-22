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

    private static final String REDIRECTLINK           = "https?://(?:www\\.)?livemixtap\\.es/[a-z0-9]+";
    private static final String MUSTBELOGGEDIN         = ">You must be logged in to access this page";
    private static final String ONLYREGISTEREDUSERTEXT = "Download is only available for registered users";
    private static final Object LOCK                   = new Object();
    public static final String  TYPE_DOWNLOAD          = "https?://(?:\\w+\\.)?livemixtapes\\.com/download/.+";

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-04-22: Preventive measure to try to avoid captchas */
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(TYPE_DOWNLOAD)) {
            /* --> Host plugin */
            decryptedLinks.add(this.createDownloadlink(parameter));
            return decryptedLinks;
        }
        /* 2020-04-22: Convert embed URLs --> Normal URLs */
        final UrlQuery query = new UrlQuery().parse(parameter);
        String album_id = query.get("album_id");
        if (album_id == null) {
            album_id = new Regex(parameter, "/mixtapes/(\\d+)").getMatch(0);
        } else {
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
                /* TODO: Check if a captcha can happen here as well. */
                br.setFollowRedirects(false);
                getPage(parameter);
                String redirect = br.getRedirectLocation();
                if (redirect == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                getPage(redirect);
                redirect = br.getRedirectLocation();
                if (redirect == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                } else if (redirect.matches(TYPE_DOWNLOAD)) {
                    logger.warning("WTF final downloadurl might be the same as the one which was initially added: " + parameter);
                    return null;
                }
                decryptedLinks.add(this.createDownloadlink(redirect));
                /* Redirect will most likely go back into this crawler. */
                return decryptedLinks;
            }
            if (album_id == null) {
                /* This should never happen */
                logger.warning("album_id is null");
                return null;
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
        if (fpName == null) {
            /* Final fallback */
            fpName = album_id;
        }
        fpName = Encoding.htmlDecode(fpName).trim();
        FilePackage fpStream = FilePackage.getInstance();
        fpStream.setName(fpName + " - stream");
        FilePackage fpDownload = FilePackage.getInstance();
        fpDownload.setName(fpName + " - download");
        /* 2020-04-22: New */
        /* TODO: Maybe switch to "API" for future bugfixes e.g. https://www.livemixtapes.com/playlist.json.php?playlist_id=123456 */
        final String[] trackInfo = br.getRegex("<li class=\"[^\"]*?track-streamable[^\"]*?\"(.*?)showPlaylistPopup").getColumn(0);
        final String[] songIDs = br.getRegex("onclick=\"showPlaylistPopup\\((\\d+)\\)\"").getColumn(0);
        final boolean hasAdditionalInfo = trackInfo.length == songIDs.length;
        for (int i = 0; i < songIDs.length; i++) {
            final String songID = songIDs[i];
            String officialDownloadURL = null;
            final DownloadLink dl = this.createDownloadlink(String.format("https://club.livemixtapes.com/play/%s", songID));
            String filename = null;
            if (hasAdditionalInfo) {
                final String src = trackInfo[i];
                final String tracknum = new Regex(src, "class=\"track-num\">\\s*?(\\d+)").getMatch(0);
                String trackname = new Regex(src, "<h3[^>]*?>(.*?)(\\s+\\(\\d+:\\d{1,2}\\s*\\)\\s*)?<").getMatch(0);
                if (tracknum != null && trackname != null) {
                    trackname = Encoding.htmlDecode(trackname);
                    trackname = trackname.trim();
                    filename = tracknum + ". " + trackname;
                }
                officialDownloadURL = new Regex(src, "(/download/mp3/" + songID + "/[a-z0-9\\-]+\\.html)").getMatch(0);
            }
            if (filename != null) {
                if (!filename.endsWith(".mp3")) {
                    filename += ".mp3";
                }
                dl.setFinalFileName(filename);
            } else {
                dl.setName(songID + ".mp3");
            }
            dl.setAvailable(true);
            dl._setFilePackage(fpStream);
            decryptedLinks.add(dl);
            if (officialDownloadURL != null) {
                officialDownloadURL = "https://www." + this.getHost() + officialDownloadURL;
                final DownloadLink dlDownload = this.createDownloadlink(officialDownloadURL);
                if (filename != null) {
                    dlDownload.setFinalFileName(filename);
                } else {
                    dlDownload.setName(songID + ".mp3");
                }
                dlDownload.setAvailable(true);
                dlDownload._setFilePackage(fpDownload);
                decryptedLinks.add(dlDownload);
            }
        }
        /* Add .zip with complete album download */
        String officialAlbumDownloadLink = br.getRegex("(/download/" + album_id + "/[a-z0-9\\-]+\\.html)").getMatch(0);
        if (officialAlbumDownloadLink != null) {
            final DownloadLink album = this.createDownloadlink("https://www." + this.getHost() + officialAlbumDownloadLink);
            album.setName(fpName + ".zip");
            album.setAvailable(true);
            decryptedLinks.add(album);
        }
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
        hostPlugin.setBrowser(this.br);
        try {
            ((jd.plugins.hoster.LiveMixTapesCom) hostPlugin).login(aa);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }
}
