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

package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

public class UploadrEu extends PluginForDecrypt {

    public UploadrEu(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        if (parameter.contains("/link/")) {
            String link = br.getRedirectLocation();
            if (link == null) link = br.getRegex("main_frame' src='(.*?)'").getMatch(0);
            if (link == null) link = br.getRegex("<title>Uploadr.eu - Weiterleitung zu (.*?)</title>").getMatch(0);
            if (link == null) link = br.getRegex("innerHTML = '<a href=\"(.*?)\"").getMatch(0);
            if (link == null) link = br.getRegex("self\\.window\\.location = \\('(.*?)'\\)").getMatch(0);
            if (link == null) link = br.getRegex("<title>Uploadr.eu - Redirect to (.*?)</title>").getMatch(0);
            if (link == null) return null;
            decryptedLinks.add(createDownloadlink(link));
            return decryptedLinks;
        } else if (parameter.contains("/folder/")) {
            boolean captcha = false;
            boolean password = false;
            String captchastring = null;
            String passwordstring = null;
            String captchaurl = null;
            if (!br.containsHTML("<strong>Status:</strong>")) {
                for (int i = 0; i < 5; i++) {
                    Form passcap = br.getFormbyProperty("name", "captcha");
                    if (passcap == null) br.getForm(0);
                    if (br.containsHTML("Ordnerpasswort:")) {
                        passwordstring = getUserInput(JDL.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"), param.getDecrypterPassword(), param);
                        password = true;
                    }

                    if (br.containsHTML("Nicht lesbar?")) {
                        captchaurl = br.getRegex("img id='captcha' src='(.*?)'").getMatch(0);
                        if (captchaurl == null) captchaurl = br.getRegex("captchaimage'><img src='(.*?)'").getMatch(0);
                        if (captchaurl == null) captchaurl = br.getRegex("captcha.src=\"(.*?)\"").getMatch(0);
                        if (captchaurl != null) captchaurl = captchaurl.replaceFirst("\\.\\./", "http://uploadr.eu/");
                        captchastring = getCaptchaCode(captchaurl, param);
                        captcha = true;
                    }
                    if (captcha) passcap.put("captchastring", captchastring);
                    if (password && !captcha) passcap.put("passwordvalue", passwordstring);
                    if (password && captcha) passcap.put("password", passwordstring);

                    br.submitForm(passcap);
                    if (!br.containsHTML("Eingabe falsch!")) break;
                    if (i == 4) { throw new DecrypterException(JDL.L("plugins.decrypter.uploadreu.badpassorcaptcha", "You entered bad password or captcha code 5 times. Please review your data.")); }
                    br.getPage(parameter);

                }
            }

            String[] links = br.getRegex("onclick='window\\.open\\(\"(.*?)\"").getColumn(0);
            br.setFollowRedirects(false);
            for (int j = 0; j < links.length; j++) {
                br.getPage(links[j]);
                decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
            }
            return decryptedLinks;
        } else
            return null;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}