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
import org.appwork.utils.Regex;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SweetpornOrg extends PluginForDecrypt {
    public SweetpornOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "sweetporn.org" });
        ret.add(new String[] { "sitesrip.org" });
        ret.add(new String[] { "pornrips.org" });
        /* 2021-04-19 */
        ret.add(new String[] { "hdpornclub.biz" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?!category|tag|wp-content)([a-z0-9\\-]+)(/.*)?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getHttpConnection().getContentType().contains("html")) {
            /* E.,g. /wp-json */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Obtain title from URL */
        final String fpName = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0).replace("-", " ");
        final String[] ids = br.getRegex("get_download_link\\('([^<>\"\\']+)").getColumn(0);
        if (ids.length == 0) {
            logger.info("Either plugin broken or failed to find any downloadable content");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName).trim());
        final String thumbnails[] = br.getRegex("href\\s*=\\s*\"[^\"]*goto[^\"]*\"[^>]*>\\s*<img\\s*src\\s*=\\s*\"(https?://.*?)\"").getColumn(0);
        for (String thumbnail : thumbnails) {
            final DownloadLink dl = createDownloadlink(thumbnail);
            dl._setFilePackage(fp);
            decryptedLinks.add(dl);
            distribute(dl);
        }
        int index = 0;
        for (final String id : ids) {
            logger.info("Crawling item " + (index + 1) + " / " + ids.length);
            final Browser brc = br.cloneBrowser();
            brc.postPage("/get_file.php", "id=" + Encoding.urlEncode(id));
            final Map<String, Object> entries = restoreFromString(brc.toString(), TypeRef.MAP);
            final String html = (String) entries.get("htmlcode");
            final String url = new Regex(html, "<a href=\"([^\"]+)").getMatch(0);
            if (url != null) {
                final DownloadLink dl = createDownloadlink(url);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
            } else {
                /*
                 * 2021-03-12: This can happen for single items e.g.
                 * "<b><font color='Red'>This File not Available for Downloading!</font></b>"
                 */
                logger.info("Failed to find URL for ID: " + id);
                logger.info("HTML=");
                logger.info(html);
            }
            // final String[] urls = HTMLParser.getHttpLinks(html, br.getURL());
            // for (final String url : urls) {
            // final DownloadLink dl = createDownloadlink(url);
            // dl._setFilePackage(fp);
            // decryptedLinks.add(dl);
            // distribute(dl);
            // }
            if (this.isAbort()) {
                break;
            }
            index += 1;
        }
        return decryptedLinks;
    }
}
