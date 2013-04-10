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

// http://[\\w\\.]*?fdnlinks\\.com/link/[a-zA-Z0-9]+
// http://fdnlinks.com/link/c866cc4e84c4a3da6f3d58caaeab11e1
// decompiled http://fdnlinks.com/design/links.swf?a=c866cc4e84c4a3da6f3d58caaeab11e1 with Flare

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fdnlinks.com" }, urls = { "http://[\\w\\.]*?fdnlinks\\.com/link/[\\w]+" }, flags = { 0 })
public class FDNLnksCm extends PluginForDecrypt {

    public FDNLnksCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String id = parameter.substring(parameter.lastIndexOf("/") + 1);
        br.getPage("http://fdnlinks.com/ajax/links.html?a=" + id + "&x=" + ((int) (Math.random() * 1000)));
        String[] cryptedLinks = br.getRegex("\\<link\\>(.*?)\\</link\\>").getColumn(0);

        progress.setRange(cryptedLinks.length);
        for (String link : cryptedLinks) {
            link = this.decrypt(link);
            decryptedLinks.add(createDownloadlink(link));
            progress.increase(1);
        }

        return decryptedLinks;
    }

    private String decrypt(String crypted) {
        int v2;
        String v3 = "";
        String v4 = "";
        String v5 = "";
        String v6 = "NOPQvwxyz012RSTUVWXYZabcdefghijkABCDEFGHIJKLMlmnopqrstu3456789_-";

        int i = 0;
        while (i < crypted.length()) {
            v2 = v6.indexOf(crypted.charAt(i));
            v3 = ("000000" + Integer.toBinaryString(v2));
            v3 = v3.substring(v3.length() - 5, v3.length());
            v4 += v3;
            i++;
        }

        i = 0;
        while (i < v4.length()) {

            if ((i + 8) > v4.length()) {
                v3 = v4.substring(i);
            } else {
                v3 = v4.substring(i, i + 8);
            }

            // convert binary string to int
            int base = 0;
            v2 = 0;
            for (int j = v3.length() - 1; j >= 0; j--) {
                if (v3.charAt(j) == '1') {
                    v2 += Math.round(Math.pow(2, base));
                }
                base++;
            }
            // ---

            v5 += (char) v2;
            i += 8;

        }

        return v5;
    }

    // @Override

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}