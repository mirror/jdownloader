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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "funkyupload.com" }, urls = { "http://[\\w\\.]*?funkyupload\\.com/files/[0-9A-Z]{8}/.*|http://funkyupload.com/redirect/[0-9A-Z]{8}/\\d+" }, flags = { 0 })
public class FnkypldCm extends PluginForDecrypt {

    public FnkypldCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String hosturl = null;
        String[] links = null;
        String parameter = param.toString();
        String uid = new Regex(parameter, "/files/([0-9A-Z]{8})/").getMatch(0);

        if (parameter.matches(".*?/files/.*?")) {

            br.getPage("http://funkyupload.com/status.php?uid=" + uid);
            links = br.getRegex("<a href=(/redirect/[0-9A-Z]{8}/\\d+) target=_blank>Click Here</a>").getColumn(0);
            if (links.length == 0) return null;
            for (String redirecturl : links) {
                br.getPage(redirecturl);
                hosturl = br.getRegex("<frame name=\"main\" src=\"(.*?)\">").getMatch(0);
                decryptedLinks.add(createDownloadlink(hosturl));
            }
        } else if (parameter.matches(".*?/redirect/.*?")) {
            br.getPage("http://funkyupload.com/files/" + uid + "/");
            br.getPage(parameter);
            hosturl = br.getRegex("<frame name=\"main\" src=\"(.*?)\">").getMatch(0);
            decryptedLinks.add(createDownloadlink(hosturl));
        }

        return decryptedLinks;
    }

    // @Override

}
