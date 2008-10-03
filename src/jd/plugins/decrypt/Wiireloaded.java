//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.decrypt;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

public class Wiireloaded extends PluginForDecrypt {

    public Wiireloaded(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        String parameter = param.toString();
        Vector<String> link_passwds = new Vector<String>();
        link_passwds.add("wii-reloaded.info");
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        br.setCookiesExclusive(true);
        br.clearCookies("wii-reloaded.ath.cx");
        progress.setRange(3);
        br.getPage(parameter);
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        String page = br.getPage(parameter);
        progress.increase(1);
        int max = 10;
        while (page.contains("captcha/captcha.php") || page.contains("Sicherheitscode war falsch")) {
            if (max-- <= 0) {
                logger.severe("Captcha Code has been wrong many times. abort.");
                return null;
            }
            String adr = "http://wii-reloaded.ath.cx/protect/captcha/captcha.php";
            File captchaFile = Plugin.getLocalCaptchaFile(this, ".jpg");
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(adr));
            progress.addToMax(1);
            if (!captchaFile.exists() || captchaFile.length() == 0) {
                return null;
            } else {
                String capTxt = Plugin.getCaptchaCode(captchaFile, this, param);
                Form post = br.getForm(0);
                post.setVariable(1, capTxt);
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                }
                page = br.submitForm(post);
            }
        }
        String[][] ids = new Regex(page, "onClick=\"popup_dl\\((.*?)\\)\"").getMatches();
        progress.addToMax(ids.length);
        Browser brc = br.cloneBrowser();
        for (String[] element : ids) {
            for (int i = 1; i < 3; i++) {
                String u = "http://wii-reloaded.ath.cx/protect/hastesosiehtsaus.php?i=" + element[0];
                brc.getPage(u);

                String adr = "http://wii-reloaded.ath.cx/protect/captcha/numeric.php";
                File captchaFile = Plugin.getLocalCaptchaFile(this, ".jpg");
                Browser.download(captchaFile, brc.cloneBrowser().openGetConnection(adr));
                if (!captchaFile.exists() || captchaFile.length() == 0) {
                    return null;
                } else {
                    String capTxt = Plugin.getCaptchaCode(this, "wii-numeric", captchaFile, false, param);
                    Form post = brc.getForm(0);
                    post.put("insertvalue", capTxt);
                    page = brc.submitForm(post);
                }

                if (brc.getRedirectLocation() != null) {
                    DownloadLink link = createDownloadlink(brc.getRedirectLocation());
                    link.setSourcePluginPasswords(link_passwds);
                    decryptedLinks.add(link);
                    break;
                }
            }
            progress.increase(1);
        }
        progress.increase(1);
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
