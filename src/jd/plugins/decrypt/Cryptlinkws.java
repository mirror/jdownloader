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
import java.util.regex.Pattern;

import jd.controlling.DistributeData;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class Cryptlinkws extends PluginForDecrypt {

    static private String host = "cryptlink.ws";
    final static private Pattern patternSupported_File = Pattern.compile("http://[\\w\\.]*?cryptlink\\.ws/crypt\\.php\\?file=[0-9]+", Pattern.CASE_INSENSITIVE);
    final static private Pattern patternSupported_Folder = Pattern.compile("http://[\\w\\.]*?cryptlink\\.ws/\\?file=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    final static private Pattern patternSupported = Pattern.compile(patternSupported_Folder.pattern() + "|" + patternSupported_File.pattern(), Pattern.CASE_INSENSITIVE);

    public Cryptlinkws(String cfgName){
        super(cfgName);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        if (parameter.matches(patternSupported_File.pattern())) {
            /* Einzelne Datei */
            br.getPage(parameter);
            String link = br.getRegex("unescape\\(('|\")(.*?)('|\")\\)").getMatch(1);
            link = Encoding.htmlDecode(Encoding.htmlDecode(link));
            br.getPage("http://www.cryptlink.ws/" + link);
            link = br.getRegex("unescape\\(('|\")(.*?)('|\")\\)").getMatch(1);
            link = Encoding.htmlDecode(Encoding.htmlDecode(link));
            if (link.startsWith("cryptfiles/")) {
                /* Weiterleitung durch Server */
                br.getPage("http://www.cryptlink.ws/" + link);

                /* Das hier ist eher weniger gut gelöst */
                decryptedLinks.addAll(new DistributeData(br.toString()).findLinks(false));
            } else {
                /* Direkte Weiterleitung */
                decryptedLinks.add(createDownloadlink(link));
            }
        } else if (parameter.matches(patternSupported_Folder.pattern())) {
            /* ganzer Ordner */
            boolean do_continue = false;
            for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {

                br.getPage(parameter);

                Form[] forms = br.getForms();

                if (forms.length == 1) {
                    /* Weder Captcha noch Passwort vorhanden */
                } else {
                    /* Captcha vorhanden, Passwort vorhanden oder beides */

                    if (forms[0].getVars().containsKey("folderpass")) {
                        /* Eingabefeld für Passwort vorhanden */
                        String password = JDUtilities.getGUI().showUserInputDialog("Ordnerpasswort?");
                        if (password == null) {
                            /* Auf "Abbruch" geklickt */
                            return decryptedLinks;
                        }
                        forms[0].put("folderpass", password);

                    }

                    if (forms[0].getVars().containsKey("captchainput")) {
                        /* Eingabefeld für Captcha vorhanden */

                        File captchaFile = getLocalCaptchaFile(this);
                        String captchaCode;
                        if (!br.cloneBrowser().downloadFile(captchaFile, "http://www.cryptlink.ws/captcha.php")) {

                            /* Fehler beim Captcha */
                            logger.severe("Captcha Download fehlgeschlagen!");
                            return decryptedLinks;
                        }
                        /* CaptchaCode holen */
                        if ((captchaCode = Plugin.getCaptchaCode(captchaFile, this)) == null) { return decryptedLinks; }
                        forms[0].put("captchainput", captchaCode);

                    }

                    br.submitForm(forms[0]);
                }

                if (!br.containsHTML("Wrong Password! Klicken Sie") && !br.containsHTML("Wrong Captchacode! Klicken Sie")) {
                    do_continue = true;
                    break;
                }

            }

            if (do_continue == true) {
                String[] links = br.getRegex("href=\"crypt\\.php\\?file=(.*?)\"").getColumn(0);
                progress.setRange(links.length);
                for (String element : links) {
                    decryptedLinks.add(createDownloadlink("http://www.cryptlink.ws/crypt.php?file=" + element));
                    progress.increase(1);
                }
            }
        }

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}