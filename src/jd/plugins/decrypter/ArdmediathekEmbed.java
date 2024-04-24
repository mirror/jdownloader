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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLSearch;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ArdmediathekEmbed extends PluginForDecrypt {
    public ArdmediathekEmbed(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "swrfernsehen.de", "swr.de" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl().replaceFirst("^(?i)http://", "https://");
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = HTMLSearch.searchMetaTag(br, "og:title");
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
        }
        final ArrayList<String> audioIDs = new ArrayList<String>();
        final String[] ardaudiothekHTMLs = br.getRegex("<p class=\"mediaplayer-download\"[^>]*><a href=\"https?://[^>]*>.*?</a></p>").getColumn(-1);
        if (ardaudiothekHTMLs != null && ardaudiothekHTMLs.length > 0) {
            /* E.g. https://www.swr.de/swr4/tipps/rezept-kraeuterdip-crissini-selber-machen-100.html */
            for (final String ardaudiothekHTML : ardaudiothekHTMLs) {
                final String url = new Regex(ardaudiothekHTML, "href=\"(https?://[^\"]+)").getMatch(0);
                final String filesize = new Regex(ardaudiothekHTML, "\\((\\d+(,\\d+)? [A-Za-z]{1,5}) \\| MP3").getMatch(0);
                final DownloadLink audio = this.createDownloadlink(url);
                if (title != null && ardaudiothekHTMLs.length == 1) {
                    audio.setFinalFileName(title + ".mp3");
                }
                if (filesize != null) {
                    audio.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                audio.setAvailable(true);
                ret.add(audio);
                final String audioID = new Regex(url, "(\\d+)\\.?[A-Za-z]+\\.mp3$").getMatch(0);
                if (audioID != null) {
                    audioIDs.add(audioID);
                }
            }
        }
        /*
         * Very simple wrapper that finds embedded content --> Adds it via "new style" of URLs --> Goes into Ardmediathek main crawler and
         * content gets crawler
         */
        final String[] links = br.getRegex("data-cridid=\"(crid://[^\"]+)\"").getColumn(0);
        if (links != null && links.length > 0) {
            if (links.length == audioIDs.size()) {
                logger.info("Skipping possible video items because most likely those would be the same audio items that we've already crawled");
            } else {
                for (final String ardAppURL : links) {
                    final String mediaID = new Regex(ardAppURL, "(\\d+)$").getMatch(0);
                    if (audioIDs.contains(mediaID)) {
                        /* Skip audio items as we've already crawled them and they cannot be accessed via ardmediathek.de. */
                        continue;
                    }
                    String appURLEncoded = Encoding.Base64Encode(ardAppURL);
                    /* WTF */
                    appURLEncoded = appURLEncoded.replace("=", "");
                    ret.add(createDownloadlink("https://www.ardmediathek.de/ard/player/" + appURLEncoded));
                }
            }
        }
        if (ret.isEmpty()) {
            logger.info("Failed to find any downloadable content");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        if (title != null) {
            fp.setName(title);
        } else {
            /* Fallback */
            fp.setName(br._getURL().getPath());
        }
        fp.addLinks(ret);
        return ret;
    }
}
