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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "desixnxx.net" }, urls = { "https?://(?:www\\.)?desixnxx\\.net/[a-z0-9\\-]{20,}/" })
public class DesixnxxNet extends PluginForDecrypt {
    public DesixnxxNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (jd.plugins.hoster.DesixnxxNet.isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* 2017-05-31: Usually openload.co URLs */
        final String finallink = this.br.getRegex("<iframe src=\"(http[^<>\"]+)\"").getMatch(0);
        if (finallink != null) {
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            /* Pass url to hosterplugin */
            final DownloadLink dl = createDownloadlink(parameter.replaceAll("https?://", "desixnxxdecrypted://"));
            dl.setName(jd.plugins.hoster.DesixnxxNet.getFilename(this.br, parameter));
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
}
