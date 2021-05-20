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

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "turboimagehost.com" }, urls = { "https?://(?:www\\.)?turboimagehost\\.com/p/\\d+/[^/]+\\.html|https?://[a-z0-9\\-]+\\.turboimg\\.net/t1/\\d+_[^/]+" })
public class TurboimagehostCom extends PluginForDecrypt {
    public TurboimagehostCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final Regex thumbnail = new Regex(param.getCryptedUrl(), "https?://[a-z0-9\\-]+\\.turboimg\\.net/t1/(\\d+)_([^/]+)");
        if (thumbnail.matches()) {
            /* Change thumbnail --> Normal URL --> Then we can crawl the fullsize URL. */
            final String newURL = "https://www." + this.getHost() + "/p/" + thumbnail.getMatch(0) + "/" + thumbnail.getMatch(1) + ".html";
            param.setCryptedUrl(newURL);
        }
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        final String finallink = this.br.getRegex("\"(https?://s\\d+d\\d+\\.(?:turboimagehost\\.com|turboimg\\.net)/sp/[a-z0-9]+/.*?)\"").getMatch(0);
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
