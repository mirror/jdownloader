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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xhamster.com" }, urls = { "http://[\\w\\.]*?xhamster\\.com/photos/gallery/[0-9]+-[0-9]+\\.html" }, flags = { 0 })
public class XHamsterGallery extends PluginForDecrypt {

    public XHamsterGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String fpname = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (fpname == null) fpname = br.getRegex("width='100%'><tr><td >(.*?)>").getMatch(0);
        /* Error handling */
        if (br.containsHTML("Sorry, no photos found")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String[] redirectLinks = br.getRegex("align=\"center\" width=\"100%\">.*?<a href=\"(http.*?)\"").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) redirectLinks = br.getRegex("\"(http://xhamster.com/photos/view/.*?\\.html)\"").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) return null;
        progress.setRange(redirectLinks.length);
        for (String link : redirectLinks) {
            br.getPage(link);
            String finallink = br.getRegex("id='imgSized' src='(http.*?)'").getMatch(0);
            if (finallink == null) finallink = br.getRegex("'(http://p[0-9]+\\.xhamster\\.com/.*?\\..*?)'").getMatch(0);
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
            progress.increase(1);
        }
        if (fpname != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpname.trim());
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}