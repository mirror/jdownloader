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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "musicore.net" }, urls = { "http://[\\w\\.]*?musicore\\.net/(forums/index\\.php\\?/topic/\\d+-|\\?id=.*?\\&url=[a-zA-Z0-9=+/\\-]+)" }, flags = { 0 })
public class MsCreNt extends PluginForDecrypt {

    /* must be static so all plugins share same lock */
    private static final Object LOCK = new Object();

    public MsCreNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replace("amp;", "");
        br.setCookiesExclusive(true);
        synchronized (LOCK) {
            if (!getUserLogin(parameter)) return null;
            br.setFollowRedirects(false);
            // Access the page
            if (!br.getURL().equals(parameter)) br.getPage(parameter);
            if (!parameter.contains("musicore.net/forums/index.php?")) {
                if (br.containsHTML("(An error occurred|We're sorry for the inconvenience)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
                String finallink = br.getRedirectLocation();
                if (finallink == null) {
                    logger.warning("finallink from the following link had to be regexes and could not be found by the direct redirect: " + parameter);
                    finallink = br.getRegex("URL=(.*?)\"").getMatch(0);
                }
                if (finallink == null) {
                    logger.warning("Decrypter is defect, browser contains: " + br.toString());
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            } else {
                String fpName = br.getRegex("<title>(.*?)- musiCore Forums</title>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("<h1>musiCore Forums:(.*?)- musiCore Forums").getMatch(0);
                    if (fpName == null) {
                        fpName = br.getRegex("class='main_topic_title'>(.*?)<span class=").getMatch(0);
                    }
                }
                String redirectlinks[] = br.getRegex("'(http://r\\.musicore\\.net/\\?id=.*?url=.*?)'").getColumn(0);
                if (redirectlinks == null || redirectlinks.length == 0) return null;
                progress.setRange(redirectlinks.length);
                for (String redirectlink : redirectlinks) {
                    redirectlink = redirectlink.replace("amp;", "");
                    br.getPage(redirectlink);
                    String finallink = br.getRedirectLocation();
                    if (finallink == null) finallink = br.getRegex("URL=(.*?)\"").getMatch(0);
                    if (finallink == null) {
                        logger.warning("finallink from the following link had to be regexes and could not be found by the direct redirect: " + parameter);
                        logger.warning("Browser contains test: " + br.toString());
                        finallink = br.getRegex("URL=(.*?)\"").getMatch(0);
                    }
                    decryptedLinks.add(createDownloadlink(finallink));
                    progress.increase(1);
                }
                if (fpName != null) {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName.trim());
                    fp.addLinks(decryptedLinks);
                }
            }
            return decryptedLinks;
        }

    }

    private boolean getUserLogin(String url) throws IOException, DecrypterException {
        br.setFollowRedirects(true);
        // br.getPage(url);
        String username = null;
        String password = null;
        br.getPage("http://musicore.net/forums/index.php?app=core&module=global&section=login");
        synchronized (LOCK) {
            username = this.getPluginConfig().getStringProperty("user", null);
            password = this.getPluginConfig().getStringProperty("pass", null);
            if (username != null && password != null) {
                br.postPage("http://musicore.net/forums/index.php?app=core&module=global&section=login&do=process", "referer=" + Encoding.urlEncode(url) + "&username=" + Encoding.urlEncode(username) + "&password=" + Encoding.urlEncode(password) + "&rememberMe=1");
            }
            for (int i = 0; i < 3; i++) {
                if (br.getCookie("http://musicore.net/", "pass_hash") == null || br.getCookie("http://musicore.net/", "pass_hash").equals("0") || br.getCookie("http://musicore.net/", "member_id") == null || br.getCookie("http://musicore.net/", "member_id").equals("0")) {
                    this.getPluginConfig().setProperty("user", Property.NULL);
                    this.getPluginConfig().setProperty("pass", Property.NULL);
                    username = UserIO.getInstance().requestInputDialog("Enter Loginname for musicore.net :");
                    if (username == null) return false;
                    password = UserIO.getInstance().requestInputDialog("Enter password for musicore.net :");
                    if (password == null) return false;
                    br.postPage("http://musicore.net/forums/index.php?app=core&module=global&section=login&do=process", "referer=" + Encoding.urlEncode(url) + "&username=" + Encoding.urlEncode(username) + "&password=" + Encoding.urlEncode(password) + "&rememberMe=1");
                    if (!br.getURL().equals(url)) br.getPage(url);
                } else {
                    this.getPluginConfig().setProperty("user", username);
                    this.getPluginConfig().setProperty("pass", password);
                    this.getPluginConfig().save();
                    return true;
                }

            }
        }
        throw new DecrypterException("Login or/and password wrong");
    }

}
