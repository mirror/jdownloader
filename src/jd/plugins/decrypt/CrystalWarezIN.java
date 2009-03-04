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

package jd.plugins.decrypt;

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

public class CrystalWarezIN extends PluginForDecrypt {

    private static final String patternLink_Protected = "(http://[\\w\\.]*?crystal-warez\\.in/protect/[^\"']*)";
    static private Integer lock = 0;

    public CrystalWarezIN(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String parameter = param.toString();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        synchronized (lock) {
            br.getPage(parameter);
            if (parameter.matches(patternLink_Protected)) {
                String code = null;
                while (code == null) {
                    File file = this.getLocalCaptchaFile(this);
                    URLConnectionAdapter con = br.cloneBrowser().openGetConnection("http://crystal-warez.in/securimage_show.php");
                    Browser.download(file, con);
                    code = Plugin.getCaptchaCode(file, this, param);
                    Form form = br.getForm(1);
                    form.put("captcha", code);
                    br.submitForm(form);
                    if (br.containsHTML("Code ist falsch")) code = null;
                }
                String[] links = br.getRegex("window\\.open\\('(.*?)'").getColumn(0);
                progress.setRange(links.length);
                for (String element : links) {
                    DownloadLink link = createDownloadlink(element);
                    decryptedLinks.add(link);
                    progress.increase(1);
                }
            } else {
                String[] mirrors = br.getRegex(patternLink_Protected).getColumn(0);
                String pw = br.getRegex("<b>Passwort:</b></td>.*?<td.*?>(<i>)?(.*?)(</i>)?</td>").getMatch(1);
                for (String element : mirrors) {

                    DownloadLink dl = createDownloadlink(element);
                    if (!pw.toLowerCase().contains("kein")) dl.addSourcePluginPassword(pw);
                    decryptedLinks.add(dl);
                }
            }
            return decryptedLinks;
        }
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}