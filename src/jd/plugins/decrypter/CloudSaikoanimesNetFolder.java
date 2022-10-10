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

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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
        do {
            br.getPage(nextPageURL);
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
            if (fp == null) {
                final Map<String, Object> rootFolder = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "meta/root/data/attributes");
                if (rootFolder != null) {
                    fp = FilePackage.getInstance();
                    fp.setName(rootFolder.get("name").toString());
                }
            }
            final List<Map<String, Object>> items = (List<Map<String, Object>>) entries.get("data");
            for (final Map<String, Object> item : items) {
                final Map<String, Object> itemdata = (Map<String, Object>) item.get("data");
                final Map<String, Object> attributes = (Map<String, Object>) itemdata.get("attributes");
                final String filename = attributes.get("name").toString();
                final DownloadLink file = this.createDownloadlink("directhttp://" + attributes.get("file_url"));
                file.setFinalFileName(filename);
                file.setProperty(DirectHTTP.FIXNAME, filename);
                file.setDownloadSize(SizeFormatter.getSize(attributes.get("filesize").toString()));
                file.setAvailable(true);
                if (fp != null) {
                    file._setFilePackage(fp);
                }
                distribute(file);
                ret.add(file);
            }
            final Map<String, Object> links = (Map<String, Object>) entries.get("links");
            nextPageURL = (String) links.get("next");
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
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
        return ret;
    }
}
