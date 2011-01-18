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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "asapload.com" }, urls = { "http://[\\w\\.]*?asapload\\.com/info\\?id=\\d+" }, flags = { 0 })
public class AsapLoadCom extends PluginForDecrypt {

    public AsapLoadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PASSWORDPROTECTED = "<b>File is protected with password</b>";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("<h1>File not found</h1>")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (br.containsHTML(PASSWORDPROTECTED)) {
            String fileID = new Regex(parameter, "asapload\\.com/info\\?id=(\\d+)").getMatch(0);
            if (fileID == null) return null;
            for (int i = 0; i <= 3; i++) {
                String passCode = getUserInput(null, param);
                br.postPage(parameter, "id=" + fileID + "&upload_password=" + passCode);
                if (br.containsHTML(PASSWORDPROTECTED)) continue;
                break;
            }
            if (br.containsHTML(PASSWORDPROTECTED)) throw new DecrypterException(DecrypterException.PASSWORD);
        }
        String[] allLinks = br.getRegex("\"(http://asapload\\.com/download\\?id=\\d+)\"").getColumn(0);
        if (allLinks == null || allLinks.length == 0) return null;
        progress.setRange(allLinks.length);
        for (String singleLink : allLinks) {
            br.getPage(singleLink);
            String finallink = br.getRedirectLocation();
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }
        return decryptedLinks;
    }

}
