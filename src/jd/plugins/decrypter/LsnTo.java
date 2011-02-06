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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lesen.to" }, urls = { "http://(www\\.)?lesen\\.to/protection/folder_\\d+\\.html" }, flags = { 0 })
public class LsnTo extends PluginForDecrypt {

    public LsnTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String RECAPTCHA = "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        boolean failed = true;
        for (int i = 0; i <= 5; i++) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, param);
            rc.setCode(c);
            if (br.containsHTML(RECAPTCHA)) {
                br.getPage(parameter);
                continue;
            }
            failed = false;
            break;
        }
        if (failed) throw new DecrypterException(DecrypterException.CAPTCHA);
        if (br.containsHTML("Anfrage abgefangen")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String[] links = br.getRegex("class=\"container\"><a href=\"(http://.*?)\"").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("\"(http://(www\\.)?lesen\\.to/protection/.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String singleLink : links) {
            br.getPage(singleLink);
            String finallink = br.getRedirectLocation();
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }

        return decryptedLinks;
    }

}
