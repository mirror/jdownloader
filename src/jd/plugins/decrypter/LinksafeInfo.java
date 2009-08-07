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
import jd.http.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linksafe.info" }, urls = { "http://[\\w\\.]*?linksafe\\.info/[^\\s^/]+"}, flags = { 0 })


public class LinksafeInfo extends PluginForDecrypt {

    public LinksafeInfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.setFollowRedirects(true);
        br.getPage(parameter);

        String matchText = "posli\\(\\\"([0-9]+)\\\",\\\"([0-9]+)\\\"\\)";

        String downloadId = br.getRegex(matchText).getMatch(1);

        String[] fileIds = br.getRegex(matchText).getColumn(0);
        progress.setRange(fileIds.length);
        for (String fileId : fileIds) {
            br.getPage("http://www.linksafe.info/posli.php?match=" + fileId + "&id=" + downloadId);
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(br.getURL())));
            progress.increase(1);
        }

        return decryptedLinks;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
