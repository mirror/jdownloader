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

import java.io.IOException;
import java.util.ArrayList;

import java.util.regex.Pattern;
import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {"houselegend.com"}, urls = {"http://[\\w\\.]*?houselegend\\.com/[0-9]+.+"}, flags = {0})
public class HsLgndCm extends PluginForDecrypt {

    private Pattern sitePattern = Pattern.compile("target='_blank' href='(.*?houselegend\\.com/redirector\\.php\\?url=.*?)'>", Pattern.CASE_INSENSITIVE);
    private Pattern redirectorPattern = Pattern.compile("click on the link <a href=\"(http://.*?)\">", Pattern.CASE_INSENSITIVE);
    private String cookie;

    public HsLgndCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookiesExclusive(false);
        br.getPage(parameter);
        for (int i = 0; i < 4; i++) {
            if (br.containsHTML("Registration\"><b>REGISTER</b></a> before you can view this text")) {
                getUserLogin(parameter);
            } else {
                String[] links = br.getRegex(sitePattern).getColumn(0);
                if (links == null || links.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                for (String link : links) {
                    br.getPage(link);
                    decryptedLinks.add(createDownloadlink(br.getRegex(redirectorPattern).getMatch(0)));
                }
            }
        }
        throw new DecrypterException("Login or/and password wrong");
    }

    private void getUserLogin(String url) throws IOException {
        String login = UserIO.getInstance().requestInputDialog("Enter Loginname for houselegend.com :");
        String pass = UserIO.getInstance().requestInputDialog("Enter password for houselegend.com :");
        cookie = "login_name=" + login + "&login_password=" + Encoding.urlEncode(pass) + "&no_cookies=1&image=LOGIN&login=submit";
        br.postPage(url, cookie);
        br.getPage(url);
    }

}
