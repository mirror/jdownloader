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
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protected.socadvnet.com" }, urls = { "http://[\\w\\.]*?protected\\.socadvnet\\.com/\\?[a-z0-9-]+" }, flags = { 0 })
public class PrtctdScdvntCm extends PluginForDecrypt {

    private static String MAINPAGE = "http://protected.socadvnet.com/";

    public PrtctdScdvntCm(final PluginWrapper wrapper) {
        super(wrapper);
    }

    // At the moment this decrypter only decrypts turbobit.net links as
    // "protected.socadvnet.com" only allows crypting links of this host!
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        this.br.setDebug(true);
        final String parameter = param.toString();
        this.br.setFollowRedirects(false);
        this.br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        final String postvar = new Regex(parameter, "protected\\.socadvnet\\.com/\\?(.+)").getMatch(0);
        if (postvar == null) { return null; }
        this.br.getPage(parameter);
        if ((this.br.getRedirectLocation() != null) && this.br.getRedirectLocation().equals(PrtctdScdvntCm.MAINPAGE + "index.php")) { throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore.")); }
        if (this.br.getRedirectLocation() != null) {
            this.br.getPage(this.br.getRedirectLocation());
        }

        final String cpPage = this.br.getRegex("<img src=\"(.*?)\"").getMatch(0);
        final Browser ajax = this.br;
        String sendCaptcha = this.br.getRegex("src='/(.*?)'").getMatch(0);
        if (sendCaptcha == null) { return null; }
        ajax.getPage(PrtctdScdvntCm.MAINPAGE + sendCaptcha);
        sendCaptcha = ajax.getRegex("req\\._POST\\(\"/(.*?)\\?res_code=").getMatch(0);

        this.br.postPage(PrtctdScdvntCm.MAINPAGE + "allinks.php", "LinkName=" + postvar);
        final String[] linksCount = this.br.getRegex("(moc\\.tenvdacos\\.detcetorp//:eopp)").getColumn(0);
        if ((cpPage == null) || (linksCount == null) || (linksCount.length == 0)) { return null; }
        final int linkCounter = linksCount.length;
        for (int i = 0; i <= 3; i++) {
            final String equals = this.getCaptchaCode(PrtctdScdvntCm.MAINPAGE + cpPage, param);
            this.br.postPage(PrtctdScdvntCm.MAINPAGE + sendCaptcha, "res_code=" + equals);
            if (!this.br.toString().trim().equals("1") && !this.br.toString().trim().equals("0")) {
                this.logger.warning("Error in doing the maths for link: " + parameter);
                return null;
            }
            if (!this.br.toString().trim().equals("1")) {
                continue;
            }
            break;
        }
        if (!this.br.toString().trim().equals("1")) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        this.logger.info("Found " + linkCounter + " links, decrypting now...");
        progress.setRange(linkCounter);
        for (int i = 0; i <= linkCounter - 1; i++) {
            this.br.getHeaders().put("Referer", parameter);
            final String actualPage = PrtctdScdvntCm.MAINPAGE + "allinks.php?out_name=" + postvar + "&&link_id=" + i;
            this.br.getPage(actualPage);
            if (this.br.containsHTML("No htmlCode read")) {
                this.logger.info("Found one offline link for link " + parameter + " linkid:" + i);
                continue;
            }
            String finallink = this.br.getRegex("http-equiv=\"refresh\" content=\"0;url=(http.*?)\"").getMatch(0);
            if (finallink == null) {
                // Handlings for more hosters will come soon i think
                if (this.br.containsHTML("turbobit\\.net")) {
                    final String singleProtectedLink = PrtctdScdvntCm.MAINPAGE + "plugin/turbobit.net.free.php?out_name=" + postvar + "&link_id=" + i;
                    this.br.getPage(singleProtectedLink);
                    if (this.br.getRedirectLocation() == null) {
                        this.logger.warning("Redirect location for this link is null: " + parameter);
                        return null;
                    }
                    final String turboId = new Regex(this.br.getRedirectLocation(), "http://turbobit\\.net/download/free/(.+)").getMatch(0);
                    if (turboId == null) {
                        this.logger.warning("There is a problem with the link: " + actualPage);
                        return null;
                    }
                    finallink = "http://turbobit.net/" + turboId + ".html";
                } else if (this.br.containsHTML("hotfile\\.com")) {
                    finallink = this.br.getRegex("style=\"margin:0;padding:0;\" action=\"(.*?)\"").getMatch(0);
                    if (finallink == null) {
                        finallink = this.br.getRegex("onmouseout=\"hoverFuncRemove\\(this\\)\" ><a href=\"(/dl.*?\\.html)\\?uploadid=").getMatch(0);
                        if (finallink != null) {
                            finallink = "http://hotfile.com" + finallink;
                        }
                    }
                }
            }
            if (finallink == null) {
                this.logger.warning("Finallink for the following link is null: " + parameter);
            }
            decryptedLinks.add(this.createDownloadlink(finallink));
            progress.increase(1);
        }
        return decryptedLinks;
    }
}
