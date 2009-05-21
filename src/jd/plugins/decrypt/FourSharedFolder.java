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

package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

public class FourSharedFolder extends PluginForDecrypt {

    public FourSharedFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String script = br.getRegex("src=\"(/account/homeScript.*?)\"").getMatch(0);
        br.cloneBrowser().getPage("http://4shared.com" + script);
        String pages[] = br.getRegex("javascript:pagerShowFiles\\((\\d+)\\);").getColumn(0);
        String burl = br.getRegex("var bUrl = \"(/account/changedir.jsp\\?sId=.*?)\";").getMatch(0);
        String name = br.getRegex("hidden\" name=\"defaultZipName\" value=\"(.*?)\">").getMatch(0);
        String[] links = br.getRegex("<a href=\"(.*?4shared.com/file/.*?)\"").getColumn(0);
        FilePackage fp = FilePackage.getInstance();
        fp.setName(name);
        if (links.length == 0) return null;
        for (String dl : links) {
            decryptedLinks.add(this.createDownloadlink(dl));
        }
        String changedir = br.getRegex("var currentDirId = (\\d+);").getMatch(0);
        for (int i = 0; i < pages.length - 1; i++) {
            String url = "http://4shared.com" + burl + "&ajax=true&changedir=" + changedir + "&firstFileToShow=" + pages[i] + "&sortsMode=NAME&sortsAsc=&random=0.1863370989474954";
            br.getPage(url);
            links = br.getRegex("<a href=\"(.*?4shared.com/file/.*?)\"").getColumn(0);
            if (links.length == 0) return null;
            for (String dl : links) {
                decryptedLinks.add(this.createDownloadlink(dl));
            }
        }
        fp.addLinks(new ArrayList<DownloadLink>(decryptedLinks));
        return decryptedLinks;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
