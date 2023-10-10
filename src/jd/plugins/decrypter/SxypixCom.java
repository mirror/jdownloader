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

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SxypixCom extends PluginForDecrypt {
    public SxypixCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "sxypix.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:watch\\?g=|w/)([a-f0-9]{32})");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String galleryHash = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        /* 2021-09-30: pics.vc/watch URLs will often redirect to sxypix.com/w URLs. */
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("class=\\.gall_not_found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("class='gnf'")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fpName = br.getRegex("(?i)<title>\\s*([^<]+)\\s*-\\s*(?:PICS\\.VC|SxyPix\\.com)\\s*</title>").getMatch(0);
        int foundNumberofItems = 0;
        int page = 0;
        final DecimalFormat df = new DecimalFormat("000");
        final FilePackage fp = FilePackage.getInstance();
        if (fpName != null) {
            final String lines[] = StringUtils.getLines(fpName);
            fp.setName(Encoding.htmlDecode(lines[0].trim()));
            if (lines.length > 1) {
                fp.setComment(StringUtils.join(Arrays.asList(lines).subList(1, lines.length), ","));
            }
        } else {
            /* Fallback */
            fp.setName(galleryHash);
        }
        HashSet<String> dupes = new HashSet<String>();
        final String dataX = br.getRegex("data-x\\s*=\\s*'(.*?)'").getMatch(0);
        final String dataAid = br.getRegex("data-aid\\s*=\\s*'(.*?)'").getMatch(0);
        final String dataPhotoid = br.getRegex("data-photoid\\s*=\\s*'([a-f0-9]{32})'").getMatch(0);
        if (dataX == null || dataPhotoid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Origin", "https://" + br.getHost());
        final UrlQuery query = new UrlQuery();
        query.add("x", dataX);
        if (dataAid != null) {
            query.add("aid", dataAid);
        }
        query.add("pid", dataPhotoid);
        query.add("ghash", galleryHash);
        query.add("width", "1720");
        br.postPage("/php/gall.php", query);
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
        final List<String> htmls = (List<String>) entries.get("r");
        for (final String html : htmls) {
            final String relativeurl = new Regex(html, "data-src=\\'([^\\']+)'").getMatch(0);
            if (relativeurl == null) {
                /* Skip invalid items */
                continue;
            }
            if (dupes.add(relativeurl)) {
                final URL url = br.getURL(relativeurl);
                String filename = Plugin.getFileNameFromURL(url);
                filename = df.format(++foundNumberofItems) + "_" + filename;
                final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(url.toExternalForm()));
                dl.setFinalFileName(filename);
                dl._setFilePackage(fp);
                dl.setAvailable(true);
                ret.add(dl);
                distribute(dl);
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}
