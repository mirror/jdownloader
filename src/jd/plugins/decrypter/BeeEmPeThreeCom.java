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

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "beemp3.com" }, urls = { "http://(www\\.)?beemp3\\.com/download\\.php\\?file=\\d+\\&song=.+" }, flags = { 0 })
public class BeeEmPeThreeCom extends PluginForDecrypt {

    public BeeEmPeThreeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final Object  LOCK         = new Object();
    private static final String CAPTCHAREGEX = "\"(code\\.php\\?par=\\d+)\"";
    private static final String DONE         = "Done#\\|#";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("(<TITLE>404 Not Found</TITLE>|<H1>Not Found</H1>)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String captchaUrl = null;
        boolean failed = true;
        String fileID = new Regex(parameter, "beemp3\\.com/download\\.php\\?file=(\\d+)").getMatch(0);
        for (int i = 0; i <= 5; i++) {
            captchaUrl = br.getRegex(CAPTCHAREGEX).getMatch(0);
            if (captchaUrl == null) return null;
            captchaUrl = "http://beemp3.com/" + captchaUrl;
            String code = getCaptchaCode(captchaUrl, param);
            br.getPage("http://beemp3.com/chk_cd.php?id=" + fileID + "&code=" + code);
            if (!br.containsHTML(DONE)) {
                br.clearCookies("http://beemp3.com");
                br.getPage(parameter);
                continue;
            }
            failed = false;
            break;
        }
        if (failed) throw new DecrypterException(DecrypterException.CAPTCHA);
        String finallink = br.getRegex("Done#\\|#(http://.*?\\.mp3)").getMatch(0);
        if (finallink == null) return null;
        decryptedLinks.add(createDownloadlink("directhttp://" + finallink.trim()));

        return decryptedLinks;
    }
}
