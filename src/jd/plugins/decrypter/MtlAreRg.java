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
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "metalarea.org" }, urls = { "https?://(?:www\\.)?metalarea\\.org/forum/index\\.php\\?showtopic=([0-9]+)" })
public class MtlAreRg extends PluginForDecrypt {
    /* must be static so all plugins share same lock */
    private static Object LOCK = new Object();

    public MtlAreRg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setCookiesExclusive(false);
        br.setFollowRedirects(true);
        if (!getUserLogin(param.getCryptedUrl())) {
            logger.info("No- or wrong logindata entered!");
            throw new AccountRequiredException();
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = br.getRegex("<td width='99%' style='word-wrap:break-word;'>\\s*<div>\\s*<img[^>]*/>\\s*\\&nbsp;\\s*<b>\\s*(.*?)\\s*</div>\\s*</td>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>\\s*(.*?)\\s*</title>").getMatch(0);
            if (fpName != null) {
                fpName = fpName.replace(" - Metal Area - Extreme Music Portal", "");
            }
        }
        if (fpName != null) {
            fpName = fpName.replaceAll("(</b>(\\s*,)?)", "");
        }
        // Filter links in hide(s)
        final String htmls[] = br.getRegex("<\\!\\-\\-HideBegin\\-\\->(.*?)<\\!\\-\\-HideEnd\\-\\->").getColumn(0);
        if (htmls == null || htmls.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (String pagepiece : htmls) {
            String[] links = HTMLParser.getHttpLinks(pagepiece, "");
            if (links != null && links.length != 0) {
                for (String link : links) {
                    ret.add(createDownloadlink(link));
                }
            }
        }
        if (fpName != null) {
            fpName = Encoding.htmlDecode(fpName).trim();
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(ret);
        }
        return ret;
    }

    private boolean getUserLogin(final String url) throws IOException, DecrypterException {
        synchronized (LOCK) {
            String logincookie = this.getPluginConfig().getStringProperty("masession_id");
            if (logincookie != null) {
                br.setCookie(this.getHost(), "masession_id", logincookie);
                br.getPage(url);
                if (this.isLoggedIN(br)) {
                    logger.info("Cookie login successful");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                    this.br.clearAll();
                }
            }
            logger.info("Full login required");
            String username = this.getPluginConfig().getStringProperty("user", null);
            String password = this.getPluginConfig().getStringProperty("pass", null);
            if (username == null || password == null) {
                username = UserIO.getInstance().requestInputDialog("Enter Loginname for metalarea.org :");
                if (username == null) {
                    return false;
                }
                password = UserIO.getInstance().requestInputDialog("Enter password for metalarea.org :");
                if (password == null) {
                    return false;
                }
            }
            br.getPage("https://" + this.getHost() + "/forum/index.php?act=Login");
            br.postPage("/forum/index.php?act=Login&CODE=01", "UserName=" + Encoding.urlEncode(username) + "&PassWord=" + Encoding.urlEncode(password) + "&CookieDate=1");
            logincookie = br.getCookie(br.getHost(), "masession_id", Cookies.NOTDELETEDPATTERN);
            if (logincookie != null && this.isLoggedIN(br)) {
                logger.info("Full login successful");
                this.getPluginConfig().setProperty("user", username);
                this.getPluginConfig().setProperty("pass", password);
                this.getPluginConfig().setProperty("masession_id", password);
                this.getPluginConfig().save();
                br.getPage(url);
                return true;
            } else {
                /* Clear logindata so that we can ask user again the next time */
                logger.info("Full login failed");
                this.getPluginConfig().setProperty("user", Property.NULL);
                this.getPluginConfig().setProperty("pass", Property.NULL);
                this.getPluginConfig().setProperty("masession_id", Property.NULL);
                this.getPluginConfig().save();
                return false;
            }
        }
    }

    boolean isLoggedIN(final Browser br) {
        /* Check if user control-panel is visible. */
        return br.containsHTML("id=\"userlinks\"");
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}