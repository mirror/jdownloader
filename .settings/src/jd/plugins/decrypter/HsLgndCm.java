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
import jd.config.Property;
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

@DecrypterPlugin(revision = "$Revision: 16228 $", interfaceVersion = 2, names = { "houselegend.com" }, urls = { "http://[\\w\\.]*?houselegend\\.com/(([0-9]+.+)|(redirector.+))" }, flags = { 0 })
public class HsLgndCm extends PluginForDecrypt {

    private Pattern             sitePattern       = Pattern.compile("target='_blank' href='(.*?houselegend\\.com/redirector\\.php\\?url=.*?)'>", Pattern.CASE_INSENSITIVE);
    private Pattern             redirectorPattern = Pattern.compile("click on the link <a href=\"(http://.*?)\">", Pattern.CASE_INSENSITIVE);
    /* must be static so all plugins share same lock */
    private static final Object LOCK              = new Object();

    public HsLgndCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookiesExclusive(true);
        String[] links = null;
        br.getPage(parameter);
        if (parameter.contains("redirector.php?")) {
            links = new String[] { parameter };
        } else {
            if (!getUserLogin(parameter)) {
                logger.info("Invalid logindata!");
                return decryptedLinks;
            }
            links = br.getRegex(sitePattern).getColumn(0);
        }
        if (links == null || links.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (String link : links) {
            br.getPage(Encoding.htmlDecode(link));
            decryptedLinks.add(createDownloadlink(br.getRegex(redirectorPattern).getMatch(0)));
        }
        return decryptedLinks;

    }

    private boolean getUserLogin(String url) throws IOException, DecrypterException {
        String ltmp = null;
        String ptmp = null;
        synchronized (LOCK) {
            ltmp = this.getPluginConfig().getStringProperty("user", null);
            ptmp = this.getPluginConfig().getStringProperty("pass", null);
            if (ltmp != null && ptmp != null) {
                br.postPage(url, "login_name=" + ltmp + "&login_password=" + Encoding.urlEncode(ptmp) + "&no_cookies=1&image=LOGIN&login=submit");
            }
            br.getPage(url);
            for (int i = 0; i < 3; i++) {
                if (br.containsHTML("Registration\"><b>REGISTER</b></a> before you can view this text")) {
                    this.getPluginConfig().setProperty("user", Property.NULL);
                    this.getPluginConfig().setProperty("pass", Property.NULL);
                    ltmp = UserIO.getInstance().requestInputDialog("Enter Loginname for houselegend.com :");
                    if (ltmp == null) return false;
                    ptmp = UserIO.getInstance().requestInputDialog("Enter password for houselegend.com :");
                    if (ptmp == null) return false;
                    br.postPage(url, "login_name=" + ltmp + "&login_password=" + Encoding.urlEncode(ptmp) + "&no_cookies=1&image=LOGIN&login=submit");
                    br.getPage(url);
                } else {
                    this.getPluginConfig().setProperty("user", ltmp);
                    this.getPluginConfig().setProperty("pass", ptmp);
                    this.getPluginConfig().save();
                    return true;
                }

            }
        }
        throw new DecrypterException("Login or/and password wrong");
    }

}
