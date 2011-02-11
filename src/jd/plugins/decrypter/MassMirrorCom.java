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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "massmirror.com" }, urls = { "http://[\\w\\.]*?massmirror\\.com/.*?\\.html" }, flags = { 0 })
public class MassMirrorCom extends PluginForDecrypt {

    public MassMirrorCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("(File Not Found|The file you requested was not found|This file never existed on|Removed due to copyright violations)") || !br.containsHTML("download.php")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String[] redirectLinks = br.getRegex("\"(download\\.php\\?id=.*?fileid=[a-z0-9A-Z]+)\"").getColumn(0);
        String directLink = br.getRegex("(/get\\.php\\?dl=.*?)\"").getMatch(0);
        if (redirectLinks == null || redirectLinks.length == 0) return null;
        progress.setRange(redirectLinks.length);
        for (String link : redirectLinks) {
            link = link.replace("amp;", "");
            Browser cl = br.cloneBrowser();
            cl.getPage("http://massmirror.com/" + link);
            String dllink = cl.getRedirectLocation();
            if (dllink == null) return null;
            decryptedLinks.add(createDownloadlink(dllink));
            progress.increase(1);
        }
        if (directLink != null) {
            String link = "directhttp://http://massmirror.com" + directLink;
            DownloadLink direct = new DownloadLink(null, null, "DirectHTTP", link, true);
            direct.setProperty("cookies", br.getCookies("http://massmirror.com").get("PHPSESSID"));
            direct.setProperty("refURL", br.getURL());
            decryptedLinks.add(direct);
        }

        return decryptedLinks;
    }

}