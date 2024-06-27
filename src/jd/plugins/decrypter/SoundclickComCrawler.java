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

import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLSearch;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.SoundClickCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { SoundClickCom.class })
public class SoundclickComCrawler extends PluginForDecrypt {
    public SoundclickComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        return SoundClickCom.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/.*bandID=\\d+.*");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        final UrlQuery query = UrlQuery.parse(contenturl);
        String singleSongID = query.get("ID");
        if (singleSongID == null) {
            singleSongID = query.get("songID");
        }
        if (singleSongID != null) {
            /* Single song -> Will be handled by hosterplugin. */
            final DownloadLink link = createDownloadlink(generateSingleSongURL(singleSongID));
            ret.add(link);
        } else {
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String bandID = new Regex(param.getCryptedUrl(), "(?:&|\\?)bandID=(\\d+)").getMatch(0);
            String title = br.getRegex("id=\"sclkArtist_pageHead_name\"[^>]*>([^<]+)</div>").getMatch(0);
            if (title == null) {
                title = HTMLSearch.searchMetaTag(br, "og:title");
            }
            String[] songIDs = br.getRegex("data-songid=\"(\\d+)").getColumn(0);
            if (songIDs == null || songIDs.length == 0) {
                songIDs = br.getRegex("songid=(\\d+)").getColumn(0);
            }
            if (songIDs == null || songIDs.length == 0) {
                /* We can be quite sure that there is no downloadable content. */
                logger.info("Link offline or plugin broken");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final FilePackage fp = FilePackage.getInstance();
            if (title != null) {
                fp.setName(Encoding.htmlDecode(title).trim());
            } else {
                /* Fallback */
                fp.setName(bandID);
            }
            for (final String songID : songIDs) {
                final DownloadLink link = createDownloadlink(generateSingleSongURL(songID));
                if (title != null) {
                    link.setName(title + " - " + songID + ".mp3");
                } else {
                    link.setName(songID + ".mp3");
                }
                /* We know that the content is available. Do this so it won't get checked again via hosterplugin. */
                link.setAvailable(true);
                link._setFilePackage(fp);
                ret.add(link);
            }
        }
        return ret;
    }

    private String generateSingleSongURL(final String songID) {
        return "https://www." + getHost() + "/music/songInfo.cfm?songID=" + songID;
    }
}
