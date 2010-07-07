//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagefap.com" }, urls = { "http://[\\w\\.]*?imagefap\\.com/(gallery\\.php\\?gid=.+|gallery/.+)" }, flags = { 0 })
public class MgfpCm extends PluginForDecrypt {

    public MgfpCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replaceAll("view\\=[0-9]+", "view=2");
        if (!parameter.contains("view=2")) parameter += "&view=2";
        br.getPage(parameter);
        String links[] = br.getRegex("image\\.php\\?id=(.*?)\">").getColumn(0);
        if (links == null || links.length == 0) return null;
        double counter = 0.001;
        for (String element : links) {
            DownloadLink link = createDownloadlink("http://imagefap.com/image.php?id=" + element);
            link.setProperty("orderid", new Regex(String.format("&orderid=%.3f&", counter), "\\&orderid=0\\.(\\d+)").getMatch(0));
            decryptedLinks.add(link);
            counter += 0.001;
        }
        return decryptedLinks;
    }

}
