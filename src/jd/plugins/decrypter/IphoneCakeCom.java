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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "iphonecake.com" }, urls = { "http://(www\\.)?iphonecake\\.com/appcake/dl\\.php\\?dlid=\\d+\\&id=\\d+" }, flags = { 0 })
public class IphoneCakeCom extends PluginForDecrypt {

    public IphoneCakeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        String dllink = null;
        for (int i = 1; i <= 3; i++) {
            final String code = getCaptchaCode("http://iphonecake.com/appcake/secimage.php", param);
            br.postPage(br.getURL(), "seccode=" + Encoding.urlEncode(code.toUpperCase()));
            dllink = br.getRegex("http\\-equiv=\"refresh\" content=\"\\d+;URL=(http[^<>\"]*?)\"").getMatch(0);
            if (dllink != null) break;
        }
        decryptedLinks.add(createDownloadlink(dllink));

        return decryptedLinks;
    }

}
