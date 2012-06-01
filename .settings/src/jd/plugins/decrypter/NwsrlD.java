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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 7387 $", interfaceVersion = 2, names = { "newsurl.de" }, urls = { "http://[\\w\\.]*?newsurl.de/.*" }, flags = { 0 })
public class NwsrlD extends PluginForDecrypt {
    final static private Object LOCK = new Object();
    private DownloadLink dl;

    public NwsrlD(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        synchronized (LOCK) {
            br.getPage(parameter);
            String link = br.getRedirectLocation();
            if (link == null) link = br.getRegex("<a href=\"([^\"]*)\" .*?(style|onClick)").getMatch(0);
            if (link == null) link = br.getRegex("<META HTTP-EQUIV=\"Refresh\" .*? URL=(.*?)\">").getMatch(0);
            if (link == null) link = br.getRegex("onClick=\"top\\.location='(.*?)'\">").getMatch(0);
            if (link == null) link = br.getRegex("<iframe name='redirectframe' id='redirectframe'.*?src='(.*?)'.*?></iframe>").getMatch(0);
            if (link == null) return null;
            decryptedLinks.add(dl = createDownloadlink(link));
            dl.setProperties(param.getProperties());
            dl.setProperty("referer", param.getCryptedUrl());
        }
        return decryptedLinks;
    }

    // @Override

}
