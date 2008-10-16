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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

public class ScumIn extends PluginForDecrypt {

    public ScumIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);

        File captchaFile = this.getLocalCaptchaFile(this);
        Browser.download(captchaFile, br.cloneBrowser().openGetConnection("http://scum.in/share/includes/captcha.php?t="));
        String captchaCode = Plugin.getCaptchaCode(captchaFile, this, param);

        br.postPage("http://scum.in/plugins/home/links.callback.php", "id=" + parameter.substring(parameter.lastIndexOf("=") + 1) + "&captcha=" + captchaCode);

        String links[] = br.getRegex("href=\"(.*?)\"").getColumn(0);
        progress.setRange(links.length);
        for (String element : links) {
            DownloadLink dLink = createDownloadlink(element);
            dLink.addSourcePluginPassword("scum.in");
            decryptedLinks.add(dLink);
            progress.increase(1);
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}