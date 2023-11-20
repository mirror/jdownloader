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
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getHttpConnection().getContentType().contains("html")) {
            /* E.,g. /wp-json */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Obtain title from URL */
        final String urlSlug = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String fpName = urlSlug.replace("-", " ").trim();
        final String[] ids = br.getRegex("get_download_link\\('([^<>\"\\']+)").getColumn(0);
        final String[] b64Strings = br.getRegex("/goto\\?([a-zA-Z0-9_/\\+\\=\\-%]+)").getColumn(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        final String thumbnails[] = br.getRegex("href\\s*=\\s*\"[^\"]*goto[^\"]*\"[^>]*>\\s*<img\\s*src\\s*=\\s*\"(https?://.*?)\"").getColumn(0);
        for (String thumbnail : thumbnails) {
            final DownloadLink dl = createDownloadlink(thumbnail);
            dl._setFilePackage(fp);
            ret.add(dl);
            distribute(dl);
        }
        final HashSet<String> dupes = new HashSet<String>();
        if (ids != null && ids.length > 0) {
            int index = 0;
            for (final String id : ids) {
                if (!dupes.add(id)) {
                    continue;
                }
                logger.info("Crawling item " + (index + 1) + " / " + ids.length);
                final Browser brc = br.cloneBrowser();
                brc.postPage("/get_file.php", "id=" + Encoding.urlEncode(id));
                final Map<String, Object> entries = restoreFromString(brc.toString(), TypeRef.MAP);
                final String html = (String) entries.get("htmlcode");
                final String url = new Regex(html, "<a href=\"([^\"]+)").getMatch(0);
                if (url != null) {
                    final DownloadLink dl = createDownloadlink(url);
                    dl._setFilePackage(fp);
                    ret.add(dl);
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
                    logger.info("Stopping because: Aborted by user");
                    break;
                } else {
                    /* Continue to next item */
                    index += 1;
                }
            }
        }
        if (b64Strings != null && b64Strings.length > 0) {
            /* 2023-11-20: hdpornclub.biz */
            for (final String b64String : b64Strings) {
                if (!dupes.add(b64String)) {
                    continue;
                }
                final ScriptEngineManager mgr = JavaScriptEngineFactory.getScriptEngineManager(null);
                final ScriptEngine engine = mgr.getEngineByName("JavaScript");
                final StringBuilder sb = new StringBuilder();
                sb.append(
                        "            function bde(lkfv) {                var b64 = \"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=\";                var o1, o2, o3, h1, h2, h3, h4, bits, i = 0, ac = 0, dec = \"\", tmp_arr = [];                if (!lkfv) {                    return lkfv;                }                lkfv += '';                do {                    h1 = b64.indexOf(lkfv.charAt(i++));                    h2 = b64.indexOf(lkfv.charAt(i++));                    h3 = b64.indexOf(lkfv.charAt(i++));                    h4 = b64.indexOf(lkfv.charAt(i++));                    bits = h1 << 18 | h2 << 12 | h3 << 6 | h4;                    o1 = bits >> 16 & 0xff;                    o2 = bits >> 8 & 0xff;                    o3 = bits & 0xff;                    if (h3 == 64) {                        tmp_arr[ac++] = String.fromCharCode(o1);                    } else if (h4 == 64) {                        tmp_arr[ac++] = String.fromCharCode(o1, o2);                    } else {                        tmp_arr[ac++] = String.fromCharCode(o1, o2, o3);                    }                } while (i < lkfv.length);                dec = tmp_arr.join('');                return dec;            }");
                sb.append("            function getresult(klbs) {                ensldflis = klbs.length;                for (i = ensldflis - 3; i > 1; i = i - 3) {                    klbs = klbs.substr(0, i) + klbs.substr(i + 1);                }                klbs = klbs.substr(1);                klbs = bde(klbs);              return klbs;            }");
                sb.append("var thisresult = getresult('" + b64String + "');");
                engine.eval(sb.toString());
                final String url = engine.get("thisresult").toString();
                logger.info("js result: " + url);
                final DownloadLink dl = this.createDownloadlink(url);
                dl._setFilePackage(fp);
                ret.add(dl);
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}
