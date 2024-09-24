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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
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
        final String channelID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        /*
         * Prefer to use channelname from html - channelname inside URL is always lowercase and sometimes a cryptic string while html
         * contains the "nicer" channelname.
         */
        String channelname = br.getRegex("itemprop=\"name\" content=\"([^\"]+)").getMatch(0);
        if (StringUtils.isEmpty(channelname)) {
            /* Fallback */
            channelname = channelID;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(channelname.trim()));
        fp.setPackageKey("bitchute://channel/" + channelID);
        int offset = 0;
        int page = 0;
        final int itemsPerRequest = 10;
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        final Map<String, Object> postdata = new HashMap<String, Object>();
        postdata.put("channel_id", channelID);
        postdata.put("limit", itemsPerRequest);
        pagination: do {
            postdata.put("offset", offset);
            br.getPage(br.createJSonPostRequest("https://api.bitchute.com/api/beta/channel/videos", postdata));
            if (br.getHttpConnection().getResponseCode() == 403) {
                /**
                 * E.g. GEO-blocked: <br>
                 * {"errors":[{"context":"AUTH","message":"Forbidden - Cannot perform this action"},{"context":"reason","message":"Content
                 * access is restricted based on the users location"}]}
                 */
                throw new DecrypterRetryException(RetryReason.GEO);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> videos = (List<Map<String, Object>>) entries.get("videos");
            int numberofNewItemsThisPage = 0;
            for (final Map<String, Object> videomap : videos) {
                final String videoID = videomap.get("video_id").toString();
                if (!dupes.add(videoID)) {
                    continue;
                }
                final DownloadLink dl = createDownloadlink("https://www." + this.getHost() + "/video/" + videoID);
                dl.setName(videomap.get("video_name") + ".mp4");
                dl.setAvailable(true);
                /* Property for Packagizer */
                dl.setProperty("username", channelname);
                dl._setFilePackage(fp);
                ret.add(dl);
                distribute(dl);
                numberofNewItemsThisPage++;
            }
            logger.info("Crawled page: " + page + " | offset: " + offset + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                break pagination;
            } else if (numberofNewItemsThisPage < itemsPerRequest) {
                logger.info("Stopping because number of items found doesn't match number of items per request");
                break pagination;
            }
            /* Continue to next page */
            offset += itemsPerRequest;
            page += 1;
        } while (!this.isAbort());
        return ret;
    }
}
