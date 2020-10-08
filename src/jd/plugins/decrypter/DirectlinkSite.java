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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "directlink.site" }, urls = { "https?://(?:www\\.)?directlink\\.site/l/([A-Za-z0-9]+)" })
public class DirectlinkSite extends PluginForDecrypt {
    public DirectlinkSite(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String itemID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.getHeaders().put("Referer", parameter);
        /* Oh yeah there is an API :D */
        br.postPage("https://" + this.getHost() + "/api/get", "url=" + itemID);
        final String errorStr = PluginJSonUtils.getJson(br, "error");
        if (br.getHttpConnection().getResponseCode() == 404 || "true".equals(errorStr)) {
            /* 2020-10-08: E.g. returns error without errormessage == offline: {"error":true} */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String finallink = PluginJSonUtils.getJson(br, "url");
        if (StringUtils.isEmpty(finallink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
