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
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.ArchivebateCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ArchivebateComCrawler extends PluginForDecrypt {
    public ArchivebateComCrawler(PluginWrapper wrapper) {
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
        ret.add(new String[] { "archivebate.com", "archivebate.cc" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:embed|watch)/(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl().replaceFirst("^(?i)http://", "https://");
        final ArchivebateCom hosterplugin = (ArchivebateCom) this.getNewPluginForHostInstance(this.getHost());
        final DownloadLink selfhostedVideo = this.createDownloadlink(contenturl);
        hosterplugin.requestFileInformation(selfhostedVideo);
        /* No exception = Item looks to be online */
        selfhostedVideo.setDefaultPlugin(hosterplugin);
        selfhostedVideo.setAvailable(true);
        final String iframePlayerURL = br.getRegex("<iframe src=\"(https?://[^\"]+)\"").getMatch(0);
        String externalDownloadurl = br.getRegex("href=\"(https?://[^\"]+)\" class=\"btn download-btn\"").getMatch(0);
        if (externalDownloadurl == null) {
            externalDownloadurl = br.getRegex("name=\"fid\"[^>]*value=\"([^\"]+)").getMatch(0);
        }
        if (externalDownloadurl != null) {
            /* We need the full URL with protocol */
            externalDownloadurl = br.getURL(externalDownloadurl).toExternalForm();
        }
        if (iframePlayerURL != null && !this.canHandle(iframePlayerURL)) {
            /* Video is hosted on external website */
            ret.add(this.createDownloadlink(iframePlayerURL));
        } else if (externalDownloadurl != null) {
            /* Video is hosted on external website */
            if (iframePlayerURL != null) {
                /* Try to fix broken downloadurl */
                final String domain = Browser.getHost(iframePlayerURL, true);
                final String domainWithSlash = domain + "/";
                if (externalDownloadurl.contains(domain) && !externalDownloadurl.contains(domainWithSlash)) {
                    externalDownloadurl = externalDownloadurl.replace(domain, domainWithSlash);
                }
            }
            ret.add(this.createDownloadlink(externalDownloadurl));
        } else {
            /* Video is selfhosted */
            ret.add(selfhostedVideo);
        }
        return ret;
    }
}
