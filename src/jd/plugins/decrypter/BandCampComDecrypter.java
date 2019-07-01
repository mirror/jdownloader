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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bandcamp.com" }, urls = { "https?://((www\\.)?[a-z0-9\\-]+\\.bandcamp\\.com/album/[a-z0-9\\-_]+|(?<!www\\.)?[a-z0-9\\-]+\\.bandcamp\\.com/?$)" })
public class BandCampComDecrypter extends PluginForDecrypt {
    public BandCampComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final SubConfiguration CFG = SubConfiguration.getConfig("bandcamp.com");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">Sorry, that something isn\\'t here\\.<|trackinfo[\t\n\r ]*?:[\t\n\r ]*?\\[\\],") || this.br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String[][] links = br.getRegex("\"(/track/[a-z0-9\\-]+)\" itemprop=\"url\"\\s*>\\s*<span[^<]*itemprop\\s*=\\s*\"name\"\\s*>\\s*([^<>\"]*?)\\s*</span>.*?<meta\\s*itemprop\\s*=\\s*\"duration\"\\s*content=\"(.*?)\"").getMatches();
        String artist = br.getRegex("artist: \"([^<>\"]*?)\"").getMatch(0);
        String album = br.getRegex("<title>\\s*(.*?)\\s*\\|.*?</title>").getMatch(0);
        if (album == null) {
            album = br.getRegex("<title>\\s*(.*?)\\s*</title>").getMatch(0);
        }
        final String date = br.getRegex("<meta itemprop=\"datePublished\" content=\"(\\d+)\"/>").getMatch(0);
        if (links == null || links.length == 0 || artist == null || album == null || date == null) {
            if (br.getURL().endsWith("bandcamp.com/")) {
                return decryptedLinks;
            }
            if (br.containsHTML("class='download-link buy-link'")) {
                logger.info("Seems like this album can't be downloaded: " + parameter);
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        artist = Encoding.htmlDecode(artist.trim());
        album = Encoding.htmlDecode(album.trim());
        final DecimalFormat df;
        if (links.length > 999) {
            df = new DecimalFormat("0000");
        } else if (links.length > 99) {
            df = new DecimalFormat("000");
        } else {
            df = new DecimalFormat("00");
        }
        int trackcounter = 1;
        for (final String linkinfo[] : links) {
            final String dllink = br.getURL(linkinfo[0]).toString().replaceFirst("bandcamp\\.com", "bandcampdecrypted.com");
            final String fname = Encoding.htmlDecode(linkinfo[1].trim());
            final DownloadLink dl = createDownloadlink(dllink);
            dl.setProperty("fromdecrypter", true);
            dl.setProperty("directdate", date);
            dl.setProperty("directartist", artist);
            dl.setProperty("directalbum", album);
            dl.setProperty("directname", fname);
            dl.setProperty("type", "mp3");
            dl.setProperty("directtracknumber", df.format(trackcounter));
            final String formattedFilename = jd.plugins.hoster.BandCampCom.getFormattedFilename(dl);
            dl.setName(formattedFilename);
            if (CFG.getBooleanProperty(jd.plugins.hoster.BandCampCom.FASTLINKCHECK, false)) {
                dl.setAvailable(true);
            }
            final String duration[] = new Regex(linkinfo[2], "P(\\d+)H(\\d+)M(\\d+)S").getRow(0);
            if (duration != null && duration.length == 3) {
                final long length = 128 * 1024l / 8 * ((Integer.parseInt(duration[0]) * 60 * 60) + (Integer.parseInt(duration[1]) * 60) + (Integer.parseInt(duration[2])));
                dl.setDownloadSize(length);
                dl.setAvailable(true);
            }
            decryptedLinks.add(dl);
            trackcounter++;
        }
        final boolean decryptThumb = CFG.getBooleanProperty(jd.plugins.hoster.BandCampCom.GRABTHUMB, false);
        final String thumbnail = br.getRegex("<a class=\"popupImage\" href=\"(https?://[^<>\"]*?\\.jpg)\"").getMatch(0);
        if (decryptThumb && thumbnail != null) {
            final DownloadLink thumb = createDownloadlink("directhttp://" + thumbnail);
            thumb.setProperty("fromdecrypter", true);
            thumb.setProperty("directdate", date);
            thumb.setProperty("directartist", artist);
            thumb.setProperty("directalbum", album);
            thumb.setProperty("directname", "thumbnail");
            thumb.setProperty("type", "jpg");
            thumb.setProperty("directtracknumber", df.format(0));
            final String formattedFilename = jd.plugins.hoster.BandCampCom.getFormattedFilename(thumb);
            thumb.setFinalFileName(formattedFilename);
            decryptedLinks.add(thumb);
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
                        if (CFG.getBooleanProperty(jd.plugins.hoster.BandCampCom.FASTLINKCHECK, false)) {
                            dl.setAvailable(true);
                            decryptedLinks.add(dl);
                        } else {
                            Browser br2 = br.cloneBrowser();
                            URLConnectionAdapter con = null;
                            try {
                                con = br2.openHeadConnection(url);
                                if (con.isOK() && StringUtils.containsIgnoreCase(con.getContentType(), "video")) {
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
        final String formattedpackagename = getFormattedPackagename(CFG, artist, album, date);
        if (!CFG.getBooleanProperty(jd.plugins.hoster.BandCampCom.CLEANPACKAGENAME, false)) {
            fp.setProperty("CLEANUP_NAME", false);
        }
        fp.setName(formattedpackagename);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private final static String defaultCustomPackagename = "*artist* - *album*";

    public static String getFormattedPackagename(SubConfiguration CFG, final String artist, final String album, final String date) throws ParseException {
        String formattedpackagename = CFG.getStringProperty(jd.plugins.hoster.BandCampCom.CUSTOM_PACKAGENAME, defaultCustomPackagename);
        if (formattedpackagename == null || formattedpackagename.equals("")) {
            formattedpackagename = defaultCustomPackagename;
        }
        if (!formattedpackagename.contains("*artist*") && !formattedpackagename.contains("*album*")) {
            formattedpackagename = defaultCustomPackagename;
        }
        String formattedDate = null;
        if (date != null && formattedpackagename.contains("*date*")) {
            final String userDefinedDateFormat = CFG.getStringProperty(jd.plugins.hoster.BandCampCom.CUSTOM_DATE);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
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
            if (formattedDate != null) {
                formattedpackagename = formattedpackagename.replace("*date*", formattedDate);
            } else {
                formattedpackagename = formattedpackagename.replace("*date*", "");
            }
        }
        if (formattedpackagename.contains("*artist*")) {
            formattedpackagename = formattedpackagename.replace("*artist*", artist);
        }
        // Insert albumname at the end to prevent errors with tags
        formattedpackagename = formattedpackagename.replace("*album*", album);
        if (CFG.getBooleanProperty(jd.plugins.hoster.BandCampCom.PACKAGENAMELOWERCASE, false)) {
            formattedpackagename = formattedpackagename.toLowerCase();
        }
        if (CFG.getBooleanProperty(jd.plugins.hoster.BandCampCom.PACKAGENAMESPACE, false)) {
            formattedpackagename = formattedpackagename.replace(" ", "_");
        }
        return formattedpackagename;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}