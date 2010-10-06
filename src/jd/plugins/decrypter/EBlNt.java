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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "e-bol.net" }, urls = { "http://[\\w\\.]*?e-bol\\.net/.*/" }, flags = { 0 })
public class EBlNt extends PluginForDecrypt {

    public EBlNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">Sorry, the page your requested could not be found, or no longer exists\\. </div>")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String theID = br.getRegex("id=\"post-ratings-(\\d+)\"").getMatch(0);
        if (theID == null) return null;
        progress.setRange(3);
        for (int i = 0; i <= 3; i++) {
            String postData = "Download%5B" + i + "%5D.x=&Download%5B" + i + "%5D.y=&p_id=" + theID;
            br.postPage("http://e-bol.net/wp-content/themes/Basic/download.php", postData);
            String finallink = br.getRegex("name=\"Download\">-->[\t\n\r ]+<frame src=\"(.*?)\"").getMatch(0);
            if (finallink != null) decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) return null;
        return decryptedLinks;
    }

}
