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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mygirlfriendvids.net" }, urls = { "http://(www\\.)?mygirlfriendvids\\.net/\\d+/.*?\\.html" }) 
public class MyGirlfriendsVidsNetDecrypt extends PluginForDecrypt {

    public MyGirlfriendsVidsNetDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        final String parameter = param.toString();
        /* Sometimes it randomly fails ... this is a small workaround! */
        int counter = 0;
        do {
            br.getPage(parameter);
            final String tempID = br.getRedirectLocation();
            if (tempID != null) {
                DownloadLink dl = createDownloadlink(tempID);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            counter++;
        } while (this.br.containsHTML("Unable to read session") && counter <= 4);
        final DownloadLink dl = createDownloadlink(parameter.replace("mygirlfriendvids.net/", "mygirlfriendvidsdecrypted.net/"));
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}