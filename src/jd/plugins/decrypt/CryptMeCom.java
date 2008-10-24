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
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class CryptMeCom extends PluginForDecrypt {

    public CryptMeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);

        if (br.containsHTML("<img src=\"http://crypt-me.com/rechen-captcha.php\">")) {
            Form form = br.getForm(0);
            String captchaAddress = "http://crypt-me.com/rechen-captcha.php";
            File captchaFile = this.getLocalCaptchaFile(this);
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaAddress));
            String captchaCode = Plugin.getCaptchaCode(captchaFile, this, param);
            form.put("sicherheitscode", captchaCode);
            br.submitForm(form);
        }

        String containerId = br.getRegex("<a href='http://crypt-me.com/dl\\.php\\?file=(.*?)\\.(dlc|ccf|rsdf)' target='_blank'>").getMatch(0);

        if (br.containsHTML("<a href='.*?' target='_blank'>\\.dlc Download</a>")) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
            Browser.download(container, "http://crypt-me.com/dl.php?file=" + containerId + ".dlc");
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
        }

        if (decryptedLinks.size() == 0 && br.containsHTML("<a href='.*?' target='_blank'>\\.ccf Download</a>")) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".ccf");
            Browser.download(container, "http://crypt-me.com/dl.php?file=" + containerId + ".ccf");
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
        }

        if (decryptedLinks.size() == 0 && br.containsHTML("<a href='.*?' target='_blank'>\\.rsdf Download</a>")) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf");
            Browser.download(container, "http://crypt-me.com/dl.php?file=" + containerId + ".rsdf");
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
        }

        if (decryptedLinks.size() == 0) {
            String folderId = new Regex(parameter, "folder/([a-zA-Z0-9]+)\\.html").getMatch(0);
            String[] folderSize = br.getRegex("<a onclick=\"return newpopup\\(('.*?', '.*?')\\);\" ").getColumn(0);

            for (int i = 1; i <= folderSize.length; i++) {
                String encodedLink = new Regex(br.getPage("http://crypt-me.com/go.php?id=" + folderId + "&lk=" + i), "<iframe src=\"http://anonym.to/\\?(.*?)\"").getMatch(0);

                decryptedLinks.add(createDownloadlink(encodedLink));

            }
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
