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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidshare.com" }, urls = { "http://[\\w\\.]*?rapidshare\\.com/(users/[A-Z0-9]+(\\&pw=.+)?|#!linklist\\|[A-Z0-9]+)" }, flags = { 0 })
public class RpdshrCmFldr extends PluginForDecrypt {

    private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

    public RpdshrCmFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String parameter = param.toString();
        String id = new Regex(parameter, "http://[\\w\\.]*?rapidshare\\.com/users/([A-Z0-9]+)(\\&pw=.+)?").getMatch(0);
        if (id == null) id = new Regex(parameter, "http://[\\w\\.]*?rapidshare\\.com/#!linklist\\|([A-Z0-9]+)").getMatch(0);
        String page = br.getPage("http://rapidshare.com/cgi-bin/rsapi.cgi?sub=viewlinklist_v1&linklist=" + id + "&cbf=RSAPIDispatcher&cbid=1");
        page = page.replaceAll("\\\\\"", "\"");
        String links[][] = new Regex(page, "\"(\\d+)\",\"(\\d+)\",\"(\\d+)\",\"(.*?)\",\"(\\d+)\"").getMatches();
        for (String[] link : links) {
            decryptedLinks.add(this.createDownloadlink("http://rapidshare.com/files/" + link[2] + "/" + link[3]));
        }

        return decryptedLinks;
    }

}
