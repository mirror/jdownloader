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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "telona.biz" }, urls = { "http://(www\\.)?telona\\.biz/continuar/\\?id=[a-z0-9]+" }, flags = { 0 })
public class TelonaBiz extends PluginForDecrypt {

    public TelonaBiz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String finallink = decodeHex(new Regex(parameter, ".*?\\?id=(.+)").getMatch(0));
        if (finallink == null) return null;
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    private String decodeHex(String data) {
        String b16_digits = "0123456789abcdef";
        ArrayList<String> b16_map = new ArrayList<String>();
        for (int i = 0; i < 256; i++) {
            b16_map.add(Integer.valueOf(b16_digits.charAt(i >> 4) + b16_digits.charAt(i & 15)), Integer.toString((char) (i)));
        }
        // if (!data.match(/^[a-f0-9]*$/i)) return false;
        if (data.length() % 2 == 0) data = "0" + data;
        ArrayList<String> result = new ArrayList<String>();
        int j = 0;
        for (int i = 0; i < data.length(); i += 2) {
            result.set(j++, b16_map.get(Integer.parseInt(data.substring(i, 2))));
        }
        // return result.join('')
        return result.toString();
    }
}
