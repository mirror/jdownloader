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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hides.at" }, urls = { "http://(www\\.)?hides\\.at/[a-z0-9]+" }, flags = { 0 })
public class HidsAt extends PluginForDecrypt {

    public HidsAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String CAPTCHATEXT = "securimage-1\\.0\\.3\\.1/securimage_show\\.php";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("Error loading list or invalid list")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (!br.containsHTML(CAPTCHATEXT)) return null;
        String linkID = new Regex(parameter, "hides\\.at/(.+)").getMatch(0);
        for (int i = 0; i <= 5; i++) {
            String code = getCaptchaCode("http://hides.at/include/securimage-1.0.3.1/securimage_show.php", param);
            br.getPage("http://hides.at/" + linkID + "?captcha_code=" + code + "&hash=" + linkID + "&btnSubmit=Submit");
            if (br.containsHTML(CAPTCHATEXT)) continue;
            break;
        }
        if (br.containsHTML(CAPTCHATEXT)) throw new DecrypterException(DecrypterException.CAPTCHA);
        String list = br.getRegex("id=\"list2Copy\" style=\"display: none;\">(.*?)</div>").getMatch(0);
        if (list == null) return null;
        String[] links = HTMLParser.getHttpLinks(list, "");
        if (links == null || links.length == 0) return null;
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));
        return decryptedLinks;
    }

}
