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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "camvideos.org" }, urls = { "https?://(?:www\\.)?camvideos\\.org/videos/(\\d+)/([a-z0-9\\-]+)/" })
public class CamvideosOrg extends PornEmbedParser {
    public CamvideosOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    /**
     * Note: 2021-03-17: This website used to have selfhosted content but I was unable to find that thus at this moment we're only having
     * this crawler. </br>
     * In case a host plugin is needed again, use: KernelVideoSharingComV2HostsDefault
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* Get filename from URL */
        String filename = new Regex(parameter, this.getSupportedLinks()).getMatch(1).replace("-", " ");
        filename = filename.trim();
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (decryptedLinks.isEmpty()) {
            /* 2021-12-09 e.g. dood.to embedded URLs im iFrame */
            final String[] iframes = br.getRegex("<iframe(.*?)</iframe>").getColumn(0);
            for (final String iframe : iframes) {
                final String[] urls = HTMLParser.getHttpLinks(iframe, br.getURL());
                for (final String url : urls) {
                    decryptedLinks.add(this.createDownloadlink(url));
                }
            }
        }
        return decryptedLinks;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KernelVideoSharing;
    }
}
