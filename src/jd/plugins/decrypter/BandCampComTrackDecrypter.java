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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision: 39491 $", interfaceVersion = 2, names = { "bandcamp.com" }, urls = { "https?://(www\\.)?[a-z0-9\\-]+\\.bandcamp\\.com/track/[a-z0-9\\-_]+" })
public class BandCampComTrackDecrypter extends PluginForDecrypt {
    public BandCampComTrackDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">Sorry, that something isn\\'t here\\.<|trackinfo[\t\n\r ]*?:[\t\n\r ]*?\\[\\],") || this.br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final SubConfiguration CFG = SubConfiguration.getConfig("bandcamp.com");
        final DownloadLink link = createDownloadlink(parameter.replaceFirst("bandcamp\\.com", "bandcampdecrypted.com"));
        if (CFG.getBooleanProperty(jd.plugins.hoster.BandCampCom.FASTLINKCHECK, false)) {
            link.setAvailable(true);
        }
        String duration = br.getRegex("<meta itemprop=\"duration\"\\s*content=\"([0-9\\.]+)\"").getMatch(0);
        if (duration != null) {
            final long length = 128 * 1024l / 8 * (int) Double.parseDouble(duration);
            link.setDownloadSize(length);
            link.setAvailable(true);
        }
        link.setProperty("type", "mp3");
        final String artist = br.getRegex("artist\\s*:\\s*\"([^<>\"]*?)\"").getMatch(0);
        if (artist != null) {
            link.setProperty("directartist", artist);
        }
        final String date = br.getRegex("<meta itemprop=\"datePublished\" content=\"(\\d+)\"/>").getMatch(0);
        if (date != null) {
            link.setProperty("directdate", date);
        }
        String album = br.getRegex("album_title\\s*:\\s*\"([^<>\"]*?)\"").getMatch(0);
        if (album == null) {
            album = br.getRegex("\\s+title\\s*:\\s*\"([^<>\"]*?)\"").getMatch(0);
        }
        if (album != null) {
            link.setProperty("directalbum", album);
        }
        String tracknumber = br.getRegex("\"track_number\":(\\d+)").getMatch(0);
        if (tracknumber != null) {
            final int trackNum = Integer.parseInt(tracknumber);
            final DecimalFormat df;
            if (trackNum > 999) {
                df = new DecimalFormat("0000");
            } else if (trackNum > 99) {
                df = new DecimalFormat("000");
            } else {
                df = new DecimalFormat("00");
            }
            link.setProperty("directtracknumber", df.format(trackNum));
        }
        final String name = br.getRegex("itemprop=\"name\">\\s*([^<>\"]*?)\\s*</").getMatch(0);
        if (name != null) {
            link.setProperty("directname", name);
        }
        if (name != null) {
            final String formattedFilename = jd.plugins.hoster.BandCampCom.getFormattedFilename(link);
            link.setName(formattedFilename);
        }
        decryptedLinks.add(link);
        if (StringUtils.isAllNotEmpty(artist, date, album)) {
            final FilePackage fp = FilePackage.getInstance();
            final String formattedpackagename = BandCampComDecrypter.getFormattedPackagename(CFG, artist, album, date);
            if (!CFG.getBooleanProperty(jd.plugins.hoster.BandCampCom.CLEANPACKAGENAME, false)) {
                fp.setProperty("CLEANUP_NAME", false);
            }
            fp.setName(formattedpackagename);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}