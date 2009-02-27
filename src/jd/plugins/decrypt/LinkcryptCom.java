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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

public class LinkcryptCom extends PluginForDecrypt {

    public LinkcryptCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);

        if (br.containsHTML("Sicherheitscode")) {
            Form f = br.getForm(0);
            String url = br.getRegex("(captcha\\-.*?\\.gif)").getMatch(0);
            File captchaFile = this.getLocalCaptchaFile(this);
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(url));

            String captchaCode = Plugin.getCaptchaCode(captchaFile, this, param);
            if (captchaCode == null) return null;
            f.put("captcha", captchaCode);
            br.submitForm(f);

        }
        String[] ids = br.getRegex("startDownload\\(\\'(.*?)\\'\\)").getColumn(0);
        if (ids.length > 0) {
            Browser tab;
            progress.setRange(ids.length);
            for (String id : ids) {
                tab = br.cloneBrowser();
                tab.getPage("pl-" + id);
                String link = tab.getRegex("<iframe .*? src=\"(.*?)\">").getMatch(0);
                decryptedLinks.add(createDownloadlink(link));
                progress.increase(1);

            }
            progress.finalize();
        }

        String url = br.getRegex("<meta http-equiv=\"refresh\" .*? URL=(.*?)\" />").getMatch(0);
        decryptedLinks.add(createDownloadlink(url));
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 4227 $");
    }
}