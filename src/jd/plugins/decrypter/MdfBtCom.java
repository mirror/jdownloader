//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mediafirebot.com" }, urls = { "http://[\\w\\.]*?mediafirebot\\.com/m/file\\.php\\?file_id=\\d+" }, flags = { 0 })
public class MdfBtCom extends PluginForDecrypt {

    public MdfBtCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final Object LOCK = new Object();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        String fileID = new Regex(parameter, "mediafirebot\\.com/m/file\\.php\\?file_id=(\\d+)").getMatch(0);
        if (fileID == null) return null;
        // This host doesn't like simultan captcha requests
        synchronized (LOCK) {
            br.getPage(parameter);
            if (br.containsHTML("captcha\\.php")) {
                boolean failed = true;
                for (int i = 0; i <= 3; i++) {
                    String code = getCaptchaCode("http://mediafirebot.com/captcha.php", param);
                    br.postPage("http://mediafirebot.com/captchacontrol.php", "captcha=" + code + "&file_id=727069&submit_com=Submit");
                    if (br.containsHTML(">Please enter the correct security code")) {
                        br.getPage(parameter);
                        continue;
                    }
                    failed = false;
                    break;
                }
                if (failed) throw new DecrypterException(DecrypterException.CAPTCHA);
            }
        }
        // Errorhandling for invalid links
        if (br.containsHTML("supplied argument is not a valid MySQL result resource in")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String finallink = br.getRegex("downloading the below file.</div><h2><a href=\"(.*?)\"").getMatch(0);
        if (finallink == null) finallink = br.getRegex(">DOWNLOAD  HERE</a></h2><br><br><a href=\"(.*?)\"").getMatch(0);
        if (finallink == null) return null;
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
