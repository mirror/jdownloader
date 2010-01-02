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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sexvideo.to" }, urls = { "http://[\\w\\.]*?sexvideo\\.to/(random|movies|clips|porn-images|dvd-r|hdtv|hentai|magazines|games)/.+" }, flags = { 0 })
public class SxVdT extends PluginForDecrypt {

    public SxVdT(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.containsHTML("(Download incorrect|You have request a wrong or old download-link)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String packagename = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
        if (packagename == null) packagename = br.getRegex("/search/(.*?)'").getMatch(0);
        for (int i = 0; i < 3; i++) {
            String captchaurl = br.getRegex("(captcha/.*?)\"").getMatch(0);
            if (captchaurl == null) return null;
            captchaurl = "http://sexvideo.to/img/" + captchaurl;
            String code = getCaptchaCode(captchaurl, param);
            br.postPage(param.getCryptedUrl(), "captcha=" + Encoding.htmlDecode(code) + "&submit=Submit%21&cname=&cemail=&ctext=");
            if (!br.containsHTML("captcha/")) break;
        }
        if (br.containsHTML("captcha/")) throw new DecrypterException(DecrypterException.CAPTCHA);
        String[] decryptedlinks = br.getRegex("<button name=\"\" type=\"button\" onclick=\"window\\.open\\('(.*?)'").getColumn(0);
        if (decryptedlinks == null || decryptedlinks.length == 0) return null;
        for (String decryptedlink : decryptedlinks) {
            DownloadLink dl = createDownloadlink(decryptedlink);
            dl.addSourcePluginPassword("sexvideo.to");
            decryptedLinks.add(dl);
        }
        if (packagename != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(packagename.trim());
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
