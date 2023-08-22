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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.appwork.storage.TypeRef;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "audionow.de" }, urls = { "https?://(?:(?:www\\.)?audionow\\.de|plus\\.rtl\\.de)/podcast/([a-z0-9\\-]+)" })
public class AudionowDe extends PluginForDecrypt {
    public AudionowDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    private static String     token      = null;
    private static AtomicLong validUntil = new AtomicLong(0);

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        /* 2021-02-18: E.g. redirect to "audionow.de/404" without actual 404 http code */
        if (br.getHttpConnection().getResponseCode() == 404 || !this.canHandle(br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String contentID = new Regex(br.getURL(), "(?i)/podcast/[a-z0-9\\-]+\\-([a-z0-9]+)(/.+|$)").getMatch(0);
        if (contentID == null) {
            /* Invalid URL or developer mistake. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        synchronized (validUntil) {
            if (token == null || System.currentTimeMillis() > validUntil.get()) {
                logger.info("Obtaining fresh token");
                br.getPage("https://plus.rtl.de/main.5bd47519b4daf53f.js");
                final String secret = br.getRegex("client_secret\\s*:\\s*\"([a-f0-9\\-]+)").getMatch(0);
                br.postPage("https://auth.rtl.de/auth/realms/rtlplus/protocol/openid-connect/token", "grant_type=client_credentials&client_id=anonymous-user&client_secret=" + secret);
                final Map<String, Object> authmap = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                token = authmap.get("access_token").toString();
                validUntil.set(System.currentTimeMillis() + (((Number) authmap.get("expires_in")).longValue() * 1000));
            }
        }
        br.getHeaders().put("Authorization", "Bearer " + token);
        br.getHeaders().put("Rtlplus-Client-Id", "rci:rtlplus:web");
        br.getHeaders().put("Rtlplus-Client-Version", "2023.8.21.5");
        /* See main.*.js -> Yn = fn.sha256,... */
        final String sha256Hash = "3a24ebe82cd8425d597419728fff9d7e4b8894e8c36af583699d2c196048e0ed";
        final int itemsPerPage = 20;
        int offset = 0;
        final FilePackage fp = FilePackage.getInstance();
        int page = 1;
        do {
            br.getPage("https://cdn.gateway.now-plus-prod.aws-cbc.cloud/graphql?operationName=PodcastDetail&variables=%7B%22offset%22:" + offset + ",%22id%22:%22" + contentID + "%22,%22take%22:" + itemsPerPage + "   ,%22sort%22:%7B%22direction%22:%22DEFAULT%22%7D%7D&extensions=%7B%22persistedQuery%22:%7B%22version%22:1,%22sha256Hash%22:%22" + sha256Hash + "%22%7D%7D");
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> data = (Map<String, Object>) entries.get("data");
            final Map<String, Object> podcast = (Map<String, Object>) data.get("podcast");
            final Map<String, Object> episodesmap = (Map<String, Object>) podcast.get("episodes");
            final List<Map<String, Object>> episodelist = (List<Map<String, Object>>) episodesmap.get("items");
            if (offset == 0) {
                fp.setName(podcast.get("title").toString());
                fp.setComment(podcast.get("description").toString());
            }
            for (final Map<String, Object> episode : episodelist) {
                final DownloadLink link = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(episode.get("url").toString()));
                link.setFinalFileName(episode.get("title").toString() + ".mp3");
                link.setComment(episode.get("description").toString());
                link.setAvailable(true);
                link._setFilePackage(fp);
                distribute(link);
                ret.add(link);
            }
            final int numberOfEpisodes = ((Number) podcast.get("numberOfEpisodes")).intValue();
            logger.info("Crawled page " + page + " | Found items: " + ret.size() + "/" + numberOfEpisodes);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
            } else if (offset >= numberOfEpisodes) {
                logger.info("Stopping because: Found all items: " + numberOfEpisodes);
                break;
            } else {
                offset += episodelist.size();
                page++;
                continue;
            }
        } while (!this.isAbort());
        return ret;
    }
}
