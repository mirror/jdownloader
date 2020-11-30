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
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.URLHelper;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bandcamp.com" }, urls = { "https?://(([a-z0-9\\-]+\\.)?bandcamp\\.com/(?:album|track)/[a-z0-9\\-_]+|(?<!www\\.)?[a-z0-9\\-]+\\.bandcamp\\.com/?$)" })
public class BandCampComDecrypter extends PluginForDecrypt {
    public BandCampComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-07-30: Too many requests in a short time --> 503 server response */
        return 1;
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final SubConfiguration CFG = SubConfiguration.getConfig(this.getHost());
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">Sorry, that something isn\\'t here\\.<|trackinfo\\s*:\\s*\\[\\],") || this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String json = br.getRegex("trackinfo(?:&quot;|\")\\s*:\\s*(\\[.*?\\])(\\s*,\\s*\"|\\s*,\\s*&quot)").getMatch(0);
        if (!br.getURL().contains("bandcamp.com") && json == null) {
            /* 2020-03-16: Redirect to external website */
            decryptedLinks.add(this.createDownloadlink(br.getURL()));
            return decryptedLinks;
        }
        /*
         * 2020-11-30: URLs to Labels look the same as artist URLs but they won't lead to any music directly but simply list all artists
         * under that label.
         */
        final String[] artistList = br.getRegex("(https?://[^/]+\\.bandcamp\\.com)\\?label=\\d+").getColumn(0);
        final String[] albumList = br.getRegex("(/album/[^<>\"\\']+)").getColumn(0);
        if (json == null) {
            if (!this.canHandle(br.getURL())) {
                logger.info("Invalid URL or URL doesn't contain any downloadable content");
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            } else {
                /* Let's see if there is anything else other than music that we may be able to download. */
                if (artistList.length > 0) {
                    for (String artistURL : artistList) {
                        artistURL += "/";
                        decryptedLinks.add(this.createDownloadlink(artistURL));
                    }
                    return decryptedLinks;
                } else if (albumList.length > 0) {
                    for (String albumURL : albumList) {
                        albumURL = br.getURL(albumURL).toString();
                        decryptedLinks.add(this.createDownloadlink(albumURL));
                    }
                    return decryptedLinks;
                } else {
                    /* E.g. https://daily.bandcamp.com/ */
                    logger.info("Failed to find any downloadable content");
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
            }
        } else if (Encoding.isHtmlEntityCoded(json)) {
            json = Encoding.htmlDecode(json);
        }
        final String json_album = br.getRegex("<script type=\"application/(?:json\\+ld|ld\\+json)\">\\s*(.*?)\\s*</script>").getMatch(0);
        if (json_album == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> albumInfo = JSonStorage.restoreFromString(json_album, TypeRef.HASHMAP);
        String artist = (String) JavaScriptEngineFactory.walkJson(albumInfo, "byArtist/name");
        if (artist == null) {
            artist = br.getRegex("name\\s*=\\s*\"title\"\\s*content\\s*=\\s*\"[^\"]+,\\s*by\\s*([^<>\"]+)\\s*\"").getMatch(0);
        }
        String album = (String) JavaScriptEngineFactory.walkJson(albumInfo, "inAlbum/name");
        if (album == null) {
            album = br.getRegex("<title>\\s*(.*?)\\s*\\|.*?</title>").getMatch(0);
            if (album == null) {
                album = br.getRegex("<title>\\s*(.*?)\\s*</title>").getMatch(0);
            }
        }
        String date = (String) JavaScriptEngineFactory.walkJson(albumInfo, "datePublished");
        if (date == null) {
            date = br.getRegex("<meta itemprop=\"datePublished\" content=\"(\\d+)\"/>").getMatch(0);
        }
        // if (links == null || links.length == 0 || artist == null || album == null || date == null) {
        // if (br.getURL().endsWith("bandcamp.com/")) {
        // return decryptedLinks;
        // }
        // if (br.containsHTML("class='download-link buy-link'")) {
        // logger.info("Seems like this album can't be downloaded: " + parameter);
        // return decryptedLinks;
        // }
        // logger.warning("Decrypter broken for link: " + parameter);
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        final List<Object> ressourcelist = (List<Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        artist = Encoding.htmlDecode(artist.trim());
        album = Encoding.htmlDecode(album.trim());
        final DecimalFormat df;
        if (ressourcelist.size() > 999) {
            df = new DecimalFormat("0000");
        } else if (ressourcelist.size() > 99) {
            df = new DecimalFormat("000");
        } else {
            df = new DecimalFormat("00");
        }
        int trackcounter = 1;
        for (final Object audioO : ressourcelist) {
            final Map<String, Object> entries = (Map<String, Object>) audioO;
            String dllink = (String) entries.get("title_link");
            final String title = (String) entries.get("title");
            final long duration = JavaScriptEngineFactory.toLong(entries.get("duration"), 0);
            if (StringUtils.isEmpty(dllink) || StringUtils.isEmpty(title)) {
                /* Skip invalid objects */
                continue;
            }
            if (dllink.startsWith("/")) {
                dllink = "https://" + Browser.getHost(parameter, true) + dllink;
            }
            final DownloadLink dl = createDownloadlink(dllink);
            dl.setProperty("fromdecrypter", true);
            dl.setProperty("directdate", date);
            dl.setProperty("directartist", artist);
            dl.setProperty("directalbum", album);
            dl.setProperty("directname", title);
            dl.setProperty("type", "mp3");
            dl.setProperty("directtracknumber", df.format(trackcounter));
            final String formattedFilename = jd.plugins.hoster.BandCampCom.getFormattedFilename(this, dl);
            dl.setName(formattedFilename);
            if (CFG.getBooleanProperty(jd.plugins.hoster.BandCampCom.FASTLINKCHECK, true)) {
                dl.setAvailable(true);
            }
            if (duration > 0) {
                final long length = 128 * 1024l / 8 * duration;
                dl.setDownloadSize(length);
                dl.setAvailable(true);
            }
            decryptedLinks.add(dl);
            trackcounter++;
        }
        final boolean decryptThumb = CFG.getBooleanProperty(jd.plugins.hoster.BandCampCom.GRABTHUMB, false);
        if (decryptThumb) {
            String thumbnail = br.getRegex("<a class=\"popupImage\" href=\"(https?://[^<>\"]*?\\.jpg)\"").getMatch(0);
            if (thumbnail != null) {
                thumbnail = thumbnail.replaceFirst("(_\\d+)(\\.\\w+)$", "_0$2");
                final DownloadLink thumb = createDownloadlink("directhttp://" + thumbnail);
                thumb.setProperty("fromdecrypter", true);
                thumb.setProperty("directdate", date);
                thumb.setProperty("directartist", artist);
                thumb.setProperty("directalbum", album);
                thumb.setProperty("directname", "thumbnail");
                thumb.setProperty("type", "jpg");
                thumb.setProperty("directtracknumber", df.format(0));
                final String formattedFilename = jd.plugins.hoster.BandCampCom.getFormattedFilename(this, thumb);
                thumb.setFinalFileName(formattedFilename);
                decryptedLinks.add(thumb);
            }
        }
        final String videos[][] = br.getRegex("<a class=\"has-video\"\\s*href=\"(/video/t/\\d+)\"\\s*data-href-mobile=\"(/.*?)\"").getMatches();
        if (videos != null) {
            final HashSet<String> dups = new HashSet<String>();
            for (final String video[] : videos) {
                final String original = URLHelper.parseLocation(new URL("http://bandcamp.23video.com"), video[1]);
                String nameResult = null;
                final String names[][] = br.getRegex("<div class=\"title\">.*?<span itemprop=\"name\">(.*?)</span>(.*?)</div>").getMatches();
                if (names != null) {
                    for (String name[] : names) {
                        if (name[1].contains(video[0])) {
                            nameResult = name[0];
                            break;
                        }
                    }
                }
                for (final String format : new String[] { "video_mobile_high", "video_hd", "video_1080p", }) {
                    final String url = original.replace("video_mobile_high", format);
                    if (dups.add(url)) {
                        final DownloadLink dl = createDownloadlink("directhttp://" + url.toString());
                        if (nameResult != null) {
                            dl.setFinalFileName(nameResult + "_" + format + Plugin.getFileNameExtensionFromURL(url));
                        }
                        if (CFG.getBooleanProperty(jd.plugins.hoster.BandCampCom.FASTLINKCHECK, true)) {
                            dl.setAvailable(true);
                            decryptedLinks.add(dl);
                        } else {
                            Browser br2 = br.cloneBrowser();
                            br2.setFollowRedirects(true);
                            URLConnectionAdapter con = null;
                            try {
                                con = br2.openHeadConnection(url);
                                if (looksLikeDownloadableContent(con)) {
                                    dl.setAvailable(true);
                                    dl.setVerifiedFileSize(con.getLongContentLength());
                                    decryptedLinks.add(dl);
                                }
                            } catch (IOException e) {
                                logger.log(e);
                            } finally {
                                if (con != null) {
                                    con.disconnect();
                                }
                            }
                        }
                    }
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        final String formattedpackagename = getFormattedPackagename(this, CFG, artist, album, date);
        if (!CFG.getBooleanProperty(jd.plugins.hoster.BandCampCom.CLEANPACKAGENAME, false)) {
            fp.setProperty("CLEANUP_NAME", false);
        }
        fp.setName(formattedpackagename);
        fp.addLinks(decryptedLinks);
        if (decryptedLinks.size() == 0) {
            logger.info("Failed to find any downloadable content: Empty album?");
        }
        return decryptedLinks;
    }

    private final static String defaultCustomPackagename = "*artist* - *album*";

    public static String getFormattedPackagename(PluginForDecrypt plugin, SubConfiguration cfg, final String artist, final String album, final String dateString) throws ParseException {
        String formattedpackagename = cfg.getStringProperty(jd.plugins.hoster.BandCampCom.CUSTOM_PACKAGENAME, defaultCustomPackagename);
        if (formattedpackagename == null || formattedpackagename.equals("")) {
            formattedpackagename = defaultCustomPackagename;
        }
        if (!formattedpackagename.contains("*artist*") && !formattedpackagename.contains("*album*")) {
            formattedpackagename = defaultCustomPackagename;
        }
        if (dateString != null && formattedpackagename.contains("*date*")) {
            Date date = TimeFormatter.parseDateString(dateString);
            if (date == null) {
                try {
                    final SimpleDateFormat oldFormat = new SimpleDateFormat("yyyyMMdd");
                    date = oldFormat.parse(dateString);
                } catch (Exception e) {
                    plugin.getLogger().log(e);
                }
            }
            if (date != null) {
                final String userDefinedDateFormat = cfg.getStringProperty("CUSTOM_DATE", "dd.MM.yyyy_HH-mm-ss");
                String formattedDate = null;
                for (final String format : new String[] { userDefinedDateFormat, "yyyyMMdd" }) {
                    if (format != null) {
                        try {
                            final SimpleDateFormat formatter = new SimpleDateFormat(userDefinedDateFormat);
                            formattedDate = formatter.format(date);
                            if (formattedDate != null) {
                                break;
                            }
                        } catch (Exception e) {
                            plugin.getLogger().log(e);
                        }
                    }
                }
                if (formattedDate != null) {
                    formattedpackagename = formattedpackagename.replace("*date*", formattedDate);
                } else {
                    formattedpackagename = formattedpackagename.replace("*date*", "");
                }
            }
        }
        if (formattedpackagename.contains("*artist*")) {
            formattedpackagename = formattedpackagename.replace("*artist*", artist);
        }
        // Insert albumname at the end to prevent errors with tags
        formattedpackagename = formattedpackagename.replace("*album*", album);
        if (cfg.getBooleanProperty(jd.plugins.hoster.BandCampCom.PACKAGENAMELOWERCASE, false)) {
            formattedpackagename = formattedpackagename.toLowerCase();
        }
        if (cfg.getBooleanProperty(jd.plugins.hoster.BandCampCom.PACKAGENAMESPACE, false)) {
            formattedpackagename = formattedpackagename.replace(" ", "_");
        }
        return formattedpackagename;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}