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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundcloud.com" }, urls = { "https?://((www\\.|m\\.)?(soundcloud\\.com/[^<>\"\\']+(\\?format=html\\&page=\\d+|\\?page=\\d+)?|snd\\.sc/[A-Za-z09]+)|api\\.soundcloud\\.com/tracks/\\d+|api\\.soundcloud\\.com/playlists/\\d+\\?secret_token=[A-Za-z0-9\\-_]+)" }, flags = { 0 })
public class SoundCloudComDecrypter extends PluginForDecrypt {

    public SoundCloudComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String CLIENTID           = "b45b1aa10f1ac2941910a7f0d10f8e28";
    private static final String INVALIDLINKS       = "https?://(www\\.)?soundcloud\\.com/(you/|tour|signup|logout|login|premium|messages|settings|imprint|community\\-guidelines|videos|terms\\-of\\-use|sounds|jobs|press|mobile|#?search|upload|people|dashboard|#).*?";
    private static final String PLAYLISTAPILINK    = "https?://(www\\.|m\\.)?api\\.soundcloud\\.com/playlists/\\d+\\?secret_token=[A-Za-z0-9\\-_]+";

    private static final String GRAB500THUMB       = "GRAB500THUMB";
    private static final String CUSTOM_PACKAGENAME = "CUSTOM_PACKAGENAME";
    private static final String CUSTOM_DATE        = "CUSTOM_DATE";

    private PluginForHost       HOSTPLUGIN         = null;
    private SubConfiguration    CFG                = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        CFG = SubConfiguration.getConfig("soundcloud.com");
        final boolean decryptThumb = CFG.getBooleanProperty(GRAB500THUMB, false);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // Sometimes slow servers
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        try {
            // They can have huge pages, allow eight times the normal load limit
            br.setLoadLimit(8388608);
        } catch (final Throwable e) {
            // Not available in old 0.9.581 Stable
        }
        // Login if possible, helps to get links which need the user to be logged in
        HOSTPLUGIN = JDUtilities.getPluginForHost("soundcloud.com");
        final Account aa = AccountController.getInstance().getValidAccount(HOSTPLUGIN);
        if (aa != null) {
            try {
                ((jd.plugins.hoster.SoundcloudCom) HOSTPLUGIN).login(this.br, aa, false);
            } catch (final PluginException e) {
            }
        }

