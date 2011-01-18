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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kisaurl.com" }, urls = { "http://[\\w\\.]*?kisaurl\\.com/\\?\\d[A-Z]{2}" }, flags = { 0 })
public class KsRLCm extends PluginForDecrypt {

    public KsRLCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String linkurl = null;
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);

        if (br.getHost().equals("kisaurl.com") && !br.containsHTML("sayfa sistemde bulunamad.")) {
            Form form = br.getForm(0);
            if (form != null && form.hasInputFieldByName("field_password")) {
                int flag = 0;
                for (int i = 0; i < 5; i++) {
                    logger.info("pw protected link");
                    String password = getUserInput(null, param);
                    String request = br.postPage("includes/ajax.php", "password=" + password + "&action=checkpassword");
                    flag = Integer.parseInt(new Regex(request, "\\['(\\d)','.*?'\\]").getMatch(0));
                    String url = new Regex(request, "\\['\\d','(.*?)'\\]").getMatch(0);
                    if (flag == 1) {
                        br.getPage(url);
                        break;
                    }
                }
                if (flag == 0) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
            }
        }

        linkurl = br.getURL();

        if (linkurl == null) return null;

        decryptedLinks.add(createDownloadlink(linkurl));

        return decryptedLinks;
    }

    // @Override

}
