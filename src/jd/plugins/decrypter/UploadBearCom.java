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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadbear.com" }, urls = { "http://[\\w\\.]*?uploadbear\\.com/mirrors\\.php\\?id=.*?\\&key=[0-9a-z]+" }, flags = { 0 })
public class UploadBearCom extends PluginForDecrypt {

    public UploadBearCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("This file does not exist")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String[] redirectLinks = br.getRegex("\"(/link\\.php\\?id=.*?\\&filename=.*?)\"").getColumn(0);
        if ((redirectLinks == null || redirectLinks.length == 0) && !br.containsHTML("download.php")) return null;
        if (br.containsHTML("download.php")) {
            String filename = br.getRegex("filename=(.*?)\"").getMatch(0);
            String filesize = br.getRegex("Filesize:</b></td><td>(.*?)</td>").getMatch(0);
            DownloadLink upbear = createDownloadlink(parameter.replace("mirrors.php?id=", "download.php?id="));
            if (filename != null) upbear.setName(filename.trim());
            if (filesize != null) upbear.setDownloadSize(Regex.getSize(filesize));
            decryptedLinks.add(upbear);
        }
        progress.setRange(redirectLinks.length);
        for (String link : redirectLinks) {
            link = "http://www.uploadbear.com" + link;
            br.getPage(link);
            String finallink = br.getRedirectLocation();
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }
        return decryptedLinks;
    }

}