        String parameter = param.toString().replace("http://", "https://").replaceAll("(/download|\\\\)", "").replaceFirst("://(www|m)\\.", "://");
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Invalid link: " + parameter);
            return decryptedLinks;
        }

        // Correct links
        if (parameter.matches("http://(www\\.)?snd\\.sc/[A-Za-z09]+")) {
            br.setFollowRedirects(false);
            br.getPage(parameter);
            final String newparameter = br.getRedirectLocation();
            if (newparameter == null) {
                logger.warning("Decrypter failed on redirect link: " + parameter);
                return null;
            }
            parameter = newparameter;
        } else if (parameter.matches("https?://api\\.soundcloud\\.com/tracks/\\d+")) {
            br.getPage("https://api.soundcloud.com/tracks/" + new Regex(parameter, "(\\d+)$").getMatch(0) + "?client_id=" + SoundCloudComDecrypter.CLIENTID + "&format=json");
            if (br.containsHTML("\"error_message\":\"404 \\- Not Found\"")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String newparameter = br.getRegex("\"permalink_url\":\"(http://soundcloud\\.com/[a-z0-9\\-_]+/[a-z0-9\\-_]+(/[a-z0-9\\-_]+)?)\"").getMatch(0);
            if (newparameter == null) {
                logger.warning("Decrypter failed on redirect link: " + parameter);
                return null;
            }
            newparameter = newparameter.replace("http://", "https://");
            parameter = newparameter;
        } else if (parameter.matches(PLAYLISTAPILINK)) {
            final Regex info = new Regex(parameter, "api\\.soundcloud\\.com/playlists/(\\d+)\\?secret_token=([A-Za-z0-9\\-_]+)");
            br.getPage("https://api.sndcdn.com/playlists/" + info.getMatch(0) + "/?representation=compact&secret_token=" + info.getMatch(1) + "&client_id=" + SoundCloudComDecrypter.CLIENTID + "&format=json");
            String newparameter = br.getRegex("\"permalink_url\":\"(http://(www\\.)?soundcloud\\.com/[^<>\"/]*?/sets/[^<>\"]*?)\"").getMatch(0);
            if (newparameter == null) {
                logger.warning("Decrypter failed on redirect link: " + parameter);
                return null;
            }
            newparameter = newparameter.replace("http://", "https://");
            parameter = newparameter;
        }

        // Decrypt links
        br.setFollowRedirects(true);
        boolean decryptList = parameter.matches(".*?soundcloud\\.com/[a-z\\-_0-9]+/(tracks|favorites)(\\?page=\\d+)?");
        if (!decryptList) {
            decryptList = !parameter.matches(".*?soundcloud\\.com/[A-Za-z\\-_0-9]+/([A-Za-z\\-_0-9]+/([A-Za-z\\-_0-9]+)?|[A-Za-z\\-_0-9]+/?)");
            if (!decryptList) decryptList = (parameter.contains("/groups/") || parameter.contains("/sets/"));
        }
        if (decryptList) {
            final String clientID = jd.plugins.hoster.SoundcloudCom.CLIENTID;
            br.getPage("https://api.sndcdn.com/resolve?url=" + Encoding.urlEncode(parameter) + "&_status_code_map%5B302%5D=200&_status_format=json&client_id=" + clientID);
            if (br.containsHTML("\"404 \\- Not Found\"")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML("<duration type=\"integer\">0</duration>")) {
                logger.info("Link offline (empty set-link (playlist)): " + parameter);
                return decryptedLinks;
            }
            String username = null;
            String playlistname = null;
            // For sets ("/set/" links)
            if (parameter.contains("/sets/")) {
                playlistname = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
                if (playlistname == null) playlistname = new Regex(parameter, "/sets/(.+)$").getMatch(0);
                username = (((jd.plugins.hoster.SoundcloudCom) HOSTPLUGIN).getXML("username", br.toString()));
                final String[] items = br.getRegex("<track>(.*?)</track>").getColumn(0);
                final String usernameOfSet = new Regex(parameter, "soundcloud\\.com/(.*?)/sets/").getMatch(0);
                if (items == null || items.length == 0 || usernameOfSet == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String item : items) {
                    final String permalink = ((jd.plugins.hoster.SoundcloudCom) HOSTPLUGIN).getXML("permalink", item);
                    if (permalink == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    final DownloadLink dl = createDownloadlink("https://soundclouddecrypted.com/" + usernameOfSet + "/" + permalink);
                    final AvailableStatus status = ((jd.plugins.hoster.SoundcloudCom) HOSTPLUGIN).checkStatus(dl, item);
                    dl.setAvailableStatus(status);
                    decryptedLinks.add(dl);
                }
            } else {
                // Decrypt all tracks of a user
                username = ((jd.plugins.hoster.SoundcloudCom) HOSTPLUGIN).getXML("username", br.toString());
                if (username == null) username = getJson("username");
                if (username == null) username = new Regex(parameter, "soundcloud\\.com/(.+)").getMatch(0);
                String userID = br.getRegex("<uri>https://api\\.soundcloud\\.com/users/(\\d+)").getMatch(0);
                if (userID == null) userID = br.getRegex("id type=\"integer\">(\\d+)").getMatch(0);
                if (userID == null) {
                    logger.info("Link probably offline: " + parameter);
                    return decryptedLinks;
                }
                br.getPage("https://api.sndcdn.com/e1/users/" + userID + "/sounds?limit=10000&offset=0&linked_partitioning=1&client_id=" + clientID);
                final String[] items = br.getRegex("<stream\\-item>(.*?)</stream\\-item>").getColumn(0);
                if (items == null || items.length == 0) {
                    if (br.containsHTML("<stream\\-items type=\"array\"/>")) {
                        logger.info("Link offline: " + parameter);
                        return decryptedLinks;
                    }
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String item : items) {
                    final String url = ((jd.plugins.hoster.SoundcloudCom) HOSTPLUGIN).getXML("permalink", item);
                    if (url == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    final DownloadLink dl = createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted") + "/" + url);
                    final AvailableStatus status = ((jd.plugins.hoster.SoundcloudCom) HOSTPLUGIN).checkStatus(dl, item);
                    dl.setAvailableStatus(status);
                    if (decryptThumb) {
                        try {
                            // Handle thumbnail stuff
                            final DownloadLink thumb = getThumbnail(dl, item);
                            if (thumb != null) decryptedLinks.add(thumb);
                        } catch (final ParseException e) {
                            logger.info("Failed to get thumbnail, adding song link only");
                        }
                    }
                    decryptedLinks.add(dl);
                }
            }
            final String date = br.getRegex("<created\\-at type=\"datetime\">([^<>\"]*?)</created\\-at>").getMatch(0);
            if (username == null) username = "Unknown user";
            username = Encoding.htmlDecode(username.trim());
            if (playlistname == null) playlistname = username;
            playlistname = Encoding.htmlDecode(playlistname.trim());
            final String fpName = getFormattedPackagename(username, playlistname, date);
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        } else {
            // If the user wants to download the thumbnail also it's a bit more complicated
            if (decryptThumb) {
                try {
                    br.getPage(parameter);
                    if (br.containsHTML("\"404 \\- Not Found\"")) {
                        final DownloadLink dl = createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted"));
                        dl.setAvailable(false);
                        dl.setProperty("offline", true);
                        decryptedLinks.add(dl);
                        return decryptedLinks;
                    }
                    br.getPage("https://api.sndcdn.com/resolve?url=" + Encoding.urlEncode(parameter) + "&_status_code_map%5B302%5D=200&_status_format=json&client_id=" + CLIENTID);
                    // Add soundcloud link
                    final DownloadLink dl = createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted"));
                    final AvailableStatus status = ((jd.plugins.hoster.SoundcloudCom) HOSTPLUGIN).checkStatus(dl, this.br.toString());
                    dl.setAvailableStatus(status);
                    decryptedLinks.add(dl);

                    // Handle thumbnail stuff
                    final DownloadLink thumb = getThumbnail(dl, this.br.toString());
                    if (thumb != null) decryptedLinks.add(thumb);
                } catch (final Exception e) {
                    logger.info("Failed to get thumbnail, adding song link only");
                    decryptedLinks.add(createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted")));
                }
            } else {
                decryptedLinks.add(createDownloadlink(parameter.replace("soundcloud", "soundclouddecrypted")));
            }
        }
        return decryptedLinks;
    }

    private DownloadLink getThumbnail(final DownloadLink audiolink, final String source) throws ParseException {
        // Handle artwork stuff
        DownloadLink thumb = null;
        String artworkurl = new Regex(source, "<artwork\\-url>(https?://[a-z0-9]+\\.sndcdn\\.com/artworks\\-[a-z0-9\\-]+\\-large\\.jpg\\?[a-z0-9]+)</artwork\\-url>").getMatch(0);
        if (artworkurl != null) {
            artworkurl = artworkurl.replace("-large.jpg", "-t500x500.jpg");
            thumb = createDownloadlink("directhttp://" + artworkurl);
            thumb.setProperty("originaldate", audiolink.getStringProperty("originaldate", null));
            thumb.setProperty("plainfilename", audiolink.getStringProperty("plainfilename", null));
            thumb.setProperty("channel", audiolink.getStringProperty("channel", null));
            thumb.setProperty("type", ".jpg");
            final String formattedFilename = ((jd.plugins.hoster.SoundcloudCom) HOSTPLUGIN).getFormattedFilename(thumb);
            thumb.setFinalFileName(formattedFilename);
            thumb.setAvailable(true);
        }
        return thumb;
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^\"]+)").getMatch(0);
        return result;
    }

    private final static String defaultCustomPackagename = "*channelname* - *playlistname*";

    public String getFormattedPackagename(final String channelname, final String playlistname, String date) throws ParseException {
        String formattedpackagename = CFG.getStringProperty(CUSTOM_PACKAGENAME, defaultCustomPackagename);
        if (formattedpackagename == null || formattedpackagename.equals("")) formattedpackagename = defaultCustomPackagename;
        if (!formattedpackagename.contains("*channelname*") && !formattedpackagename.contains("*playlistname*")) formattedpackagename = defaultCustomPackagename;

        String formattedDate = null;
        if (date != null && formattedpackagename.contains("*date*")) {
            // 2011-08-10T22:50:49Z
            date = date.replace("T", ":");
            final String userDefinedDateFormat = CFG.getStringProperty(CUSTOM_DATE);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd:HH:mm");
            Date dateStr = formatter.parse(date);

            formattedDate = formatter.format(dateStr);
            Date theDate = formatter.parse(formattedDate);

            if (userDefinedDateFormat != null) {
                try {
                    formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(theDate);
                } catch (Exception e) {
                    // prevent user error killing plugin.
                    formattedDate = "";
                }
            }
            if (formattedDate != null)
                formattedpackagename = formattedpackagename.replace("*date*", formattedDate);
            else
                formattedpackagename = formattedpackagename.replace("*date*", "");
        }
        if (formattedpackagename.contains("*channelname*")) {
            formattedpackagename = formattedpackagename.replace("*channelname*", channelname);
        }
        // Insert playlistname at the end to prevent errors with tags
        formattedpackagename = formattedpackagename.replace("*playlistname*", playlistname);

        return formattedpackagename;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}