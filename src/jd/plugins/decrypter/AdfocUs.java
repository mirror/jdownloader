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
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AdfocUs extends PluginForDecrypt {
    public AdfocUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "adfoc.us" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(serve/\\?id=[a-z0-9]+|[a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        String id = new Regex(parameter, "(?i)\\.us/(.+)").getMatch(0);
        if ("forum".equalsIgnoreCase(id) || "support".equalsIgnoreCase(id) || "self".equalsIgnoreCase(id) || "user".equalsIgnoreCase(id) || "payout".equalsIgnoreCase(id) || "api".equalsIgnoreCase(id) || "js".equalsIgnoreCase(id) || "ajax".equalsIgnoreCase(id) || "faq".equalsIgnoreCase(id) || "1How".equalsIgnoreCase(id) || "tickets".equalsIgnoreCase(id) || "advertise".equalsIgnoreCase(id) || "serve".equalsIgnoreCase(id) || "privacy".equalsIgnoreCase(id) || "terms".equalsIgnoreCase(id)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        br.setFollowRedirects(false);
        if (br.containsHTML(">403 Forbidden<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("No htmlCode read")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String click = br.getRegex("(?i)var click_url = \"(https?://[^\"]+)").getMatch(0);
        if (click != null && click.equals(parameter)) {
            click = null;
        }
        if (click == null) {
            click = br.getRegex("(http://adfoc\\.us/serve/click/\\?id=[a-z0-9]+\\&servehash=[a-z0-9]+\\&timestamp=\\d+)").getMatch(0);
        }
        if (click != null && click.contains("adfoc.us/") && !click.contains(id)) {
            /* adfoc link leads to another adfoc link */
            ret.add(createDownloadlink(click));
            return ret;
        } else if (click != null && click.contains("adfoc.us/")) {
            br.getHeaders().put("Referer", parameter);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage(HTMLEntities.unhtmlentities(click));
            if (br.getRedirectLocation() != null && !br.getRedirectLocation().matches("http://adfoc.us/")) {
                ret.add(this.createDownloadlink(br.getRedirectLocation()));
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            /* External URL */
            ret.add(this.createDownloadlink(click));
        }
        return ret;
    }
}
