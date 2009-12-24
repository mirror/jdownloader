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
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xxx-blog.org" }, urls = { "http://[\\w\\.]*?xxx-blog\\.(org|to)/((share|sto|com-|u|filefactory/|relink/)[\\w\\./-]+|.*?\\.html|blog/(dvd-rips|scenes|amateur-clips|hd-(scenes|movies)|site-rips|image-sets|games)/.+/)" }, flags = { 0 })
public class XXXBlg extends PluginForDecrypt {

    public XXXBlg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        parameter = parameter.substring(parameter.lastIndexOf("http://"));
        br.getPage(parameter);
        if (br.containsHTML("Fehler 404 - Seite nicht gefunden")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (parameter.contains("/blog/")) {
            String fpname = br.getRegex("<title>(.*?)\\| XXX-Blog").getMatch(0);
            if (fpname == null) fpname = br.getRegex("rel=\"bookmark\" title=\"(.*?)\"").getMatch(0);
            String pagepiece = br.getRegex("<strong>(.*?)</a></strong></p>").getMatch(0);
            if (pagepiece == null) pagepiece = br.getRegex("<strong>(.*?)download=highspeed\"").getMatch(0);
            if (pagepiece == null) return null;
            String[] links = HTMLParser.getHttpLinks(pagepiece, "");
            if (links == null || links.length == 0) return null;
            for (String link : links) {
                DownloadLink dlink = createDownloadlink(link);
                dlink.addSourcePluginPassword("xxx-blog.dl.am");
                dlink.addSourcePluginPassword("xxx-blog.org");
                decryptedLinks.add(dlink);
            }
            if (fpname != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpname.trim());
                fp.addLinks(decryptedLinks);
            }
        } else {
            DownloadLink dLink;
            if (br.getRedirectLocation() != null) {
                dLink = createDownloadlink(br.getRedirectLocation());
            } else {
                Form form = br.getForm(0);
                if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                dLink = createDownloadlink(form.getAction(null));
            }
            dLink.addSourcePluginPassword("xxx-blog.dl.am");
            dLink.addSourcePluginPassword("xxx-blog.org");
            decryptedLinks.add(dLink);
        }

        return decryptedLinks;
    }

}
