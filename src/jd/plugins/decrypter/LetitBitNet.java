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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "letitbit.net" }, urls = { "http://(www\\.)?letitbit\\.net//page/folder/\\d+\\|.+" }, flags = { 0 })
public class LetitBitNet extends PluginForDecrypt {

    public LetitBitNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage("http://letitbit.net/");
        br.postPage("http://letitbit.net/", "England.x=10&England.y=9&vote_cr=en");
        br.getPage(parameter);
        if (!br.containsHTML("<td width=\"700\" colspan=\"2\">") || br.containsHTML("<h2>Owner: </h2>")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String[] linkInfo = br.getRegex("<td width=\"700\" colspan=\"2\">(.*?)<hr />").getColumn(0);
        if (linkInfo == null || linkInfo.length == 0) return null;
        for (String singleInfo : linkInfo) {
            String downloadUrl = new Regex(singleInfo, "\"(http://letitbit\\.net/download/.*?)\"").getMatch(0);
            if (downloadUrl == null) new Regex(singleInfo, "(http://letitbit\\.net/download/[a-z0-9\\.]+/.*?\\.html)<br").getMatch(0);
            if (downloadUrl == null) return null;
            DownloadLink dl = createDownloadlink(downloadUrl);
            String filename = new Regex(singleInfo, "target=\"_blank\"><font size=\"4\">(.*?)</font>").getMatch(0);
            if (filename != null) dl.setFinalFileName(filename.trim());
            String filesize = new Regex(singleInfo, "<b></b>(.*?)<br />").getMatch(0);
            if (filesize != null && !filesize.equals(" ")) dl.setDownloadSize(Regex.getSize(filesize));
            if (singleInfo.contains(">OK</font>"))
                dl.setAvailable(true);
            else if (singleInfo.contains("<b>BAD</b>")) dl.setAvailable(false);
            decryptedLinks.add(createDownloadlink(singleInfo));
        }

        return decryptedLinks;
    }

}
