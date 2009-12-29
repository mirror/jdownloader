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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "load2all.com" }, urls = { "http://[\\w\\.]*?load2all\\.com/files/[0-9A-Z]+/[\\w.-]+" }, flags = { 0 })
public class Load2AllCom extends PluginForDecrypt {

    public Load2AllCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replace("files", "links").replace(".html", "");
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("No htmlCode read")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String[] redirectLinks = br.getRegex("(/redirect/[0-9A-Z]+/.*?/[0-9]+)").getColumn(0);
        if (redirectLinks.length == 0) return null;
        progress.setRange(redirectLinks.length);
        for (String link : redirectLinks) {
            link = "http://www.load2all.com" + link;
            br.getPage(link);
            if (br.containsHTML("Warning")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            if (!br.containsHTML("gkod.php")) return null;
            for (int i = 0; i <= 8; i++) {
                String captchaurl = "http://www.load2all.com/gkod.php?hash=" + Math.random();
                String code = getCaptchaCode(captchaurl, param);
                br.postPage(link, "gcode=" + Encoding.urlEncode(code) + "&x=0&y=0");
                if (!br.containsHTML("dotted #bbb;padding")) continue;
                break;
            }
            if (!br.containsHTML("dotted #bbb;padding")) throw new DecrypterException(DecrypterException.CAPTCHA);
            String finallink = br.getRegex("dotted #bbb;padding.*?<a href=\"(.*?)\"").getMatch(0);
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }

        return decryptedLinks;
    }

}