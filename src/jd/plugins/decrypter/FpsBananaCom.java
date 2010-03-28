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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fpsbanana.com" }, urls = { "http://[\\w\\.]*?fpsbanana\\.com/(maps/download|maps)/\\d+" }, flags = { 0 })
public class FpsBananaCom extends PluginForDecrypt {

    public FpsBananaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        if (!parameter.contains("/download/")) parameter = parameter.replace("/maps/", "/maps/download/");
        br.getPage(parameter);
        if (br.containsHTML("(This Map doesn't have a file|None found!)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("class=\"bold tbig\">(.*?)</span").getMatch(0);
        String[] links = br.getRegex("path=(http.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String alink : links) {
            br.getPage("http://www.fpsbanana.com/mirrors/startdl/?path=" + alink);
            String finallink = br.getRedirectLocation();
            if (finallink == null)
                logger.info("Mirror " + br.getHost() + " for link " + parameter + " doesn't seem to work, decrypt goes on.");
            else
                decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
            progress.increase(1);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
