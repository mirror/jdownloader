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
import java.util.List;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DatPiffCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DatpiffComCrawler extends PluginForDecrypt {
    public DatpiffComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "datpiff.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9\\-_]+)\\-mixtape\\.(\\d+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String mixtapeTitleURL = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String mixtapeTitleURLNicer = mixtapeTitleURL.replace("-", " ");
        final String mixtapeDownloadID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(1);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String artist = br.getRegex("<li class=\"artist\">([^<>\"]+)</li>").getMatch(0);
        if (artist != null) {
            artist = Encoding.htmlDecode(artist).trim();
        }
        final String htmlTracks = br.getRegex("<ul class=\"tracklist\">(.*?)</ul>").getMatch(0);
        final String[] tracks = htmlTracks.split("</li>");
        if (tracks.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        long totalEstimatedFilesize = 0;
        for (final String htmlTrack : tracks) {
            final String trackDownloadID = new Regex(htmlTrack, "openTrackDownload\\(\\s*'(\\d+)").getMatch(0);
            final String title = new Regex(htmlTrack, "title=\"([^\"]+)\"").getMatch(0);
            final String trackPosition = new Regex(htmlTrack, "class=\"tracknumber\">(\\d+)\\.<").getMatch(0);
            if (trackDownloadID == null || title == null || trackPosition == null) {
                /* Skip invalid items */
                continue;
            }
            String mixtapeName = new Regex(htmlTrack, "content=\"([^\"]+)\" itemprop=\"inAlbum\"").getMatch(0);
            final String mixtapeStreamID = new Regex(htmlTrack, "openMixtape\\(\\s*'([a-z0-9]+)").getMatch(0);
            final DownloadLink link = this.createDownloadlink("https://www.datpiff.com/pop-download-track.php?id=" + trackDownloadID);
            String filename = Encoding.htmlDecode(title).trim();
            if (mixtapeName != null) {
                mixtapeName = Encoding.htmlDecode(mixtapeName).trim();
                filename = mixtapeName + " - " + filename;
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(mixtapeName);
                link._setFilePackage(fp);
            }
            if (artist != null) {
                filename = artist + " - " + filename;
                link.setProperty(DatPiffCom.PROPERTY_ARTIST, artist);
            }
            link.setProperty(DatPiffCom.PROPERTY_SONG_TRACK_DOWNLOAD_ID, trackDownloadID);
            link.setProperty(DatPiffCom.PROPERTY_SONG_MIXTAPE_DOWNLOAD_ID, mixtapeDownloadID);
            if (trackPosition != null) {
                link.setProperty(DatPiffCom.PROPERTY_SONG_TRACK_POSITION, trackPosition);
            }
            if (mixtapeStreamID != null) {
                link.setProperty(DatPiffCom.PROPERTY_SONG_MIXTAPE_STREAM_ID, mixtapeStreamID);
            }
            link.setAvailable(true);
            link.setFinalFileName(filename + ".mp3");
            /* Set estimated filesize based on a bitrate of 160KB/s */
            final Regex durationRegex = new Regex(htmlTrack, "content=\"PT(\\d+)M(\\d+)S\" itemprop=\"duration\"");
            final long durationSeconds = Integer.parseInt(durationRegex.getMatch(0)) * 60 + Integer.parseInt(durationRegex.getMatch(1));
            final long trackEstimatedFilesize = (durationSeconds * 160 * 1024) / 8;
            link.setDownloadSize(trackEstimatedFilesize);
            totalEstimatedFilesize += trackEstimatedFilesize;
            decryptedLinks.add(link);
        }
        /* Add mixtape to host plugin as it can also be downloaded as .zip file in some cases. */
        final DownloadLink mixtape = this.createDownloadlink(param.getCryptedUrl());
        mixtape.setName(mixtapeTitleURLNicer + ".zip");
        mixtape.setAvailable(true);
        if (artist != null) {
            mixtape.setProperty(DatPiffCom.PROPERTY_ARTIST, artist);
        }
        mixtape.setDownloadSize(totalEstimatedFilesize);
        decryptedLinks.add(mixtape);
        return decryptedLinks;
    }
}
