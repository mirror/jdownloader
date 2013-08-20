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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mymedia.yam.com" }, urls = { "http://(www\\.)?mymedia\\.yam\\.com/m/\\d+" }, flags = { 0 })
public class MymediaYamComDecrypter extends PluginForDecrypt {

    public MymediaYamComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setCustomCharset("utf-8");
        br.getPage(parameter);

        if (br.containsHTML("使用者影音平台存取發生錯誤<")) {
            final DownloadLink dl = createDownloadlink(parameter.replace("mymedia.yam.com/", "mymediadecrypted.yam.com/"));
            dl.setAvailable(false);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }

        String externID = br.getRegex("name=\"movie\" value=\"(http://(www\\.)?youtube\\.com/v/[A-Za-z0-9\\-_]+)\\&").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }

        final DownloadLink dl = createDownloadlink(parameter.replace("mymedia.yam.com/", "mymediadecrypted.yam.com/"));
        String filename = br.getRegex("class=\"heading\"><span style=\\'float:left;\\'>([^<>\"]*?)</span>").getMatch(0);
        if (filename != null) {
            dl.setName(Encoding.htmlDecode(filename.trim()));
            dl.setAvailable(true);
        }
        decryptedLinks.add(dl);

        return decryptedLinks;
    }

}
