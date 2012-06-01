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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 7387 $", interfaceVersion = 2, names = { "quicklink.me" }, urls = { "http://[\\w\\.]*?quicklink\\.me/\\?l=[\\w]+" }, flags = { 0 })
public class QckLnkM extends PluginForDecrypt {

    public QckLnkM(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        for (int retry = 0; retry < 5; retry++) {
            String loc = br.getRedirectLocation();
            if (loc != null) {
                decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
                return decryptedLinks;
            } else {
                String whatis = br.getRegex("alt=\"What is (.*?) = \"").getMatch(0);
                if (whatis == null) return null;
                String calc[] = whatis.split(" ");
                if (calc.length != 3) return null;
                Form form = br.getForm(0);
                if (calc[1].equalsIgnoreCase("*")) {
                    form.put("__ec_s", "" + (Integer.parseInt(calc[0]) * Integer.parseInt(calc[2])));
                } else if (calc[1].equalsIgnoreCase("+")) {
                    form.put("__ec_s", "" + (Integer.parseInt(calc[0]) + Integer.parseInt(calc[2])));
                } else if (calc[1].equalsIgnoreCase("-")) {
                    form.put("__ec_s", "" + (Integer.parseInt(calc[0]) - Integer.parseInt(calc[2])));
                } else if (calc[1].equalsIgnoreCase("/")) {
                    form.put("__ec_s", "" + (Double.parseDouble(calc[0]) / Double.parseDouble(calc[2])));
                }
                br.submitForm(form);
            }
        }
        return null;
    }

    // @Override

}
