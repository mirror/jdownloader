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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "divxmotion.com" }, urls = { "http://(www\\.)?divxmotion\\.com/\\d{4}/\\d{2}/[a-z0-9\\-]+\\.html" }, flags = { 0 })
public class DivxMotionCom extends PluginForDecrypt {

    public DivxMotionCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String externID = br.getRegex("nowvideo\\.(eu|co)/embed\\.php\\?v=([a-z0-9]+)").getMatch(1);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.nowvideo.eu/video/" + externID));
            return decryptedLinks;
        }
        // Works for example for xvidstage.com
        externID = br.getRegex(Pattern.compile("<IFRAME SRC=\"(http[^<>\"]*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        return decryptedLinks;
    }

}
