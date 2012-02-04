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
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "adultddl.com" }, urls = { "http://(www\\.)?adultddl\\.com/\\d{4}/\\d{2}/\\d{2}/[^<>\"\\'/]+" }, flags = { 0 })
public class AdltDlCom extends PluginForDecrypt {

    public AdltDlCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String fpName = br.getRegex(" title=\"Comment on ([^<>\"\\']+)\"").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>([^<>\"\\']+) \\| AdultDDL</title>").getMatch(0);
        final String streamLink = br.getRegex("\\'(http://(www\\.)?putlocker\\.com/embed/[A-Z0-9]+)\\'").getMatch(0);
        if (streamLink != null) decryptedLinks.add(createDownloadlink(streamLink));
        final String captchaSite = br.getRegex("\\'(http://secure\\.adultddl\\.com/\\?decrypt=\\d+)\\'").getMatch(0);
        if (captchaSite == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        for (int i = 0; i <= 5; i++) {
            br.getPage(captchaSite);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, param);
            rc.setCode(c);
            if (br.containsHTML("(The CAPTCHA wasn\\'t entered correctly\\. Please go back and try again|incorrect\\-captcha\\-sol)")) continue;
            break;
        }
        if (br.containsHTML("(The CAPTCHA wasn\\'t entered correctly\\. Please go back and try again|incorrect\\-captcha\\-sol)")) throw new DecrypterException(DecrypterException.CAPTCHA);
        String[] links = HTMLParser.getHttpLinks(br.toString(), "");
        if ((links == null || links.length == 0) && streamLink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (links != null && links.length != 0) {
            for (String singleLink : links)
                if (!singleLink.contains("adultddl.com/")) decryptedLinks.add(createDownloadlink(singleLink));
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
