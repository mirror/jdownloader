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
import java.util.HashSet;
import java.util.Set;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bitchute.com" }, urls = { "https?://(?:www\\.|old\\.)?bitchute\\.com/channel/([A-Za-z0-9]+)" })
public class BitchuteComChannel extends PluginForDecrypt {
    public BitchuteComChannel(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Set<String> dupes = new HashSet<String>();
        final String parameter = param.getCryptedUrl();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*This channel is unavailable at your location due to the following restrictions")) {
            /* 2021-09-13 */
            throw new DecrypterRetryException(RetryReason.GEO);
        } else if (br.containsHTML("(?i)>\\s*This channel is blocked under the following Community Guideline")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String csrftoken = this.br.getCookie(br.getHost(), "csrftoken", Cookies.NOTDELETEDPATTERN);
        final String channelUID = br.getRegex("channelRefreshCounts\\('([A-Za-z0-9]+)'").getMatch(0);
        if (StringUtils.isEmpty(csrftoken) || channelUID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /*
         * Prefer to use channelname from html - channelname inside URL is always lowercase and sometimes a cryptic string while html
         * contains the "nicer" channelname.
         */
        String channelname = br.getRegex("id=\"channel-title\"[^>]*>([^>]+)<").getMatch(0);
        if (StringUtils.isEmpty(channelname)) {
            /* Fallback */
            channelname = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(channelname.trim()));
        int index = 0;
        int page = 0;
        final int itemsPerRequest = 25;
        br.getHeaders().put("x-requested-with", "XMLHttpRequest");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        do {
            logger.info("Crawling page: " + page);
            br.postPage("https://www.bitchute.com/channel/" + channelUID + "/extend/", "csrfmiddlewaretoken=" + Encoding.urlEncode(csrftoken) + "&name=&offset=" + index);
            final String[] videoIDs = br.getRegex("/video/([A-Za-z0-9]+)/").getColumn(0);
            int addedItems = 0;
            for (final String videoID : videoIDs) {
                if (isAbort()) {
                    break;
                } else if (dupes.add(videoID)) {
                    dupes.add(videoID);
                    final DownloadLink dl = createDownloadlink("https://www." + this.getHost() + "/video/" + videoID);
                    dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
                    dl.setAvailable(true);
                    /* Property for packagizer */
                    dl.setProperty("username", channelname);
                    dl._setFilePackage(fp);
                    ret.add(dl);
                    distribute(dl);
                    addedItems++;
                }
            }
            logger.info("Crawling page: " + page + " | Found items so far: " + index);
            if (addedItems < itemsPerRequest) {
                logger.info("Stopping because number of items found doesn't match number of items per request");
                break;
            }
            index += itemsPerRequest;
            page += 1;
        } while (!this.isAbort());
        return ret;
    }
}
