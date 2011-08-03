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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xhamster.com" }, urls = { "http://(www\\.)?xhamster\\.com/photos/gallery/[0-9]+/.*?\\.html" }, flags = { 0 })
public class XHamsterGallery extends PluginForDecrypt {

    public XHamsterGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("Sorry, no photos found")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpname = br.getRegex("title\\'Start SlideShow\\'></a><h1>(.*?)</h1>").getMatch(0);
        if (fpname == null) fpname = br.getRegex("<title>(.*?) \\- \\d+ Pics \\- xHamster\\.com</title>").getMatch(0);
        String[] redirectLinks = br.getRegex("\"(http://xhamster.com/photos/view/.*?\\.html)\"").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) redirectLinks = br.getRegex("<div class=\\'galleryItem  galleryItemList imgItem \\' id=\\'i_\\d+\\'>[\t\n\r ]+<table cellpadding=\"0\" cellspacing=\"0\" class=\\'img\\'>[\t\n\r ]+<tr><td valign=\"middle\" align=\"center\" width=\"100%\">[\t\n\r ]+<a href=\"(http://.*?)\"").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) return null;
        progress.setRange(redirectLinks.length);
        for (String link : redirectLinks) {
            br.getPage(link);
            String finallink = br.getRegex("title=\\'Click to Next Photo \\&gt\\&gt\\'>[\t\n\r ]+<img  src=\\'(http://.*?)\\'").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("valign=\"middle\" align=\"center\" id=\\'slideTd\\'>[\t\n\r ]+<img src=\\'(http://.*?)\\'").getMatch(0);
                if (finallink == null) {
                    finallink = br.getRegex("\\'(http://p\\d+\\-\\d+\\.xhamster\\.com/\\d+/\\d+/.*?)\\'").getMatch(0);
                }
            }
            if (finallink == null) return null;
            DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
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