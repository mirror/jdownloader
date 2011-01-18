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

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spreadlink.us" }, urls = { "http://[\\w\\.]*?spreadlink\\.us/[a-z0-9]+" }, flags = { 0 })
public class SprdLnkUs extends PluginForDecrypt {

    public SprdLnkUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(parameter);
        if (br.containsHTML("loading\\.gif\\' alt=\\'Lade\\'")) {
            logger.info("Sleeping...");
            sleep(3 * 1000l, param);
            br.getPage(parameter);
        }
        if (br.containsHTML("Dir fehlt die Berechtigung für diesen Link")) {
            logger.warning("The link " + parameter + " is blocked through a referer protection and can't be accessed!");
            throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        }
        if (br.containsHTML(">Dieser Link exestiert nicht")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String finallink = null;
        if (br.containsHTML("/> Du wirst weitergeleitet in:")) {
            logger.info("Found Weiterleitungslink");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            String theID = new Regex(parameter, "spreadlink\\.us/([a-z0-9]+)").getMatch(0);
            if (theID == null) return null;
            br.getPage("http://spreadlink.us/index.php?request=datei&datei=weiterleitung&link=" + theID + "&action=1");
        } else {
            logger.info("Found captcha- or passwordlink");
            System.out.print(br.toString());
            Regex theData = br.getRegex("secToW\\(\\'(\\d+)\\', \\'([a-z0-9]+)\\', \\'(.*?)\\', \\'(\\d+)\\'\\);");
            String waittime = theData.getMatch(0);
            String linkid = theData.getMatch(1);
            String passwordOrNot = theData.getMatch(2);
            String captchaOrNot = theData.getMatch(3);
            if (waittime == null || linkid == null || passwordOrNot == null || captchaOrNot == null) return null;
            handleCaptchaAndPassword(linkid, passwordOrNot, captchaOrNot, param);
        }
        finallink = br.getRegex("location\\.href='(.*?)';").getMatch(0);
        if (finallink == null) return null;
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

    public void handleCaptchaAndPassword(String linkid, String passwordOrNot, String captchaOrNot, CryptedLink param) throws Exception {
        String captchaWrong = "Das Captcha ist falsch";
        String passwordWrong = "Das passwort ist falsch";
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        for (int i = 0; i < 5; i++) {
            String sendData = "http://spreadlink.us/index.php?request=datei&datei=weiterleitung&link=" + linkid + "&action=1";
            if (!passwordOrNot.equals("1")) {
                String thePassword = getUserInput(null, param);
                sendData = sendData + "&passwort=" + thePassword;
            }
            if (captchaOrNot.equals("1")) {
                File file = this.getLocalCaptchaFile();
                String url = "http://spreadlink.us/data/module/captcha.php";
                Browser.download(file, br.cloneBrowser().openGetConnection(url));
                Point p = UserIO.getInstance().requestClickPositionDialog(file, "relink.us", "Click on open Circle");
                /* anticaptcha does not work good enough */
                // int[] p = new jd.captcha.specials.GmdMscCm(file).getResult();
                if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
                sendData = sendData + "&button.x=" + p.x;
                sendData = sendData + "&button.y=" + p.y;
            }
            br.getPage(sendData);
            if (br.containsHTML(captchaWrong) || br.containsHTML(passwordWrong)) continue;
            break;
        }
        if (br.containsHTML(captchaWrong)) throw new DecrypterException(DecrypterException.CAPTCHA);
        if (br.containsHTML(passwordWrong)) throw new DecrypterException(DecrypterException.PASSWORD);
    }
}