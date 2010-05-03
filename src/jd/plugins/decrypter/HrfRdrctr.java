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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "forex-fileupload.co.cc", "crazytr.com", "url.tr59.info", "url.bilgiportal.com", "freakinghugeurl.com", "1lik.net", "linkpayout.com" }, urls = { "http://[\\w\\.]*?forex-fileupload\\.co\\.cc/\\?\\w+", "http://[\\w\\.]*?crazytr\\.com/url/\\d+", "http://[\\w\\.]*?url\\.tr59\\.info/[0-9]+", "http://[\\w\\.]*?url\\.bilgiportal\\.com/[0-9]+", "http://[\\w\\.]*?freakinghugeurl\\.com/refer\\.php\\?count=[0-9]+\\&url=[a-zA-Z0-9]+", "http://[\\w\\.]*?1lik\\.net/[0-9]+", "http://[\\w\\.]*?linkpayout\\.com/[0-9a-z]+" }, flags = { 0, 0, 0, 0, 0, 0, 0 })
public class HrfRdrctr extends PluginForDecrypt {

    /* Usage: {{regex, getMatch()-Index}, {..., ...}} */
    Object[][] regxps = { { "content=\".*?url=(.*?)\"", 0 }, { "<a href=\"(http://.*?)\"><img src=\".*?/(aa\\.png|dwn_btn\\.gif)\"></a>", 0 }, { "link\" value=\"(http.*?)\">", 0 }, { "redirect\\('(.*?)'\\)", 0 }, { "</script.*?href=\"(http.*?)\"", 0 }, { "marginheight=.*?src=\"(.*?)\"", 0 }, { "name=\"page.*?src=\"(.*?)\"", 0 } };

    public HrfRdrctr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String link = null;
        String parameter = param.toString();
        br.getPage(parameter);
        for (int i = 0; i < regxps.length; i++) {
            if (link == null) {
                link = br.getRegex((String) regxps[i][0]).getMatch((Integer) regxps[i][1]);
            } else {
                break;
            }
        }
        if (link == null) return null;
        decryptedLinks.add(createDownloadlink(link));
        return decryptedLinks;
    }

}