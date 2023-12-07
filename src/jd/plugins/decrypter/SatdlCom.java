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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SatdlCom extends PluginForDecrypt {
    public SatdlCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "satdl.com" });
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
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/decrypt.+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /*
         * 2023-12-07: I've removed support for "/download/..." from the pattern since that seems to only redirect to dummy files and no
         * external downloadurls.
         */
        final Pattern pattern_download = Pattern.compile("https?://[^/]+/download/(\\d+)");
        String slashDownloadURL = null;
        if (new Regex(contenturl, pattern_download).patternFind()) {
            slashDownloadURL = contenturl;
        } else {
            final String redirect = br.getRegex("location\\.replace\\(\"(https?://[^\"]+)").getMatch(0);
            if (redirect != null) {
                /* Typically a redirect to the main-page. */
                br.getPage(redirect);
            }
            final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
            for (final String url : urls) {
                if (new Regex(url, pattern_download).patternFind()) {
                    slashDownloadURL = url;
                    break;
                }
            }
            if (slashDownloadURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(slashDownloadURL);
        }
        final String externalRedirect = br.getRegex("window\\.location\\.href = \"(https?://[^\"]+)").getMatch(0);
        if (externalRedirect != null) {
            /* Typically via "protected.to" -> Redirects to final download website */
            ret.add(this.createDownloadlink(externalRedirect));
        } else {
            logger.warning("Failed to find externalRedirect");
        }
        final boolean addSelfhostedDirecturl = false;
        if (addSelfhostedDirecturl) {
            final String selfhostedDirectDownloadlink = slashDownloadURL.replace("/download/", "/protected-download/");
            final DownloadLink direct = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(selfhostedDirectDownloadlink));
            direct.setReferrerUrl(slashDownloadURL);
            ret.add(direct);
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}
