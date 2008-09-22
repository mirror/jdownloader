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
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginPattern;
import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

public class DreiDlAm extends PluginForDecrypt {
    private String password;
    private CryptedLink link;

    public DreiDlAm(PluginWrapper wrapper) {
        super(wrapper);
    }

    private void decryptFromDownload(String parameter) throws IOException, DecrypterException {
        parameter.replace("&quot;", "\"");
        br.getPage(parameter);
        // passwort auslesen
        password = br.getRegex(Pattern.compile("<b>Passwort:</b></td><td><input type='text' value='(.*?)'", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (password != null && (password.contains("kein") || password.contains("kein P"))) {
            password = null;
        }
        if (br.containsHTML("Versuche es in ein paar Minuten wieder.")) { throw new DecrypterException("Too many wrong captcha codes. Try it again in few minutes, please."); }
        for (int retry = 1; retry < 5; retry++) {
            br.getPage(parameter);
            String captcha = br.getRegex(Pattern.compile("><img src=\"/images/captcha5\\.php(.*?)\" /></td>", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (captcha != null) {
                File file = this.getLocalCaptchaFile(this);
                Form form = br.getForm(3);
                Browser.download(file, br.cloneBrowser().openGetConnection("http://3dl.am/images/captcha5.php" + captcha));
                String capTxt = Plugin.getCaptchaCode(file, this, link);
                form.put("antwort", capTxt);
                br.submitForm(form);
                if (!br.containsHTML("/failed.html';")) break;
            }
        }
        if (br.containsHTML("/failed.html';")) throw new DecrypterException("Wrong Captcha Code");
    }

    private String decryptFromLink(String parameter) throws IOException {
        br.getPage(parameter);
        String link = br.getRegex(Pattern.compile("<frame src=\"(.*?)\" width=\"100%\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        return link;
    }

    private ArrayList<String> decryptFromStart(String parameter) throws IOException, DecrypterException {
        ArrayList<String> linksReturn = new ArrayList<String>();
        if (parameter != null) br.getPage(parameter);
        if (br.containsHTML("/failed.html")) {
            String url = br.getRegex(PluginPattern.decrypterPattern_DreiDlAm_3).getMatch(-1);
            decryptFromDownload(url);
        }
        String[] links = br.getRegex(Pattern.compile("value='http://3dl\\.am/link/(.*?)/'", Pattern.CASE_INSENSITIVE)).getColumn(0);
        for (int i = 0; i < links.length; i++) {
            linksReturn.add("http://3dl.am/link/" + links[i] + "/");
        }
        return linksReturn;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws IOException, DecrypterException {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.clearCookies("3dl.am");
        link = param;
        if (new Regex(parameter, PluginPattern.decrypterPattern_DreiDlAm_2).matches()) {
            ArrayList<String> links = decryptFromStart(parameter);
            progress.setRange(links.size());
            for (int i = 0; i < links.size(); i++) {
                progress.increase(1);
                String link = decryptFromLink(links.get(i));
                DownloadLink dl_link = createDownloadlink(link);
                dl_link.addSourcePluginPassword(password);
                decryptedLinks.add(dl_link);
            }
        } else if (new Regex(parameter, PluginPattern.decrypterPattern_DreiDlAm_1).matches()) {
            progress.setRange(1);
            String link = decryptFromLink(parameter);
            decryptedLinks.add(createDownloadlink(link));
            progress.increase(1);
        } else if (new Regex(parameter, PluginPattern.decrypterPattern_DreiDlAm_3).matches()) {
            decryptFromDownload(parameter);
            ArrayList<String> links = decryptFromStart(null);
            progress.setRange(links.size());
            for (int i = 0; i < links.size(); i++) {
                progress.increase(1);
                String link2 = decryptFromLink(links.get(i));
                DownloadLink dl_link = createDownloadlink(link2);
                dl_link.addSourcePluginPassword(password);
                decryptedLinks.add(dl_link);
            }
        }
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