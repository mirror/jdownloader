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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "shrink-service.it" }, urls = { "https?://(?:www\\.)?shrink-service\\.it/s/[A-Za-z0-9]+|https?://get\\.shrink-service\\.it/[A-Za-z0-9]+" })
public class ShrinkServiceIt extends PluginForDecrypt {

    public ShrinkServiceIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String linkid = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        br.setFollowRedirects(true);
        br.getPage(String.format("http://%s/s/%s", this.getHost(), linkid));
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String finallink = br.getRegex("<input type='hidden'[^<>\">]*?value='([^<>\"']*?)'>").getMatch(0);
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        } else if (finallink.equals("")) {
            /* Empty field --> Offline/invalid url */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        finallink = Encoding.htmlOnlyDecode(finallink);
        finallink = finallink.replace("&sol;", "/");
        finallink = finallink.replace("&colon;", ":");
        finallink = finallink.replace("&period;", ".");
        finallink = finallink.replace("&quest;", "?");
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
