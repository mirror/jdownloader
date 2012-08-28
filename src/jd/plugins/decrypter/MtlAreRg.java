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

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "metalarea.org" }, urls = { "http://[\\w\\.]*?metalarea\\.org/forum/index\\.php\\?showtopic=[0-9]+" }, flags = { 0 })
public class MtlAreRg extends PluginForDecrypt {

    private static final String HOST = "http://metalarea.org";
    /* must be static so all plugins share same lock */
    private static final Object LOCK = new Object();

    public MtlAreRg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookiesExclusive(false);
        br.setFollowRedirects(false);
        if (!getUserLogin(parameter)) {
            logger.info("No or wrong logindata entered!");
            return decryptedLinks;
        }
        // Filter links in hide(s)
        String pagepieces[] = br.getRegex("<\\!\\-\\-HideBegin\\-\\->(.*?)<\\!\\-\\-HideEnd\\-\\->").getColumn(0);
        if (pagepieces == null || pagepieces.length == 0) return null;
        for (String pagepiece : pagepieces) {
            String[] links = HTMLParser.getHttpLinks(pagepiece, "");
            if (links != null && links.length != 0) {
                for (String link : links)
                    decryptedLinks.add(createDownloadlink(link));

            }
        }
        return decryptedLinks;

    }

    private boolean getUserLogin(String url) throws IOException, DecrypterException {
        String username = null;
        String password = null;
        synchronized (LOCK) {
            username = this.getPluginConfig().getStringProperty("user", null);
            password = this.getPluginConfig().getStringProperty("pass", null);
            if (username != null && password != null) {
                br.postPage("http://metalarea.org/forum/index.php?act=Login&CODE=01", "UserName=" + Encoding.urlEncode(username) + "&PassWord=" + Encoding.urlEncode(password) + "&CookieDate=1");
            }
            br.getPage(url);
            for (int i = 0; i < 3; i++) {
                if (br.getCookie(HOST, "ma_tr_pass") == null || br.getCookie(HOST, "ma_tr_uid") == null) {
                    this.getPluginConfig().setProperty("user", Property.NULL);
                    this.getPluginConfig().setProperty("pass", Property.NULL);
                    username = UserIO.getInstance().requestInputDialog("Enter Loginname for metalarea.org :");
                    if (username == null) return false;
                    password = UserIO.getInstance().requestInputDialog("Enter password for metalarea.org :");
                    if (password == null) return false;
                    br.postPage("http://metalarea.org/forum/index.php?act=Login&CODE=01", "UserName=" + Encoding.urlEncode(username) + "&PassWord=" + Encoding.urlEncode(password) + "&CookieDate=1");
                } else {
                    this.getPluginConfig().setProperty("user", username);
                    this.getPluginConfig().setProperty("pass", password);
                    this.getPluginConfig().save();
                    return true;
                }

            }
        }
        return false;
    }

}
