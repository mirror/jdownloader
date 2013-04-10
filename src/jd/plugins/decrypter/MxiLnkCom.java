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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "maxi-link.com" }, urls = { "http://[\\w\\.]*?maxi-link\\.com/[a-z0-9]+/.*?\\.html" }, flags = { 0 })
public class MxiLnkCom extends PluginForDecrypt {

    public MxiLnkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String CAPTCHATEXT = "/c/";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("class=\"size\">0 o</span>")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (!br.containsHTML(CAPTCHATEXT)) {
            logger.warning("Couldn't find the captchatext, stopping...");
            return null;
        }
        for (int i = 0; i <= 3; i++) {
            String captchaurl = br.getRegex("Please enter code :<br />[\t\n\r ]+<img src=\"(/.*?)\"").getMatch(0);
            if (captchaurl == null) captchaurl = br.getRegex("\"(/c/\\d+\\.png)\"").getMatch(0);
            if (captchaurl == null) return null;
            captchaurl = "http://en.maxi-link.com" + captchaurl;
            br.postPage(parameter, "open=Open&c=" + getCaptchaCode(captchaurl, param));
            if (br.containsHTML(CAPTCHATEXT)) continue;
            break;
        }
        if (br.containsHTML(CAPTCHATEXT)) throw new DecrypterException(DecrypterException.CAPTCHA);
        String finallink = br.getRegex(">Download : <a href=\"(.*?)\"").getMatch(0);
        if (finallink == null) finallink = br.getRegex("class=\"u\">(.*?)</a>").getMatch(0);
        if (finallink == null) {
            logger.warning("finallink could not be found...");
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}