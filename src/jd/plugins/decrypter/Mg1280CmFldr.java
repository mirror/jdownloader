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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mega.1280.com" }, urls = { "http://[\\w\\.]*?mega\\.1280\\.com/folder/[A-Z|0-9]+" }, flags = { 0 })
public class Mg1280CmFldr extends PluginForDecrypt {

    public Mg1280CmFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("<title>-- mega\\.1280\\.com -(.*?)-- </title>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<strong class=\"clr04\">(.*?)</strong").getMatch(0);
        br.getPage("http://mega.1280.com/getlinks.php?onLoad=[type Function]");
        String[] linkinformation = br.getRegex("(\\&file_name[0-9]+=.*?\\&file_size[0-9]+=.*?\\&file_linkcode[0-9]+=http://mega\\.1280\\.com/file/.*?\\&)").getColumn(0);
        if (linkinformation.length == 0) linkinformation = br.getRegex("(http://mega\\.1280\\.com/file/[A-Z0-9]+)").getColumn(0);
        if (linkinformation.length == 0) return null;
        for (String data : linkinformation) {
            String filename = new Regex(data, "file_name[0-9]+=(.*?)\\&").getMatch(0);
            String filesize = new Regex(data, "file_size[0-9]+=(.*?)\\&").getMatch(0);
            String dlink = new Regex(data, "(http://mega\\.1280\\.com/file/[A-Z0-9]+)").getMatch(0);
            if (dlink == null) return null;
            DownloadLink aLink = createDownloadlink(dlink);
            if (filename != null && filesize != null) {
                aLink.setName(filename.trim());
                aLink.setDownloadSize(Regex.getSize(filesize));
                aLink.setAvailable(true);
            }
            decryptedLinks.add(aLink);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
