//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

/**
 * So I had this written some time back, just never committed. Here is my original with proper error handling etc. -raz
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ScnlogEu extends antiDDoSForDecrypt {
    public ScnlogEu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "scnlog.me", "scnlog.eu", "scnlog.life" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:[a-z0-9_\\-]+/){2,}");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String[] deadDomains = new String[] { "scnlog.eu", "scnlog.life" };
        br.setFollowRedirects(true);
        String contenturl = param.getCryptedUrl();
        /* Change domain in url if it is a domain known to be dead. */
        final String addedHost = Browser.getHost(contenturl);
        for (final String deadDomain : deadDomains) {
            if (StringUtils.equalsIgnoreCase(addedHost, deadDomain)) {
                contenturl = contenturl.replaceFirst(Pattern.quote(deadDomain), this.getHost());
                break;
            }
        }
        getPage(contenturl);
        if (br.containsHTML("<title>404 Page Not Found</title>|>Sorry, but you are looking for something that isn't here\\.<") || this.br.toString().length() < 200 || br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.getURL().contains("/feed/") || this.br.getURL().contains("/feedback/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String fpName = br.getRegex("<strong>Release:</strong>\\s*(.*?)<(?:/|\\w*\\s*/)").getMatch(0);
        String download = br.getRegex("<div class=\"download\">.*?</div>").getMatch(-1);
        if (download == null) {
            /* Probably not a single track but a "Seearch result" site */
            logger.warning("Can not find 'download table', Please report this to JDownloader Development Team : " + contenturl);
            final String[] urls = br.getRegex("src=\"/wp-content/uploads/music\\.png\"[^>]+/><a href=\"(https?://[^/]+/music/[^\"]+)\"").getColumn(0);
            if (urls == null || urls.length == 0) {
                return null;
            }
            for (final String url : urls) {
                ret.add(createDownloadlink(url));
            }
        } else {
            final String[] results = HTMLParser.getHttpLinks(download, "");
            for (String result : results) {
                // prevent site links from been added.
                if (this.canHandle(result)) {
                    continue;
                } else {
                    ret.add(createDownloadlink(result));
                }
            }
        }
        if (ret.isEmpty()) {
            if (br.containsHTML("(?i)>\\s*Links have been removed due to DMCA request")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setAllowMerge(true);
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2024-02-05: Preventive measure to try to avoid running into rate-limit. */
        return 1;
    }
}