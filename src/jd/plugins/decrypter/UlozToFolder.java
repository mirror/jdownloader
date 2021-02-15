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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class UlozToFolder extends PluginForDecrypt {
    public UlozToFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        /* ulozto.net = the english version of the site */
        ret.add(new String[] { "uloz.to", "ulozto.sk", "ulozto.cz", "ulozto.net", "zachowajto.pl" });
        ret.add(new String[] { "pornfile.cz", "pornfile.ulozto.net" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/((?:download|file)-tracking/[a-f0-9]{96}|folder/[A-Za-z0-9]+/name/.+)");
        }
        return ret.toArray(new String[0]);
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    /** 2021-02-11: This host does not like many requests in a short time! */
    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    /** Special (temporary/IP bound) URLs available when searching for files directly on the website of this file-hoster. */
    private static final String TYPE_TRACKING = "https?://[^/]+/(?:download|file)-tracking/[a-f0-9]{96}";
    private static final String TYPE_FOLDER   = "https?://[^/]+/folder/([A-Za-z0-9]+)/name/(.+)";

    /** 2021-02-11: This host is GEO-blocking german IPs! */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_TRACKING)) {
            return crawlSingleURL(param);
        } else {
            return crawlFolder(param);
        }
    }

    private ArrayList<DownloadLink> crawlSingleURL(final CryptedLink param) throws IOException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        br.getPage(param.getCryptedUrl());
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        } else {
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

    private static final String API_BASE = "https://apis.uloz.to/v5";

    private ArrayList<DownloadLink> crawlFolder(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (true) {
            /* TODO */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("X-Auth-Token", "TODO");
        final String folderID = new Regex(param.getCryptedUrl(), TYPE_FOLDER).getMatch(0);
        final String folderNameURL = new Regex(param.getCryptedUrl(), TYPE_FOLDER).getMatch(1);
        br.getPage(API_BASE + "/folder/" + folderID + "/" + folderNameURL + "/" + folderID + "?cacheBusting=" + System.currentTimeMillis());
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("folder");
        final String folderName = (String) entries.get("name");
        final boolean is_password_protected = ((Boolean) entries.get("is_password_protected")).booleanValue();
        if (is_password_protected) {
            /* TODO */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // br.getHeaders().put("X-Password", "TODO");
        }
        final FilePackage fp = FilePackage.getInstance();
        if (!StringUtils.isEmpty(folderName)) {
            fp.setName(folderName);
        } else {
            /* Fallback */
            fp.setName(folderID);
        }
        int offset = 0;
        final int maxItemsPerPage = 50;
        int page = 0;
        do {
            page += 1;
            logger.info("Crawling page: " + page);
            br.getPage(API_BASE + "/folder/" + folderID + "/file-list?sort=-created&offset=" + offset + "&limit=" + maxItemsPerPage + "&cacheBusting=" + System.currentTimeMillis());
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final List<Object> ressourcelist = (List<Object>) entries.get("items");
            for (final Object fileO : ressourcelist) {
                entries = (Map<String, Object>) fileO;
                final String filename = (String) entries.get("name");
                final long filesize = ((Number) entries.get("filesize")).longValue();
                final String description = (String) entries.get("description");
                final String url = (String) entries.get("url");
                if (StringUtils.isEmpty(url)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final DownloadLink dl = this.createDownloadlink("https://" + this.getHost() + url);
                dl.setFinalFileName(filename);
                dl.setVerifiedFileSize(filesize);
                if (!StringUtils.isEmpty(description)) {
                    dl.setComment(description);
                }
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                distribute(dl);
                decryptedLinks.add(dl);
            }
            if (ressourcelist.size() < maxItemsPerPage) {
                logger.info("Stopping because: Reached end");
                break;
            }
        } while (!this.isAbort());
        return decryptedLinks;
    }
}
