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

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.controlling.ProgressControllerListener;
import jd.gui.UserIO;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vkontakte.ru" }, urls = { "http://(www\\.)?(vkontakte\\.ru|vk\\.com)/(audio\\.php\\?id=\\d+|video(-)?\\d+_\\d+|videos\\d+)" }, flags = { 0 })
public class VKontakteRu extends PluginForDecrypt implements ProgressControllerListener {

    /* must be static so all plugins share same lock */
    private static final Object LOCK  = new Object();
    private boolean             abort = false;

    public VKontakteRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String POSTPAGE = "http://vkontakte.ru/login.php";
    private static final String DOMAIN   = "vkontakte.ru";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        try {
            progress.getBroadcaster().addListener(this);
        } catch (Throwable e) {
            /* stable does not have appwork utils yet */
        }
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString().replace("vk.com/", "vkontakte.ru/");
        br.setCookiesExclusive(true);
        synchronized (LOCK) {
            if (!getUserLogin()) return null;
            br.setFollowRedirects(false);
            // Access the page
            br.getPage(parameter);
            if (parameter.matches(".*?vkontakte\\.ru/audio\\.php\\?id=\\d+")) {
                String[] audioLinks = br.getRegex("(class=\"playimg\" onclick=\"return operate\\(\\'\\d+_\\d+\\',\\'http://.*?class=\"duration\">)").getColumn(0);
                if (audioLinks == null || audioLinks.length == 0) audioLinks = br.getRegex("\\'(http://cs\\d+\\.vkontakte\\.ru/u\\d+/audio/[a-z0-9]+\\.mp3)\\'").getColumn(0);
                if (audioLinks == null || audioLinks.length == 0) return null;
                for (String audioInfo : audioLinks) {
                    String finallink = null;
                    if (audioInfo.startsWith("http"))
                        finallink = audioInfo;
                    else
                        finallink = new Regex(audioInfo, "return operate\\(\\'\\d+_\\d+\\',\\'(http://.*?)\\'").getMatch(0);
                    if (finallink == null) return null;
                    Regex artistAndName = new Regex(audioInfo, "id=\"performer\\d+_\\d+\"><a href=\\'gsearch\\.php\\?section=audio\\&c\\[q\\]=(.*?)\\'>(.*?)</a></b><span>\\&nbsp;-\\&nbsp;</span><span id=\"title\\d+_\\d+\">(<a href=\"\" onclick=\"showLyrics\\(\\'\\d+_\\d+\\',\\d+\\);return false;\">)?(.*?)(</a>)?</span> </div>");
                    String artist = artistAndName.getMatch(0);
                    if (artist == null) artist = artistAndName.getMatch(1);
                    String name = artistAndName.getMatch(3);
                    DownloadLink dl = createDownloadlink("directhttp://" + finallink);
                    // Set filename so we have nice filenames here ;)
                    if (artist != null && name != null) dl.setFinalFileName(artist + " - " + name + ".mp3");
                    decryptedLinks.add(dl);
                }
            } else if (parameter.matches(".*?vkontakte\\.ru/video(-)?\\d+_\\d+")) {
                DownloadLink finallink = findVideolink(parameter);
                if (finallink == null) return null;
                decryptedLinks.add(finallink);
            } else {
                // Video-Playlists
                String[] allVideos = br.getRegex("<div class=\"video_name\"><a href=\"(video.*?)\"").getColumn(0);
                if (allVideos == null || allVideos.length == 0) allVideos = br.getRegex("style=\"position:relative;\">[\t\n\r ]+<a href=\"(video.*?)\"").getColumn(0);
                if (allVideos == null || allVideos.length == 0) {
                    logger.warning("Couldn't find any videos for link: " + parameter);
                    return null;
                }
                progress.setRange(allVideos.length);
                int counter = 1;
                logger.info("Found " + allVideos.length + " videos, decrypting...");
                for (String singleVideo : allVideos) {
                    if (abort) {
                        logger.info("Decrypt process stopped at video " + counter + " / " + allVideos.length);
                        progress.setColor(Color.RED);
                        progress.setStatusText(progress.getStatusText() + ": " + JDL.L("gui.linkgrabber.aborted", "Aborted"));
                        progress.doFinalize(5000l);
                        return new ArrayList<DownloadLink>();
                    }
                    logger.info("Decrypting video " + counter + " / " + allVideos.length);
                    String completeVideolink = "http://vkontakte.ru/" + singleVideo;
                    br.getPage(completeVideolink);
                    DownloadLink finallink = findVideolink(completeVideolink);
                    if (finallink == null) {
                        logger.warning("Videolink is null...");
                        return null;
                    }
                    decryptedLinks.add(finallink);
                    progress.increase(1);
                    counter++;
                }

            }
            return decryptedLinks;
        }

    }

    private DownloadLink findVideolink(String parameter) throws IOException {
        String videoEmbedded = new Regex(br.toString().replace("\\", ""), "youtube\\.com\\/embed\\/(.*?)\\?autoplay=").getMatch(0);
        if (videoEmbedded != null) {
            return createDownloadlink("http://www.youtube.com/watch?v=" + videoEmbedded);
        } else {
            String additionalStuff = "videos/";
            String urlPart = br.getRegex("playerContainerHTML\\(\\'(http://.*?)thumbnails/").getMatch(0);
            if (urlPart == null) {
                urlPart = br.getRegex("playerContainerHTML\\(\\'(http://.*?/video/)").getMatch(0);
                additionalStuff = "";
            }
            String vtag = br.getRegex("\"vtag\":\"(.*?)\"").getMatch(0);
            String videoID = br.getRegex("\"vkid\":\"(.*?)\"").getMatch(0);
            if (videoID == null) videoID = new Regex(parameter, ".*?vkontakte\\.ru/video(\\-)?\\d+_(\\d+)").getMatch(1);
            if (videoID == null || urlPart == null || vtag == null) return null;
            // Find the highest possible quality, also every video is only
            // available
            // in 1-2 formats so we HAVE to use the highest one, if we don't do
            // that
            // we get wrong lings
            String quality = ".vk.flv";
            if (br.containsHTML("\"hd\":1")) {
                quality = ".360.mp4";
                videoID = "";
            } else if (br.containsHTML("\"hd\":2")) {
                quality = ".480.mp4";
                videoID = "";
            } else if (br.containsHTML("\"hd\":3")) {
                quality = ".720.mp4";
                videoID = "";
            } else if (br.containsHTML("\"no_flv\":1")) {
                quality = ".240.mp4";
                videoID = "";
            }
            if (br.containsHTML("\"no_flv\":0")) {
                videoID = "";
                quality = ".flv";
            }
            String videoName = br.getRegex(">Pliki Video</a>(.*?)</h1></div>").getMatch(0);
            String completeLink = "directhttp://" + urlPart + additionalStuff + vtag + videoID + quality;
            DownloadLink dl = createDownloadlink(completeLink);
            // Set filename so we have nice filenames here ;)
            if (videoName != null) dl.setFinalFileName(Encoding.htmlDecode(videoName).replaceAll("(»|\")", "").trim() + quality.substring(quality.length() - 4, quality.length()));
            return dl;
        }
    }

    private boolean getUserLogin() throws IOException, DecrypterException {
        br.setFollowRedirects(true);
        String username = null;
        String password = null;
        synchronized (LOCK) {
            username = this.getPluginConfig().getStringProperty("user", null);
            password = this.getPluginConfig().getStringProperty("pass", null);
            for (int i = 0; i < 3; i++) {
                if ((username == null && password == null) || !loginSite(username, password) || br.getCookie(POSTPAGE, "remixsid") == null) {
                    this.getPluginConfig().setProperty("user", Property.NULL);
                    this.getPluginConfig().setProperty("pass", Property.NULL);
                    username = UserIO.getInstance().requestInputDialog("Enter Loginname for " + DOMAIN + " :");
                    if (username == null) return false;
                    password = UserIO.getInstance().requestInputDialog("Enter password for " + DOMAIN + " :");
                    if (password == null) return false;
                    if (!loginSite(username, password)) break;
                } else {
                    this.getPluginConfig().setProperty("user", username);
                    this.getPluginConfig().setProperty("pass", password);
                    this.getPluginConfig().save();
                    return true;
                }

            }
        }
        throw new DecrypterException("Login or/and password for " + DOMAIN + " is wrong!");
    }

    private boolean loginSite(String username, String password) throws IOException {
        br.postPage(POSTPAGE, "act=login&success_url=&fail_url=&try_to_login=1&to=&vk=&al_test=3&email=" + Encoding.urlEncode(username) + "&pass=" + Encoding.urlEncode(password) + "&expire=");
        String hash = br.getRegex("type=\"hidden\" name=\"hash\" value=\"(.*?)\"").getMatch(0);
        // If this variable is null the login is probably wrong
        if (hash == null) return false;
        br.getPage("http://vkontakte.ru/login.php?act=slogin&fast=1&hash=" + hash + "&redirect=1&s=0");
        return true;
    }

    public void onProgressControllerEvent(ProgressControllerEvent event) {
        if (event.getID() == ProgressControllerEvent.CANCEL) {
            abort = true;
        }
    }

}
