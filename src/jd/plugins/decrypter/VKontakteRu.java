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

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.VKontakteRuHoster;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vkontakte.ru" }, urls = { "http://(www\\.)?(vkontakte\\.ru|vk\\.com)/(audio(\\.php)?(\\?album_id=\\d+\\&id=|\\?id=)(\\-)?\\d+|(video(\\-)?\\d+_\\d+|videos\\d+|video\\?section=tagged\\&id=\\d+|video_ext\\.php\\?oid=\\d+\\&id=\\d+)|(photos|tag)\\d+|albums\\-?\\d+|([A-Za-z0-9_\\-]+#/)?album(\\-)?\\d+_\\d+|photo(\\-)?\\d+_\\d+|id\\d+(\\?z=albums\\d+)?)" }, flags = { 0 })
public class VKontakteRu extends PluginForDecrypt {

    /* must be static so all plugins share same lock */
    private static final Object LOCK = new Object();

    public VKontakteRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String FILEOFFLINE = "(id=\"msg_back_button\">Wr\\&#243;\\&#263;</button|B\\&#322;\\&#261;d dost\\&#281;pu)";
    private static final String DOMAIN      = "http://vk.com";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setReadTimeout(3 * 60 * 1000);
        String parameter = param.toString().replace("vkontakte.ru/", "vk.com/");
        br.setCookiesExclusive(false);
        synchronized (LOCK) {
            try {
                /** Login process */
                if (!getUserLogin(false)) {
                    logger.info("Logindata invalid, stopping...");
                    return decryptedLinks;
                }
                br.setFollowRedirects(true);
                br.getPage(parameter);
                /**
                 * Retry if login failed Those are 2 different errormessages but refreshing the cookies works fine for both
                 * */
                String cookie = br.getCookie("http://vk.com", "remixsid");
                if (br.containsHTML(">Security Check<") || cookie == null || "deleted".equals(cookie)) {
                    this.getPluginConfig().setProperty("logincounter", "-1");
                    this.getPluginConfig().save();
                    br.clearCookies(DOMAIN);
                    br.clearCookies("login.vk.com");
                    if (!getUserLogin(true)) {
                        logger.info("Logindata invalid/refreshing cookies failed, stopping...");
                        return null;
                    }
                    logger.info("Cookies refreshed successfully, continuing to decrypt...");
                }
                /** Decryption process START */
                br.setFollowRedirects(false);
                if (parameter.matches(".*?vk\\.com/audio.*?")) {
                    /** Audio playlists */
                    decryptedLinks = decryptAudioAlbum(decryptedLinks, parameter);
                } else if (parameter.matches(".*?vk\\.com/(video(\\-)?\\d+_\\d+|video_ext\\.php\\?oid=\\d+\\&id=\\d+)")) {
                    /** Single video */
                    decryptedLinks = decryptSingleVideo(decryptedLinks, parameter);
                } else if (parameter.matches(".*?(tag|album(\\-)?\\d+_|photos|id)\\d+")) {
                    /**
                     * Photo album Examples: http://vk.com/photos575934598 http://vk.com/id28426816 http://vk.com/album87171972_0
                     */
                    decryptedLinks = decryptPhotoAlbum(decryptedLinks, parameter, progress);
                } else if (parameter.matches(".*?vk\\.com/photo(\\-)?\\d+_\\d+")) {
                    /**
                     * Single photo links, those are just passed to the hosterplugin! Example:http://vk.com/photo125005168_269986868
                     */
                    decryptedLinks = decryptSinglePhoto(decryptedLinks, parameter);
                } else if (parameter.matches(".*?vk\\.com/(albums(\\-)?\\d+|id\\d+\\?z=albums\\d+)")) {
                    /**
                     * Photo albums lists/overviews Example: http://vk.com/albums46486585
                     */
                    decryptedLinks = decryptPhotoAlbums(decryptedLinks, parameter, progress);
                } else {
                    /**
                     * Video-Albums Example: http://vk.com/videos575934598 Example2: http://vk.com/video?section=tagged&id=46468795637
                     */
                    decryptedLinks = decryptVideoAlbum(decryptedLinks, parameter, progress);
                }
            } catch (BrowserException e) {
                logger.warning("Browser exception thrown: " + e.getMessage());
                logger.warning("Decrypter failed for link: " + parameter);
            }
        }
        if (decryptedLinks != null && decryptedLinks.size() > 0) sleep(2500l, param);
        return decryptedLinks;

    }

    private ArrayList<DownloadLink> decryptAudioAlbum(ArrayList<DownloadLink> decryptedLinks, String parameter) throws IOException {
        int overallCounter = 1;
        DecimalFormat df = new DecimalFormat("00000");
        br.getPage(parameter);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String postData = null;
        if (new Regex(parameter, "vk\\.com/audio\\?id=\\-\\d+").matches()) {
            postData = "act=load_audios_silent&al=1&edit=0&id=0&gid=" + new Regex(parameter, "(\\d+)$").getMatch(0);
        } else {
            postData = "act=load_audios_silent&al=1&edit=0&gid=0&id=" + new Regex(parameter, "(\\d+)$").getMatch(0);
        }
        br.postPage("http://vk.com/audio", postData);
        String[][] audioLinks = br.getRegex("\\'(http://cs\\d+\\.(vk\\.com|userapi\\.com)/u\\d+/audio/[a-z0-9]+\\.mp3)\\',\\'\\d+\\',\\'\\d+:\\d+\\',\\'(.*?)\\',\\'(.*?)\\'").getMatches();
        if (audioLinks == null || audioLinks.length == 0) return null;
        for (String audioInfo[] : audioLinks) {
            String finallink = audioInfo[0];
            if (finallink == null) return null;
            finallink = "directhttp://" + finallink;
            DownloadLink dl = createDownloadlink(finallink);
            // Set filename so we have nice filenames here ;)
            dl.setFinalFileName(Encoding.htmlDecode(audioInfo[2].trim()) + " - " + Encoding.htmlDecode(audioInfo[3].trim()) + ".mp3");
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            logger.info("Decrypted link number " + df.format(overallCounter) + " :" + finallink);
            overallCounter++;
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptSingleVideo(ArrayList<DownloadLink> decryptedLinks, String parameter) throws IOException {
        final Regex vids = new Regex(parameter, "/video_ext\\.php\\?oid=(\\d+)\\&id=(\\d+)");
        if (vids.getMatches().length == 1) {
            parameter = "http://vk.com/video" + vids.getMatch(0) + "_" + vids.getMatch(1);
        }
        br.getPage(parameter);
        if (br.containsHTML("(class=\"button_blue\"><button id=\"msg_back_button\">Wr\\&#243;\\&#263;</button>|<div class=\"body\">[\t\n\r ]+Access denied)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        DownloadLink finallink = findVideolink(parameter);
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter + "\n");
            return null;
        }
        decryptedLinks.add(finallink);
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptPhotoAlbum(ArrayList<DownloadLink> decryptedLinks, String parameter, ProgressController progress) throws IOException {
        final String type = "singlephotoalbum";
        if (parameter.contains("#/album"))
            parameter = "http://vk.com/album" + new Regex(parameter, "#/album((\\-)?\\d+_\\d+)").getMatch(0);
        else if (parameter.matches(".*?vk\\.com/(photos|id)\\d+")) parameter = parameter.replaceAll("vk\\.com/(photos|id)", "vk.com/album") + "_0";
        br.getPage(parameter);
        if (br.containsHTML(FILEOFFLINE) || br.containsHTML("(В альбоме нет фотографий|<title>DELETED</title>)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("There are no photos in this album")) {
            logger.info("Empty album: " + parameter);
            return decryptedLinks;
        }
        String numberOfEntrys = br.getRegex("\\| (\\d+) zdj&#281").getMatch(0);
        if (numberOfEntrys == null) {
            numberOfEntrys = br.getRegex("count: (\\d+),").getMatch(0);
            if (numberOfEntrys == null) {
                numberOfEntrys = br.getRegex("</a>(\\d+) zdj\\&#281;\\&#263;<span").getMatch(0);
            }
        }
        if (numberOfEntrys == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String[][] regexesPage1 = { { "><a href=\"/photo((\\-)?\\d+_\\d+(\\?tag=\\d+)?)\"", "0" } };
        final String[][] regexesAllOthers = { { "><a href=\"/photo((\\-)?\\d+_\\d+(\\?tag=\\d+)?)\"", "0" } };
        final ArrayList<String> decryptedData = decryptMultiplePages(parameter, type, numberOfEntrys, regexesPage1, regexesAllOthers, 80, 40, 80, parameter, "al=1&part=1&offset=");
        String albumID = new Regex(parameter, "/(album.+)").getMatch(0);
        for (String element : decryptedData) {
            if (albumID == null) albumID = "tag" + new Regex(element, "\\?tag=(\\d+)").getMatch(0);
            /** Pass those goodies over to the hosterplugin */
            DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/picturelink/" + element);
            dl.setAvailable(true);
            dl.setProperty("albumid", albumID);
            decryptedLinks.add(dl);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(parameter, "/(album|tag)(.+)").getMatch(1));
        fp.setProperty("CLEANUP_NAME", false);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptSinglePhoto(ArrayList<DownloadLink> decryptedLinks, String parameter) throws IOException {
        String albumID = br.getRegex("class=\"active_link\">[\t\n\r ]+<a href=\"/(.*?)\"").getMatch(0);
        if (albumID == null) {
            logger.warning("Decrypter broken for link: " + parameter + "\n");
            return null;
        }
        DownloadLink dl = createDownloadlink("http://vkontaktedecrypted.ru/picturelink/" + new Regex(parameter, ".*?vk\\.com/photo" + "(.+)").getMatch(0));
        dl.setProperty("albumid", albumID);
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptPhotoAlbums(ArrayList<DownloadLink> decryptedLinks, String parameter, ProgressController progress) throws IOException {
        final String type = "multiplephotoalbums";
        if (parameter.matches(".*?vk\\.com/id\\d+\\?z=albums\\d+")) parameter = "http://vk.com/albums" + new Regex(parameter, "(\\d+)$").getMatch(0);
        br.getPage(parameter);
        final String numberOfEntrys = br.getRegex("\\| (\\d+) albums?</title>").getMatch(0);
        final String startOffset = br.getRegex("var preload = \\[(\\d+),\"").getMatch(0);
        if (numberOfEntrys == null || startOffset == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        /** Photos are placed in different locations, find them all */
        final String[][] regexesPage1 = { { "class=\"photo_row\" id=\"(tag\\d+|album(\\-)?\\d+_\\d+)", "0" } };
        final String[][] regexesAllOthers = { { "class=\"photo(_album)?_row\" id=\"(tag\\d+|album(\\-)?\\d+_\\d+)", "1" } };
        final ArrayList<String> decryptedData = decryptMultiplePages(parameter, type, numberOfEntrys, regexesPage1, regexesAllOthers, Integer.parseInt(startOffset), 12, 18, parameter, "al=1&part=1&offset=");
        for (String element : decryptedData) {
            final String decryptedLink = "http://vk.com/" + element;
            decryptedLinks.add(createDownloadlink(decryptedLink));
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> decryptVideoAlbum(ArrayList<DownloadLink> decryptedLinks, String parameter, ProgressController progress) throws IOException {
        final String type = "multiplevideoalbums";
        br.getPage(parameter);
        final String numberOfEntrys = br.getRegex("(\\d+) videos<").getMatch(0);
        if (numberOfEntrys == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        // Regexes 1 + 2 are for stuff from the first page, rest is for pages
        // 2-end
        final String[][] regexesPage1 = { { "<td class=\"video_thumb\"><a href=\"/video(\\d+_\\d+)\"", "0" } };
        final String[][] regexesAllOthers = { { "\\[(\\d+, \\d+), \\'", "0" } };
        final String oid = new Regex(parameter, "(\\d+)$").getMatch(0);
        final ArrayList<String> decryptedData = decryptMultiplePages(parameter, type, numberOfEntrys, regexesPage1, regexesAllOthers, 12, 12, 40, "http://vk.com/al_video.php", "act=load_videos_silent&al=1&oid=" + oid + "&offset=");
        int counter = 1;
        for (String singleVideo : decryptedData) {
            singleVideo = singleVideo.replace(", ", "_");
            logger.info("Decrypting video " + counter + " / " + numberOfEntrys);
            String completeVideolink = "http://vk.com/video" + singleVideo;
            br.getPage(completeVideolink);
            // Invalid link
            if (br.containsHTML(FILEOFFLINE)) {
                logger.info("Link offline: " + completeVideolink);
                continue;
            }
            DownloadLink finallink = findVideolink(completeVideolink);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter + "\n");
                logger.warning("stopped at: " + completeVideolink);
                return null;
            }
            decryptedLinks.add(finallink);
            counter++;
        }
        return decryptedLinks;
    }

    private DownloadLink findVideolink(String parameter) throws IOException {
        String correctedBR = br.toString().replace("\\", "");
        // Find youtube.com link if it exists
        String embeddedVideo = new Regex(correctedBR, "youtube\\.com/embed/(.*?)\\?autoplay=").getMatch(0);
        if (embeddedVideo != null) { return createDownloadlink("http://www.youtube.com/watch?v=" + embeddedVideo); }
        // Find rutube.ru link if it exists
        embeddedVideo = new Regex(correctedBR, "video\\.rutube\\.ru/(.*?)\\'").getMatch(0);
        if (embeddedVideo != null) {
            br.getPage("http://rutube.ru/trackinfo/" + embeddedVideo + ".html");
            String finalID = br.getRegex("<track_id>(\\d+)</track_id>").getMatch(0);
            if (finalID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            return createDownloadlink("http://rutube.ru/tracks/" + finalID + ".html");
        }
        // Find vimeo.com link if it exists
        embeddedVideo = new Regex(correctedBR, "player\\.vimeo\\.com/video/(\\d+)").getMatch(0);
        if (embeddedVideo != null) { return createDownloadlink("http://vimeo.com/" + embeddedVideo); }
        embeddedVideo = new Regex(correctedBR, "pdj\\.com/i/swf/og\\.swf\\?jsonURL=(http[^<>\"]*?)\\'").getMatch(0);
        if (embeddedVideo != null) {
            br.getPage(Encoding.htmlDecode(embeddedVideo));
            correctedBR = br.toString().replace("\\", "");
            embeddedVideo = new Regex(correctedBR, "@download_url\":\"(http://promodj\\.com/[^<>\"]*?)\"").getMatch(0);
            if (embeddedVideo == null) return null;
            return createDownloadlink(Encoding.htmlDecode(embeddedVideo));
        }
        embeddedVideo = new Regex(correctedBR, "url: \\'(http://player\\.digitalaccess\\.ru/[^<>\"/]*?\\&siteId=\\d+)\\&").getMatch(0);
        if (embeddedVideo != null) { return createDownloadlink("directhttp://" + Encoding.htmlDecode(embeddedVideo)); }
        // No external video found, try finding a hosted video
        String additionalStuff = "video/";
        String urlPart = new Regex(correctedBR, "\"thumb\":\"(http:.{10,100})/video").getMatch(0);
        if (urlPart == null) urlPart = new Regex(correctedBR, "\"host\":\"(.*?)\"").getMatch(0);
        String vtag = new Regex(correctedBR, "\"vtag\":\"(.*?)\"").getMatch(0);
        String videoID = new Regex(correctedBR, "\"vkid\":\"(.*?)\"").getMatch(0);
        if (videoID == null) videoID = new Regex(parameter, ".*?vk\\.com/video(\\-)?\\d+_(\\d+)").getMatch(1);
        if (videoID == null || urlPart == null || vtag == null) return null;
        /**
         * Find the highest possible quality, also every video is only available in 1-2 formats so we HAVE to use the highest one, if we don't do that we get
         * wrong links
         */
        String quality = ".vk.flv";
        if (correctedBR.contains("\"hd\":1")) {
            quality = ".360.mov";
            videoID = "";
        } else if (correctedBR.contains("\"hd\":2")) {
            quality = ".480.mov";
            videoID = "";
        } else if (correctedBR.contains("\"hd\":3")) {
            quality = ".720.mov";
            videoID = "";
        } else if (correctedBR.contains("\"no_flv\":1")) {
            quality = ".240.mov";
            videoID = "";
        }
        if (correctedBR.contains("\"hd\":3") && correctedBR.contains("\"no_flv\":0")) {
            quality = ".720.mp4";
            videoID = "";
        } else if (correctedBR.contains("\"no_flv\":0")) {
            /** Last big change done here on 07.11.2011 */
            if (urlPart.contains("vkadre.ru")) {
                additionalStuff = "assets/videos/";
            } else {
                quality = ".flv";
                videoID = "";
            }
        }
        String videoName = new Regex(correctedBR, "\"md_title\":\"(.*?)\"").getMatch(0);
        if (videoName == null) {
            videoName = new Regex(correctedBR, "\\{\"title\":\"(.*?)\"").getMatch(0);
        }
        String completeLink = "directhttp://http://" + urlPart.replace("http://", "") + "/" + additionalStuff + vtag + videoID + quality;
        DownloadLink dl = createDownloadlink(completeLink);
        // Set filename so we have nice filenames here ;)
        if (videoName != null) {
            if (videoName.length() > 100) {
                videoName = videoName.substring(0, 100);
            }
            dl.setFinalFileName(Encoding.htmlDecode(videoName).replaceAll("(»|\")", "").trim() + quality.substring(quality.length() - 4, quality.length()));
        }
        return dl;
    }

    private ArrayList<String> decryptMultiplePages(final String parameter, final String type, final String numberOfEntries, final String[][] regexesPageOne, final String[][] regexesAllOthers, int offset, final int increase, int alreadyOnPage, final String postPage, final String postData) throws IOException {
        ArrayList<String> decryptedData = new ArrayList<String>();
        logger.info("Decrypting " + numberOfEntries + "entries for linktype: " + type);
        int maxLoops = (int) StrictMath.ceil((Double.parseDouble(numberOfEntries) - alreadyOnPage) / increase);
        if (maxLoops < 0) maxLoops = 0;
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int addedLinks = 0;
        for (int i = 0; i <= maxLoops; i++) {
            if (i > 0) {
                br.postPage(postPage, postData + offset);
                for (String regex[] : regexesAllOthers) {
                    String correctedBR = br.toString().replace("\\", "");
                    String[] theData = new Regex(correctedBR, regex[0]).getColumn(Integer.parseInt(regex[1]));
                    if (theData == null || theData.length == 0) {
                        addedLinks = 0;
                        break;
                    }
                    addedLinks = theData.length;
                    for (String data : theData) {
                        decryptedData.add(data);
                    }
                }
                offset += increase;
            } else {
                for (String regex[] : regexesPageOne) {
                    String correctedBR = br.toString().replace("\\", "");
                    String[] theData = new Regex(correctedBR, regex[0]).getColumn(Integer.parseInt(regex[1]));
                    if (theData == null || theData.length == 0) {
                        addedLinks = 0;
                        break;
                    }
                    addedLinks = theData.length;
                    for (String data : theData) {
                        decryptedData.add(data);
                    }
                }
            }
            if (addedLinks < increase) {
                logger.info("Fail safe activated, stopping page parsing at page " + i + " of " + maxLoops);
                break;
            }
            logger.info("Parsing page " + i + " of " + maxLoops);
        }
        if (decryptedData == null || decryptedData.size() == 0) {
            logger.warning("Decrypter couldn't find theData for linktype: " + type + "\n");
            logger.warning("Decrypter broken for link: " + parameter + "\n");
            return null;
        }
        logger.info("Found " + decryptedData.size() + " links for linktype: " + type);

        return decryptedData;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost vkPlugin = JDUtilities.getPluginForHost("vkontakte.ru");
        Account aa = AccountController.getInstance().getValidAccount(vkPlugin);
        if (aa == null) {
            String username = UserIO.getInstance().requestInputDialog("Enter Loginname for vkontakte.ru :");
            if (username == null) throw new DecrypterException("Username not entered!");
            String password = UserIO.getInstance().requestInputDialog("Enter password for vkontakte.ru :");
            if (password == null) throw new DecrypterException("Password not entered!");
            aa = new Account(username, password);
        }
        try {
            ((VKontakteRuHoster) vkPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setEnabled(false);
            aa.setValid(false);
            return false;
        }
        // Account is valid, let's just add it
        AccountController.getInstance().addAccount(vkPlugin, aa);
        return true;
    }

}
