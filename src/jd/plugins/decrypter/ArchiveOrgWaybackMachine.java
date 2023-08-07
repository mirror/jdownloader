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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ArchiveOrgWaybackMachine extends PluginForDecrypt {
    public ArchiveOrgWaybackMachine(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "web.archive.org" });
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
            ret.add("https?://" + buildHostsPatternPart(domains) + "/web/(\\d+)\\*?/.+");
        }
        return ret.toArray(new String[0]);
    }

    private final String PATTERN_FILE = "(?i)^(https?://[^/]+/web)/(\\d+)[^/]*/(.+)";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String url = param.getCryptedUrl();
        final Regex fileURLRegex = new Regex(param.getCryptedUrl(), PATTERN_FILE);
        final String fileID = fileURLRegex.getMatch(1);
        if (fileID != null) {
            /* This may convert such URLs to direct-downloadable URLs. */
            url = fileURLRegex.getMatch(0) + "/" + fileID + "if_" + "/" + fileURLRegex.getMatch(2);
        }
        /* First check if maybe the user has added a directURL. */
        final GetRequest getRequest = br.createGetRequest(url);
        final URLConnectionAdapter con = this.br.openRequestConnection(getRequest);
        try {
            if (this.looksLikeDownloadableContent(con)) {
                final DownloadLink direct = getCrawler().createDirectHTTPDownloadLink(getRequest, con);
                ret.add(direct);
            } else {
                br.followConnection();
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                /* E.g. embedded PDF */
                final String directurl = br.getRegex("<iframe id=\"playback\"[^>]*src=\"(https?://[^\"]+)").getMatch(0);
                if (directurl == null) {
                    logger.info("URL is not supported or content is offline");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    ret.add(this.createDownloadlink(DirectHTTP.createURLForThisPlugin(directurl)));
                }
            }
        } finally {
            con.disconnect();
        }
        return ret;
    }
}
