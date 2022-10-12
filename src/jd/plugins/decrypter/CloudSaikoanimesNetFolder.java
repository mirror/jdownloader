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

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CloudSaikoanimesNetFolder extends PluginForDecrypt {
    public CloudSaikoanimesNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "cloud.saikoanimes.net" });
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
            ret.add("https?://" + buildHostsPatternPart(domains) + "/share/([A-Za-z0-9]+)/files/([a-f0-9\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String folderID = urlinfo.getMatch(0);
        final String folderhash = urlinfo.getMatch(1);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // br.getHeaders().put("X-Xsrf-Token", "");
        br.getPage("https://" + this.getHost() + "/api/sharing/" + folderID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* E.g. {"type":"error","message":"We couldn't find the resource you requested with id <folderID>"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String nextPageURL = "/api/sharing/folders/" + folderhash + "/" + folderID + "?sort=created_at&direction=DESC&page=1";
        int page = 1;
        FilePackage fp = null;
        long itemsFound = 0;
        do {
            br.getPage(nextPageURL);
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            if (fp == null) {
                final Map<String, Object> rootFolder = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "meta/root/data/attributes");
                if (rootFolder != null) {
                    fp = FilePackage.getInstance();
                    fp.setName(rootFolder.get("name").toString());
                }
            }
            final long itemsFoundBefore = itemsFound;
            final List<Map<String, Object>> items = (List<Map<String, Object>>) entries.get("data");
            itemsFound += items.size();
            for (final Map<String, Object> item : items) {
                final Map<String, Object> itemdata = (Map<String, Object>) item.get("data");
                final String id = itemdata.get("id").toString();
                final String type = itemdata.get("type").toString();
                final Map<String, Object> attributes = (Map<String, Object>) itemdata.get("attributes");
                final String name = attributes.get("name").toString();
                final DownloadLink downloadLink;
                if ("folder".equalsIgnoreCase(type)) {
                    final Number numberOfItems = (Number) attributes.get("items");
                    if (numberOfItems == null || numberOfItems.longValue() == 0) {
                        logger.info("Skip empty folder:" + name + "|" + id);
                    }
                    downloadLink = this.createDownloadlink(br.getURL("/share/" + folderID + "/files/" + id).toString());
                } else {
                    downloadLink = this.createDownloadlink("directhttp://" + attributes.get("file_url").toString());
                    downloadLink.setFinalFileName(name);
                    downloadLink.setProperty(DirectHTTP.FIXNAME, name);
                    downloadLink.setDownloadSize(SizeFormatter.getSize(attributes.get("filesize").toString()));
                    downloadLink.setAvailable(true);
                }
                if (fp != null) {
                    downloadLink._setFilePackage(fp);
                }
                distribute(downloadLink);
                ret.add(downloadLink);
            }
            final Map<String, Object> links = (Map<String, Object>) entries.get("links");
            nextPageURL = (String) links.get("next");
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
            if (itemsFound == itemsFoundBefore) {
                logger.info("Stopping because: no new items found");
                break;
            } else if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (StringUtils.isEmpty(nextPageURL)) {
                logger.info("Stopping because: nextPageURL is empty -> Looks like we've reached the last page");
                break;
            } else {
                /* Continue to next page */
                page++;
                continue;
            }
        } while (true);
        if (ret.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            return ret;
        }
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
