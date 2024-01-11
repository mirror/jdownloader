//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.NitroFlareCom;

/**
 *
 * @author raztoki, pspzockerscene
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { NitroFlareCom.class })
public class NitroFlareComFolder extends PluginForDecrypt {
    public NitroFlareComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        return NitroFlareCom.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/folder/(\\d+)/([A-Za-z0-9=]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex urlinfo = new Regex(param.getCryptedUrl(), this.getSupportedLinks());
        final String userid = urlinfo.getMatch(0);
        final String folderid = urlinfo.getMatch(1);
        final String host = NitroFlareCom.getBaseDomain(this, br);
        final int maxItemsPerPage = 100; // website uses 50
        /* fetchAll=1 avoids pagination */
        final UrlQuery query = new UrlQuery();
        query.add("userId", userid);
        query.add("folder", Encoding.urlEncode(folderid));
        query.add("perPage", Integer.toString(maxItemsPerPage));
        FilePackage fp = null;
        int page = 1;
        do {
            query.addAndReplace("page", Integer.toString(page));
            br.postPage("https://" + host + "/ajax/folder.php", query);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final List<Map<String, Object>> files = (List<Map<String, Object>>) entries.get("files");
            final int totalNumberofItems = ((Number) entries.get("total")).intValue();
            if (totalNumberofItems == 0) {
                /* Folder is empty or does not exist */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (fp == null) {
                final String folderName = (String) entries.get("name");
                fp = FilePackage.getInstance();
                if (!StringUtils.isEmpty(folderName)) {
                    fp.setName(folderName);
                } else {
                    /* Fallback */
                    fp.setName(folderid);
                }
            }
            for (final Map<String, Object> file : files) {
                String url = file.get("url").toString();
                if (url.startsWith("view/")) {
                    url = "/" + url;
                }
                final DownloadLink link = this.createDownloadlink(br.getURL(url).toString());
                link.setName(file.get("name").toString());
                link.setDownloadSize(SizeFormatter.getSize(file.get("size").toString()));
                link.setAvailable(true);
                link._setFilePackage(fp);
                ret.add(link);
                distribute(link);
            }
            final double percentCompleted = ((double) ret.size() / (double) totalNumberofItems) * 100;
            logger.info("Crawled page " + page + " | Found items: " + ret.size() + "/" + totalNumberofItems + " | Percent completed: " + String.format("%2.2f", percentCompleted));
            page++;
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (files.size() < maxItemsPerPage) {
                logger.info("Stopping because: Reached last page");
                break;
            } else if (ret.size() >= totalNumberofItems) {
                /* Double fail-safe */
                logger.info("Stopping because: Found all items");
                break;
            }
        } while (true);
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}