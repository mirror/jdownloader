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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wittytv.it" }, urls = { "https?://(?:www\\.)?wittytv\\.it/[^/]+/([^/]+/)?.+" })
public class WittytvIt extends PluginForDecrypt {
    public WittytvIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        final String guid = br.getRegex("var currentGUID = \"(FD\\d+)\";").getMatch(0);
        if (guid != null) {
            /* 2021-06-14: New handling */
            final DownloadLink dl = createDownloadlink("https://www.mediasetplay.mediaset.it/video/dummy/dummy2_" + guid);
            dl.setContentUrl(param.getCryptedUrl());
            decryptedLinks.add(dl);
        } else {
            /* Old handling (or invalid URL/content offline) */
            final DownloadLink dl = createDownloadlink(param.getCryptedUrl());
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
}
