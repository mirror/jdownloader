//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "drei.to" }, urls = { "http://[\\w\\.]*?drei\\.to/link/[a-zA-Z0-9]+|http://[\\w\\.]*?drei\\.to/download/start/[0-9]+/|http://[\\w\\.]*?drei\\.to/download/[0-9]+/.+\\.html|http://[\\w\\.]*?drei\\.to/\\?action=entrydetail&entry_id=[0-9]+" }, flags = { 0 })
public class DrDlm extends PluginForDecrypt {
    private String password;
    private CryptedLink link;
    static public final String DECRYPTER_3DLAM_1 = "http://[\\w\\.]*?drei\\.to/link/[a-zA-Z0-9]+";
    static public final String DECRYPTER_3DLAM_2 = "http://[\\w\\.]*?drei\\.to/download/start/[0-9]+/";
    static public final String DECRYPTER_3DLAM_3 = "http://[\\w\\.]*?drei\\.to/download/[0-9]+/.+\\.html";

    // static public final String DECRYPTER_3DLAM_4 =
    // "http://[\\w\\.]*?drei\\.to/\\?action=entrydetail&entry_id=[0-9]+";

    public DrDlm(PluginWrapper wrapper) {
        super(wrapper);
    }

    private void decryptFromDownload(String parameter) throws Exception {
        parameter = parameter.replace("&quot;", "\"");
        br.getPage(parameter);
        Thread.sleep(500);
        // passwort auslesen
        password = br.getRegex(Pattern.compile("Passwort:</th><td colspan=.*?<input type=\"text\" value=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (password == null || (password != null && (password.contains("kein") || password.contains("kein P")) || password.contains("keines"))) {
            password = null;
        }
        if (br.containsHTML("Versuche es in ein paar Minuten wieder.")) { throw new DecrypterException("Too many wrong captcha codes. Try it again in few minutes, please."); }
        for (int retry = 1; retry < 5; retry++) {
            br.getPage(parameter);
            Thread.sleep(500);
            String captcha = br.getRegex(Pattern.compile("<img src=\"(/index\\.php\\?action=captcha.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (captcha != null) {
                String capTxt = getCaptchaCode("drei.to", "http://drei.to" + captcha, link);
                Form form = new Form();
                form.setAction(br.getRegex("<form action=\"(.*?)\" method=\"post\"").getMatch(0));
                form.setMethod(MethodType.POST);
                InputField nv = new InputField("answer", "1");
                InputField nv2 = new InputField("submit", "Datei herunterladen");
                form.addInputField(nv);
                form.addInputField(nv2);
                form.put("answer", capTxt);
                br.submitForm(form);
                if (!br.containsHTML("/failed.html")) break;
            }
        }
        if (br.containsHTML("/failed.html';")) throw new DecrypterException("Wrong Captcha Code");
    }

    private String decryptFromLink(String parameter) throws IOException {
        br.getPage(parameter);
        String link = br.getRegex(Pattern.compile("<frame src=\"(http.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        return link;
    }

    private ArrayList<String> decryptFromStart(String parameter) throws Exception {
        ArrayList<String> linksReturn = new ArrayList<String>();
        if (parameter != null) br.getPage(parameter);
        if (br.containsHTML("/failed.html")) {
            String url = br.getRegex(DECRYPTER_3DLAM_3).getMatch(-1);
            decryptFromDownload(url);
        }
        String[] links = br.getRegex(Pattern.compile("value=\"(http://.*?drei\\.to/link/.*?/)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
        for (String link2 : links) {
            linksReturn.add(link2);
        }
        return linksReturn;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://drei.to");
        Thread.sleep(500);
        br.postPage("http://drei.to/?", "set_enter_ts=true");
        Thread.sleep(500);
        link = param;
        if (new Regex(parameter, DECRYPTER_3DLAM_2).matches()) {
            ArrayList<String> links = decryptFromStart(parameter);
            progress.setRange(links.size());
            for (int i = 0; i < links.size(); i++) {
                progress.increase(1);
                String link = decryptFromLink(links.get(i));
                DownloadLink dl_link = createDownloadlink(link);
                dl_link.addSourcePluginPassword(password);
                decryptedLinks.add(dl_link);
            }
        } else if (new Regex(parameter, DECRYPTER_3DLAM_1).matches()) {
            progress.setRange(1);
            String link = decryptFromLink(parameter);
            decryptedLinks.add(createDownloadlink(link));
            progress.increase(1);
        } else {
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

}
