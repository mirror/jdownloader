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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "revision3.com" }, urls = { "http://(www\\.)?revision3\\.com/[a-z0-9]+/[a-z0-9-]+" }, flags = { 0 })
public class RevisionThreeCom extends PluginForDecrypt {

    public RevisionThreeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("<title>(.*?) - ").getMatch(0);
        if (fpName != null) fpName = fpName.replace("...", "");
        String[] allLinks = br.getRegex("\"(http://(www\\.)?podtrac\\.com/pts/redirect\\.[a-z0-9]+/videos\\.revision3\\.com/revision3/.*?)\"").getColumn(0);
        if (allLinks == null || allLinks.length == 0) return null;
        progress.setRange(allLinks.length);
        for (String singleLink : allLinks) {
            br.getPage(singleLink);
            String finallink = br.getRedirectLocation();
            if (finallink == null) return null;
            DownloadLink fina = createDownloadlink("directhttp://" + finallink);
            if (fpName != null) fina.setFinalFileName(fpName + finallink.substring(finallink.length() - 4, finallink.length()));
            decryptedLinks.add(fina);
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
