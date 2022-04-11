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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "audionow.de" }, urls = { "https?://(?:www\\.)?audionow\\.de/podcast/([a-f0-9\\-]+)" })
public class AudionowDe extends PluginForDecrypt {
    public AudionowDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING };
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String contentID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        /* 2021-02-18: E.g. redirect to "audionow.de/404" without actual 404 http code */
        if (br.getHttpConnection().getResponseCode() == 404 || !this.canHandle(br.getURL())) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        /* 2021-05-22: We can't get a good packagename via API which is why we'll use both website and API. */
        String fpName = br.getRegex("data-podTitle=\"([^<>\"]+)\"").getMatch(0);
        if (fpName == null) {
            /* Fallback */
            fpName = contentID;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        /* 2021-05-22: Use API */
        final boolean useAPI = true;
        if (useAPI) {
            int page = 1;
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            do {
                br.getPage("https://audionow.de/api/v4/podcast/" + contentID + "/episodes.json?format=json&page=" + page);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
                    return decryptedLinks;
                }
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final List<Object> ressourcelist = (List<Object>) entries.get("data");
                for (final Object podcastO : ressourcelist) {
                    final Map<String, Object> podcast = (Map<String, Object>) podcastO;
                    final String title = (String) podcast.get("title");
                    final long filesize = ((Number) podcast.get("fileSize")).longValue();
                    final String directurl = (String) podcast.get("mediaURL");
                    final String description = (String) podcast.get("description");
                    if (StringUtils.isEmpty(title) || StringUtils.isEmpty(directurl)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final DownloadLink dl = this.createDownloadlink(directurl);
                    dl.setFinalFileName(title + ".mp3");
                    dl.setVerifiedFileSize(filesize);
                    dl.setAvailable(true);
                    if (!StringUtils.isEmpty(description)) {
                        dl.setComment(description);
                    }
                    dl._setFilePackage(fp);
                    decryptedLinks.add(dl);
                    distribute(dl);
                }
                final Map<String, Object> pagination = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "meta/pagination");
                final int lastPage = ((Number) pagination.get("total_pages")).intValue();
                logger.info("Found " + ressourcelist.size() + " items on page " + page + " / " + lastPage);
                if (this.isAbort()) {
                    break;
                } else if (page == lastPage) {
                    logger.info("Stopping because: Last page");
                    break;
                } else {
                    page += 1;
                }
            } while (true);
        } else {
            final String[] htmls = br.getRegex("<li[^>]*class=\"episode-list-item\">(.*?)</div>\\s*</li>").getColumn(0);
            if (htmls == null || htmls.length == 0) {
                logger.warning("Decrypter broken for link: " + param.getCryptedUrl());
                return null;
            }
            fpName = Encoding.htmlDecode(fpName);
            int index = 0;
            for (final String html : htmls) {
                index++;
                String eptitle = new Regex(html, "class=\"episode-title\"[^>]*>([^<>\"]+)<").getMatch(0);
                final String directurl = new Regex(html, "data-audiolink=\"(https://[^<>\"]+)").getMatch(0);
                final String filesize = new Regex(html, "<span[^>]*class=\"text-size\"[^>]*>(\\d+ [A-Za-z]+)</span>").getMatch(0);
                // final String date = new Regex(html, "class=\"text-date\">(\\d{2}\\.\\d{2}\\.\\d{4})<").getMatch(0);
                if (StringUtils.isEmpty(directurl)) {
                    /* Skip invalid items */
                    continue;
                }
                if (StringUtils.isEmpty(eptitle)) {
                    /* Fallback */
                    eptitle = index + "";
                }
                eptitle = Encoding.htmlDecode(eptitle);
                final DownloadLink dl = createDownloadlink(directurl);
                dl.setFinalFileName(fpName + " - " + eptitle + ".mp3");
                if (filesize != null) {
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
            }
            if (decryptedLinks.size() == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return decryptedLinks;
    }
}
