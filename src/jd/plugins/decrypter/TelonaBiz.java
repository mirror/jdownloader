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

    public TelonaBiz(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String decodeHex(String data) {
        try {
            final StringBuilder sb = new StringBuilder();
            if (data.length() % 2 == 0) {
                data = data + "0";
            }
            for (int i = 0; i < data.length() - 1; i += 2) {
                final String output = data.substring(i, i + 2);
                sb.append((char) Integer.parseInt(output, 16));
            }
            return sb.toString();
        } catch (final Throwable e) {
            // !hexstring
        }
        return null;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String finallink = decodeHex(new Regex(parameter, ".*?\\?id=(.+)").getMatch(0));
        if (finallink == null) { return null; }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
