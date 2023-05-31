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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MovingImageFansOrg extends PluginForDecrypt {
    public MovingImageFansOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "filmfans.org", "serienfans.org" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/external/(?:\\d+/)?[A-Za-z0-9]+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        // br.setAllowedResponseCodes(400);
        // br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)
        // Chrome/113.0.0.0 Safari/537.36");
        // br.getHeaders().put("sec-ch-ua", "\"Google Chrome\";v=\"113\", \"Chromium\";v=\"113\", \"Not-A.Brand\";v=\"24\"");
        // br.getHeaders().put("sec-ch-ua-mobile", "?0");
        // br.getHeaders().put("sec-ch-ua-platform", "\"Windows\"");
        // br.getHeaders().put("Upgrade-Insecure-Requests", "1");
        // br.getHeaders().put("Accept",
        // "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        // br.getHeaders().put("Accept-Language", "de-DE,de;q=0.9");
        // br.getHeaders().put("Sec-Fetch-Site", "none");
        // br.getHeaders().put("Sec-Fetch-Mode", "navigate");
        // br.getHeaders().put("Sec-Fetch-User", "?1");
        // br.getHeaders().put("Sec-Fetch-Dest", "document");
        br.getPage(param.getCryptedUrl().replaceFirst("http://", "https://"));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final DownloadLink link = createDownloadlink(finallink);
        /*
         * Sometimes a password is required in the subsequent crawler and most likely it is the full domain the user added here -> Set PW
         * here so in the best case we will never have to ask the user to enter it.
         */
        link.setDownloadPassword(this.getHost());
        ret.add(link);
        return ret;
    }
}
