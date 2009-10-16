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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filestube.com" }, urls = { "http://[\\w\\.]*?filestube\\.com/.+" }, flags = { 0 })
public class FlStbCm extends PluginForDecrypt {

    public FlStbCm(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        FilePackage fp = FilePackage.getInstance();
        br.getPage(parameter.toString());
        String fpName = br.getRegex("<title>(.*?)- Download").getMatch(0).trim();
        //Hmm this plugin should always have a name with that mass of alternative ways to get the name
        if (fpName == null) {
            fpName = br.getRegex("content=\"Download(.*?)from").getMatch(0).trim();
            if (fpName == null) {
                fpName = br.getRegex("\">Download:(.*?)</h2>").getMatch(0).trim();
                if (fpName == null) {
                    fpName = br.getRegex("widgetTitle: '(.*?)',").getMatch(0).trim();
                    if (fpName == null) {
                        fpName = br.getRegex("&quot;\\](.*?)\\[/url\\]\"").getMatch(0).trim();
                    }
                }
            }
        }
        fp.setName(fpName);
        String temp = br.getRegex(Pattern.compile("und:#fff;overflow: auto\">(.*?)<br /></pre></", Pattern.DOTALL)).getMatch(0);
        if (temp == null) return null;
        String[] links = temp.split("<br />");
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String data : links) {
            decryptedLinks.add(createDownloadlink(data));
            progress.increase(1);
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
