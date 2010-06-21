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
import java.util.Date;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.DownloadLink.AvailableStatus;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {"boards.4chan.org"}, urls = {"http://[\\w\\.]*?boards\\.4chan\\.org/[0-9a-z]{1,3}/(res/[0-9]+)?"}, flags = {0})
public class Brds4Chnrg extends PluginForDecrypt {

    public Brds4Chnrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        FilePackage fp = FilePackage.getInstance();
        String parameter = param.toString();
        br.getPage(parameter);
        if (parameter.matches("http://[\\w\\.]*?boards\\.4chan\\.org/[0-9a-z]{1,3}/[0-9]*")) {
            String[] threads = br.getRegex("<span id=\"nothread([0-9]+)\"><a href=\"res/").getColumn(0);
            for (String thread : threads) {
                decryptedLinks.add(createDownloadlink(parameter+ "res/" + thread));
            }
        } else {
            String[] images = br.getRegex("(http://[\\w\\.]*?images.4chan\\.org/[0-9a-z]{1,3}/src/[0-9]+\\.(?i:(gif|jpg|png)))").getColumn(0);
            
            if (br.containsHTML("404 - Not Found")) {
                fp.setName("4chan - 404 - Not Found");
                br.getPage("http://www.4chan.org/error/404/rid.php");
                String image404 = br.getRegex("(http://.+)").getMatch(0);
                DownloadLink dl = createDownloadlink(image404);
                dl.setAvailableStatus(AvailableStatus.TRUE);
                dl.setFilePackage(fp);
                decryptedLinks.add(dl);
            } else if (images.length == 0) {
                return null;
            } else {
                String domain = "4chan.org";
                String cat = br.getRegex("<div class=\"logo\">.*?<span>/.{1,3}/ - (.*?)</span>").getMatch(0).replace("&amp;", "&");
                String date = new Date().toString();
                fp.setName(domain + " - " + cat + " - " + date);
                for (String image : images) {
                    DownloadLink dl = createDownloadlink(image);
                    dl.setAvailableStatus(AvailableStatus.TRUE);
                    dl.setFilePackage(fp);
                    decryptedLinks.add(dl);
                }
            }
        }
        return decryptedLinks;
    }

    // @Override

}
