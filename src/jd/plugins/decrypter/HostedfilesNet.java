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

import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hostedfiles.net" }, urls = { "https?://(?:www\\.)?hostedfiles\\.net/cl\\.php\\?id=[a-f0-9]{32}" })
public class HostedfilesNet extends PluginForDecrypt {
    public HostedfilesNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String id = UrlQuery.parse(parameter).get("id");
        /* Let's skip the first step */
        br.getHeaders().put("Referer", parameter);
        br.setFollowRedirects(true);
        br.getPage("https://www." + this.getHost() + "/cl/load.php?f=1&a=&id=" + id);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String redirectURL = br.getRegex("var url_redirect = \"([^\"]+)\"").getMatch(0);
        if (redirectURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        redirectURL = PluginJSonUtils.unescape(redirectURL);
        String base64 = UrlQuery.parse(redirectURL).get("r");
        base64 = Encoding.htmlDecode(base64);
        final String finallink = Encoding.Base64Decode(base64);
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
