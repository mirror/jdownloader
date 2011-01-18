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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xfileseek.com" }, urls = { "http://[\\w\\.]*?xfileseek\\.com/hotfiles\\.html\\?id=[a-z0-9]+" }, flags = { 0 })
public class XfleSkCom extends PluginForDecrypt {

    public XfleSkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("Sorry, we couldn't find the page you're looking for")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpname = br.getRegex("<h2>(.*?)</font><br> download link").getMatch(0);
        if (fpname == null) fpname = br.getRegex("&q=(.*?)\"").getMatch(0);
        if (br.containsHTML("/captcha/")) {
            for (int i = 0; i <= 5; i++) {
                String captchaurl = br.getRegex("<br>enter code on image<br>.*?<img src=\"(/.*?captcha/.*?)\">").getMatch(0);
                if (captchaurl == null) captchaurl = br.getRegex("\"(/images/captcha/.*?)\"").getMatch(0);
                if (captchaurl == null) return null;
                captchaurl = "http://xfileseek.com" + captchaurl;
                String id = new Regex(parameter, "id=([a-z0-9]+)").getMatch(0);
                String imghash = new Regex(captchaurl, "captcha/(.*?)\\.").getMatch(0);
                if (imghash == null) return null;
                String code = getCaptchaCode(captchaurl, param);
                br.getPage("http://xfileseek.com/hotfiles.html?imgtext=" + code + "&id=" + id + "&imghash=" + imghash + "&submit=ok");
                if (br.containsHTML("/captcha/")) continue;
                break;
            }
            if (br.containsHTML("/captcha/")) throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        String[] finallinks = br.getRegex("<li>(http.*?)</li></a>").getColumn(0);
        if (finallinks == null || finallinks.length == 0) return null;
        for (String finallink : finallinks)
            decryptedLinks.add(createDownloadlink(finallink));
        if (fpname != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpname.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}