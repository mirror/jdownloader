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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videarn.com" }, urls = { "http://(www\\.)?videarn\\.com/video\\.php\\?id=\\d+" }, flags = { 0 })
public class VidEarnDecrypter extends PluginForDecrypt {

    public VidEarnDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("<h3 class=\"page\\-title\"><strong>(.*?)</strong></h3>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>Video \\- (.*?)</title>").getMatch(0);
            if (fpName == null) {
                fpName = new Regex(parameter, "videarn\\.com/video\\.php\\?id=(\\d+)").getMatch(0);
            }
        }
        fpName = fpName.trim();
        String additionalDownloadlink = br.getRegex("\"(http://(www\\.)?filearn\\.com/files/get/.*?)\"").getMatch(0);
        if (additionalDownloadlink == null) additionalDownloadlink = br.getRegex("<div class=\"video\\-actions\">[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
        if (additionalDownloadlink != null) {
            DownloadLink xdl = createDownloadlink(additionalDownloadlink);
            xdl.setProperty("videarnname", fpName);
            decryptedLinks.add(xdl);
        }
        decryptedLinks.add(createDownloadlink(parameter.replace("videarn.com/", "videarndecrypted.com/")));
        FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
