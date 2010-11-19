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
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 7387 $", interfaceVersion = 2, names = { "anilinkz.com" }, urls = { "http://[\\w\\.]*?anilinkz\\.com/.+/.+" }, flags = { 0 })
public class AniLinkzCom extends PluginForDecrypt {

    public AniLinkzCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // this.br.setDebug(true);
        this.br.getPage(parameter);
        // get Mirrors
        final int mirrorCount = this.br.getRegex("Source \\d+").count();
        // get unescapestring
        final List<String> unescape = new ArrayList<String>();
        String[] dllinks;
        for (int i = 2; i <= mirrorCount; i++) {
            final String escapeAll = this.br.getRegex("escapeall\\('(.*)'\\)\\)\\);").getMatch(0).replaceAll("[A-Z~!@#\\$\\*\\{\\}\\[\\]\\-\\+\\.]?", "");
            unescape.add(Encoding.htmlDecode(escapeAll));
            dllinks = new Regex(unescape.get(i - 2), "file=(.*?)\"").getColumn(0);
            if (dllinks.length > 0) {
                for (final String dllink : dllinks) {
                    System.out.println("Mirror " + (i - 1) + ": " + dllink);
                    final DownloadLink dl = this.createDownloadlink(dllink.trim());
                    decryptedLinks.add(dl);
                }
            }
            this.br.getPage(parameter + i + "/");
        }
        if (decryptedLinks.isEmpty()) { throw new DecrypterException("Out of date!"); }
        return decryptedLinks;
    }
}