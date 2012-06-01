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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 7387 $", interfaceVersion = 2, names = { "hiddenip.net" }, urls = { "http://[\\w\\.]*?hiddenip\\.net/.*?q=[0-9A-Za-z|]+.+" }, flags = { 0 })
public class HddnPnt extends PluginForDecrypt {

    public HddnPnt(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        this.setBrowserExclusive();
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(parameter.toString());
        String redirect = br.getRegex("form method=\"post\" action=\"http://hiddenip.net/index.php\">.*?<a href=\"(.*?)\">Address").getMatch(0);
        if (redirect == null) return null;
        ret.add(this.createDownloadlink(redirect));
        return ret;
    }

}
