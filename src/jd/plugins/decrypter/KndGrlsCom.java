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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kindgirls.com" }, urls = { "http://(www\\.)?kindgirls\\.com/gallery/.*?/[a-z0-9]+[0-9_]+/[a-z0-9]+/\\d+/\\d+" }, flags = { 0 })
public class KndGrlsCom extends PluginForDecrypt {

    public KndGrlsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String[] links = br.getRegex("/></a><br /><a href=\"(/.*?)\"").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("\"(/gal-\\d+/[a-z0-9]+_\\d+/.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String finallink : links)
            decryptedLinks.add(createDownloadlink("directhttp://http://www.kindgirls.com" + finallink));

        return decryptedLinks;
    }

}
