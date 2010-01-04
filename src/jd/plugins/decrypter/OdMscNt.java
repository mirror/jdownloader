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
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "odimusic.net" }, urls = { "http://[\\w\\.]*?odimusic\\.net/download/(music/.*?\\.html|engine/go\\.php\\?url=[a-zA-Z0-9% ]+)" }, flags = { 0 })
public class OdMscNt extends PluginForDecrypt {

    /* must be static so all plugins share same lock */
    private static final Object LOCK = new Object();

    public OdMscNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(false);
        if (!getUserLogin(parameter)) return null;
        if (!parameter.contains("engine/go.php?")) {
            if (br.containsHTML("(An error occurred|We're sorry for the inconvenience)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            String pagepiece = br.getRegex("news-id-(.*?)<div class=\"rating\" style=\"float: left;\">").getMatch(0);
            if (pagepiece == null) pagepiece = br.getRegex("<\\!--TBegin-->(.*?)</a></b> </div>").getMatch(0);
            if (pagepiece == null && br.containsHTML("Make A Small Donation To Have Acces")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            if (pagepiece == null) return null;
            String[] links = HTMLParser.getHttpLinks(pagepiece, "");
            if (links == null || links.length == 0) return null;
            for (String link : links) {
                // Handling for redirect-links
                if (link.contains("/download/engine/go.php")) {
                    br.getPage(link);
                    link = br.getRedirectLocation();
                    if (link == null) return null;
                    decryptedLinks.add(createDownloadlink(link));
                } else {
                    // Handling for normal links (plainlinks)
                    if (!link.contains("odimusic.net") && !link.contains("imageshack.us")) decryptedLinks.add(createDownloadlink(link));
                }
            }
        } else {
            br.getPage(parameter);
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
        }
        return decryptedLinks;

    }

    private boolean getUserLogin(String url) throws IOException, DecrypterException {
        String ltmp = null;
        String ptmp = null;
        if (url.contains("engine/go.php?")) url = "http://odimusic.net/download/index.php";
        synchronized (LOCK) {
            ltmp = this.getPluginConfig().getStringProperty("user", null);
            ptmp = this.getPluginConfig().getStringProperty("pass", null);
            if (ltmp != null && ptmp != null) {
                br.postPage(url, "login_name=" + Encoding.urlEncode(ltmp) + "&login_password=" + Encoding.urlEncode(ptmp) + "&image=Login&login=submit");
            }
            br.getPage(url);
            for (int i = 0; i < 3; i++) {
                if (br.getCookie("http://odimusic.net/", "dle_user_id") == null || br.getCookie("http://odimusic.net/", "dle_password") == null || br.containsHTML("We noticed that you are not registered")) {
                    this.getPluginConfig().setProperty("user", Property.NULL);
                    this.getPluginConfig().setProperty("pass", Property.NULL);
                    ltmp = UserIO.getInstance().requestInputDialog("Enter Loginname for odimusic.net :");
                    if (ltmp == null) return false;
                    ptmp = UserIO.getInstance().requestInputDialog("Enter password for odimusic.net :");
                    if (ptmp == null) return false;
                    br.postPage(url, "login_name=" + Encoding.urlEncode(ltmp) + "&login_password=" + Encoding.urlEncode(ptmp) + "&image=Login&login=submit");
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
