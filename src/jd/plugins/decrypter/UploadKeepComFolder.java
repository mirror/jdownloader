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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadkeep.com" }, urls = { "http://[\\w\\.]*?uploadkeep\\.com/.*?/.+" }, flags = { 0 })
public class UploadKeepComFolder extends PluginForDecrypt {

    public UploadKeepComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        boolean failed = false;
        if (parameter.matches(".*?uploadkeep\\.com/[a-z0-9]{12}/.*?")) {
            decryptedLinks.add(createDownloadlink(parameter.replace("uploadkeep.com", "dweg6532401238ohXfrthCSWEwerhtetUE")));
        } else {
            br.getPage(parameter);
            String[] links = br.getRegex("<Table class=\"file_block\"(.*?)</Table>").getColumn(0);
            if (links == null || links.length == 0) {
                failed = true;
                links = br.getRegex("\"(http://uploadkeep\\.com/[a-z0-9]{12}/.*?)\"").getColumn(0);
            }
            if (links == null || links.length == 0) return null;
            for (String data : links) {
                if (failed) {
                    decryptedLinks.add(createDownloadlink(data.replace("uploadkeep.com", "dweg6532401238ohXfrthCSWEwerhtetUE")));
                } else {
                    String filename = new Regex(data, "href=\".*?\">(.*?)</a>").getMatch(0);
                    String filesize = new Regex(data, "class=\"img\"><img src=\".*?\"><br>(.*?)</TD>").getMatch(0);
                    String dlink = new Regex(data, "\"(http://uploadkeep\\.com/[a-z0-9]{12}/.*?)\"").getMatch(0);
                    if (dlink == null) dlink = new Regex(data, "href=\"(.*?)\"").getMatch(0);
                    if (dlink == null) return null;
                    DownloadLink aLink = createDownloadlink(dlink.replace("uploadkeep.com", "dweg6532401238ohXfrthCSWEwerhtetUE"));
                    if (filename != null) aLink.setName(filename.trim());
                    if (filesize != null) aLink.setDownloadSize(Regex.getSize(filesize));
                    if (filename != null && filesize != null) aLink.setAvailable(true);
                    decryptedLinks.add(aLink);
                }
            }
        }
        return decryptedLinks;
    }

}
