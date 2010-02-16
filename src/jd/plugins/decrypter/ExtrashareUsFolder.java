//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "extrashare.us folder" }, urls = { "http://[\\w\\.]*?extrashare\\.us/(\\w\\w/)?folder/[0-9]+/" }, flags = { 0 })
public class ExtrashareUsFolder extends PluginForDecrypt {

    public ExtrashareUsFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.containsHTML("Ez a k&ouml;nyvt&aacute;r jelsz&oacute;val v&eacute;dett")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("class=\"konyvtarnev\">(.*?)</span>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
        boolean fail = false;
        String[] files = br.getRegex("<tr>(.*?<td class=tdrow1.*?)</tr>").getColumn(0);
        if (files == null || files.length == 0) {
            fail = true;
            files = br.getRegex("<td class=tdrow2><a href='(.*?)'>").getColumn(0);
        }
        if (files == null || files.length == 0) return null;
        for (String fileinfo : files) {
            String filename = new Regex(fileinfo, "<td class=tdrow1><div align=\"left\">(.*?)</div>").getMatch(0);
            String filesize = new Regex(fileinfo, "<td class=tdrow2></td>.*?<td class=tdrow1>(.*?)</td>").getMatch(0);
            String filelink = new Regex(fileinfo, "href='(.*?)'>Letöltés</a>").getMatch(0);
            if (fail) filelink = fileinfo;
            if (filelink != null) {
                DownloadLink dl = createDownloadlink(filelink);
                if (filename != null) dl.setName(filename);
                if (filesize != null) dl.setDownloadSize(Regex.getSize(filesize));
                if (filename != null && filesize != null) dl.setAvailable(true);
                decryptedLinks.add(dl);
            } else
                logger.warning("Failed to get filelink for " + fileinfo + " from link: " + param.getCryptedUrl());
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
