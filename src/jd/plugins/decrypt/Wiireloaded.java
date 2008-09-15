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
        int submitvalue = getPluginConfig().getIntegerProperty("WIIReloaded_SubmitValue", 5);
        if (submitvalue == -1) {
            logger.info("SubmitValue unknown, pls inform Support");
            return null;
        }
        String parameter = param.toString();
        Vector<String> link_passwds = new Vector<String>();
        link_passwds.add("wii-reloaded.info");
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        br.setDebug(true);
        progress.setRange(3);
        br.getPage(parameter);
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
            Browser.download(captchaFile, br.openGetConnection(adr));
            progress.addToMax(1);
            if (!captchaFile.exists() || captchaFile.length() == 0) {
                return null;
            } else {
                String capTxt = Plugin.getCaptchaCode(captchaFile, this);
                br.getPage(parameter);
                Form[] forms = br.getForms();
                Form post = forms[0];
                post.setVariable(0, capTxt);
                page = br.submitForm(post);
            }
        }
        String[][] ids = new Regex(page, "onClick=\"popup_dl\\((.*?)\\)\"").getMatches();
        logger.finer("ids found"+ids.length);
        progress.addToMax(ids.length);
        for (String[] element : ids) {
            String u = "http://wii-reloaded.ath.cx/protect/hastesosiehtsaus.php?i=" + element[0];
            br.getPage(u);
            Form form = br.getForms()[0];
            form.setVariable(0, submitvalue + "");
            br.submitForm(form);
            if (br.getRedirectLocation() == null) {
                /* neuer submit value suchen */
                logger.info("Searching new SubmitValue");
                boolean found = false;
                for (int i = 0; i <= 100; i++) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                    form.setVariable(0, i + "");
                    br.submitForm(form);
                    if (br.getRedirectLocation() != null) {
                        found = true;
                        getPluginConfig().setProperty("WIIReloaded_SubmitValue", i);
                        submitvalue = i;
                        logger.info("SubmitValue found!");
                        break;
                    }
                }
                if (found == false) {
                    logger.info("SubmitValue NOT found!");
                    getPluginConfig().setProperty("WIIReloaded_SubmitValue", -1);
                    return null;
                }
            }
            if (br.getRedirectLocation() != null) {
                DownloadLink link = createDownloadlink(br.getRedirectLocation());
                link.setSourcePluginPasswords(link_passwds);
                decryptedLinks.add(link);
            }
            progress.increase(1);
        }
        progress.increase(1);
        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
