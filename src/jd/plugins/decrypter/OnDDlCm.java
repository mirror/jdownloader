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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "oneddl.com" }, urls = { "http://[\\w\\.]*?oneddl\\.com(/.+/.*)" }, flags = { 0 })
public class OnDDlCm extends PluginForDecrypt {

    public OnDDlCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> passwords;
        String parameter = param.toString();
        br.getPage(parameter);
        String content = br.getRegex(Pattern.compile("<div id=\"main\">(.*?)<!-- related posts START -->", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (content == null) content = br.getRegex(Pattern.compile("<div class=\"postarea\">(.*?)<!-- You can start editing here\\. -->", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (content == null) return null;
        passwords = HTMLParser.findPasswords(content);
        String[] links = new Regex(content, "<a href=\"(http://.*?)\"", Pattern.CASE_INSENSITIVE).getColumn(0);
        if (links == null || links.length == 0) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        for (String link : links) {
            if (!new Regex(link, this.getSupportedLinks()).matches() && DistributeData.hasPluginFor(link, true)) {
                DownloadLink dLink = createDownloadlink(link);
                dLink.addSourcePluginPasswordList(passwords);
                decryptedLinks.add(dLink);
            }
        }
        return decryptedLinks;
    }

}
