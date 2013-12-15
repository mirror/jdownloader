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

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bandcamp.com" }, urls = { "http://(www\\.)?[a-z0-9\\-]+\\.bandcamp\\.com/album/[a-z0-9\\-_]+" }, flags = { 0 })
public class BandCampComDecrypter extends PluginForDecrypt {

    public BandCampComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String FASTLINKCHECK      = "FASTLINKCHECK";
    private static final String GRABTHUMB          = "GRABTHUMB";
    private static final String CUSTOM_PACKAGENAME = "CUSTOM_PACKAGENAME";
    private static final String CUSTOM_DATE        = "CUSTOM_DATE";

    private SubConfiguration    CFG                = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        CFG = SubConfiguration.getConfig("bandcamp.com");
        br.getPage(parameter);
        if (br.containsHTML(">Sorry, that something isn\\'t here\\.<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        final Regex inforegex = br.getRegex("<title>(.*?) \\| (.*?)</title>");
        final String[][] links = br.getRegex("\"(/track/[a-z0-9\\-]+)\" itemprop=\"url\"><span itemprop=\"name\">([^<>\"]*?)</span>").getMatches();
        String artist = inforegex.getMatch(1);
        String album = inforegex.getMatch(0);
        final String date = br.getRegex("<meta itemprop=\"datePublished\" content=\"(\\d+)\"/>").getMatch(0);
        if (links == null || links.length == 0 || artist == null || album == null || date == null) {
            if (br.containsHTML("class='download-link buy-link'")) {
                logger.info("Seems like this album can't be downloaded: " + parameter);
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        artist = Encoding.htmlDecode(artist.trim());
        album = Encoding.htmlDecode(album.trim());
        DecimalFormat df = new DecimalFormat("0");
        if (links.length > 999)
            df = new DecimalFormat("0000");
        else if (links.length > 99)
            df = new DecimalFormat("000");
        else if (links.length > 9) df = new DecimalFormat("00");
        int trackcounter = 1;
        final String bcLink = new Regex(parameter, "(http://.*?\\.bandcamp\\.com)/album/").getMatch(0);
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("bandcamp.com");
        for (final String linkinfo[] : links) {
            final String dllink = bcLink + linkinfo[0];
            final String fname = Encoding.htmlDecode(linkinfo[1].trim());
            final DownloadLink dl = createDownloadlink(dllink);
            dl.setProperty("fromdecrypter", true);
            dl.setProperty("directdate", date);
            dl.setProperty("directartist", artist);
            dl.setProperty("directalbum", album);
            dl.setProperty("directname", fname);
            dl.setProperty("type", "mp3");
            dl.setProperty("directtracknumber", df.format(trackcounter));
            final String formattedFilename = ((jd.plugins.hoster.BandCampCom) hostPlugin).getFormattedFilename(dl);
            dl.setName(formattedFilename);
            if (CFG.getBooleanProperty(FASTLINKCHECK, false)) dl.setAvailable(true);
            decryptedLinks.add(dl);
            trackcounter++;
        }

        final boolean decryptThumb = CFG.getBooleanProperty(GRABTHUMB, false);
        final String thumbnail = br.getRegex("<a class=\"popupImage\" href=\"(http://[^<>\"]*?\\.jpg)\"").getMatch(0);
        if (decryptThumb && thumbnail != null) {
            final DownloadLink thumb = createDownloadlink("directhttp://" + thumbnail);
            thumb.setProperty("fromdecrypter", true);
            thumb.setProperty("directdate", date);
            thumb.setProperty("directartist", artist);
            thumb.setProperty("directalbum", album);
            thumb.setProperty("directname", "thumbnail");
            thumb.setProperty("type", "jpg");
            thumb.setProperty("directtracknumber", df.format(0));
            final String formattedFilename = ((jd.plugins.hoster.BandCampCom) hostPlugin).getFormattedFilename(thumb);
            thumb.setFinalFileName(formattedFilename);
            decryptedLinks.add(thumb);
        }

        final FilePackage fp = FilePackage.getInstance();
        final String formattedpackagename = getFormattedPackagename(artist, album, date);
        fp.setName(formattedpackagename);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private final static String defaultCustomPackagename = "*artist* - *album*";

    public String getFormattedPackagename(final String artist, final String album, final String date) throws ParseException {
        String formattedpackagename = CFG.getStringProperty(CUSTOM_PACKAGENAME, defaultCustomPackagename);
        if (formattedpackagename == null || formattedpackagename.equals("")) formattedpackagename = defaultCustomPackagename;
        if (!formattedpackagename.contains("*artist*") && !formattedpackagename.contains("*album*")) formattedpackagename = defaultCustomPackagename;

        String formattedDate = null;
        if (date != null && formattedpackagename.contains("*date*")) {
            final String userDefinedDateFormat = CFG.getStringProperty(CUSTOM_DATE);
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
            if (formattedDate != null)
                formattedpackagename = formattedpackagename.replace("*date*", formattedDate);
            else
                formattedpackagename = formattedpackagename.replace("*date*", "");
        }
        if (formattedpackagename.contains("*artist*")) {
            formattedpackagename = formattedpackagename.replace("*artist*", artist);
        }
        // Insert albumname at the end to prevent errors with tags
        formattedpackagename = formattedpackagename.replace("*album*", album);

        return formattedpackagename;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}