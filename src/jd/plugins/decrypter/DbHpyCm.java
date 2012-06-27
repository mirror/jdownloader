//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dubhappy.com" }, urls = { "http://(www\\.)?dubhappy\\.com/[a-z0-9_\\-]+" }, flags = { 0 })
public class DbHpyCm extends PluginForDecrypt {
    public DbHpyCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setCookie("http://dubhappy.com", "lang", "english");
        br.getPage(parameter);

        if (br.containsHTML(">Error 404 - Not Found<")) {
            logger.warning("Invalid URL : " + parameter);
            return null;
        }

        String fpName = br.getRegex("<span class=\"PostHeader\">(.*?)</span>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>Watch(.*?) |").getMatch(0);
        if (fpName == null) {
            logger.warning("Can not find fpName : " + parameter);
            return null;
        }

        String getJS = br.getRegex("(<script id=\"js2\".*?1</script>)").getMatch(0);
        if (getJS == null) {
            logger.warning("Can not find JS containing links : " + parameter);
            return null;
        }

        String[] links = new Regex(getJS, "src=\"(https?://[^\"]+)").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String link : links)
            decryptedLinks.add(createDownloadlink(link));

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}