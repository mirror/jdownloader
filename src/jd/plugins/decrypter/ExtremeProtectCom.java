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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "extreme-protect.com" }, urls = { "http://[\\w\\.]*?extreme-protect\\.com/(linkcheck|linkidwoc)\\.php\\?linkid=[a-z]+" }, flags = { 0 })
public class ExtremeProtectCom extends PluginForDecrypt {

    public ExtremeProtectCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String RECAPTCHATEXT  = "api\\.recaptcha\\.net";
    private static final String RECAPTCHATEXT2 = "google\\.com/recaptcha/api/challenge";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("linkidwoc", "linkcheck");
        br.getPage(parameter);
        boolean failed = true;
        for (int i = 0; i <= 5; i++) {
            if (!br.containsHTML(RECAPTCHATEXT) && !br.containsHTML(RECAPTCHATEXT2)) return null;
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, param);
            rc.setCode(c);
            if (br.containsHTML("(The security code is <font color=\\'red\\'>incorrect</font>|The CAPTCHA wasn\\'t entered correctly)")) {
                br.getPage(parameter);
                continue;
            }
            failed = false;
            break;
        }
        if (failed) throw new DecrypterException(DecrypterException.CAPTCHA);
        if (!br.containsHTML("Title:")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("<td style=\\'border:1px;font\\-weight:bold;font\\-size:90%;font\\-family:Arial,Helvetica,sans-serif;\\'>(.*?)</td>").getMatch(0);
        String[] links = br.getRegex("target=_blank>(.*?)</a>").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
