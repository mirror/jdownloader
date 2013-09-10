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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

//http://www.hornoxe.com/hornoxe-com-picdump-273/
@DecrypterPlugin(revision = "$Revision: 18325 $", interfaceVersion = 2, names = { "hornoxe.com" }, urls = { "5u90hfikrnhirtnDELETE_ME_DUPLICATEfrh5ujbrhukfswikninehntimbno" }, flags = { 0 })
public class HrnxCm extends PluginForDecrypt {

    public HrnxCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String[] urls = br.getRegex("ngg\\-gallery\\-thumbnail\"\\s*>.*?<a href\\=\"(http.*?\\.jpg)\"").getColumn(0);
        String title = br.getRegex("<meta property=\"og\\:title\" content=\"(.*?)\" \\/>").getMatch(0);
        FilePackage fp = null;
        if (title != null) {
            fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(title.trim()));
            fp.addLinks(decryptedLinks);
        }

        add(decryptedLinks, urls, fp);
        String[] pageqs = br.getRegex("\"page-numbers\" href=\"(.*?nggpage\\=\\d+)").getColumn(0);

        for (String page : pageqs) {
            Browser pageBrowser = br.cloneBrowser();
            pageBrowser.getPage(page);

            urls = pageBrowser.getRegex("ngg\\-gallery\\-thumbnail\"\\s*>.*?<a href\\=\"(http.*?\\.jpg)\"").getColumn(0);
            add(decryptedLinks, urls, fp);
        }

        return decryptedLinks;
    }

    private void add(ArrayList<DownloadLink> decryptedLinks, String[] urls, FilePackage fp) {

        for (String url : urls) {
            DownloadLink link = createDownloadlink("directhttp://" + url);
            fp.add(link);
            decryptedLinks.add(link);
            try {
                distribute(link);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}