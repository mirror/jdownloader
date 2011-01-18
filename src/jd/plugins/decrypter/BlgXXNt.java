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
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "blog-xx.net" }, urls = { "http://[\\w\\.]*?blog-xx\\.net/wp/(.*?)/" }, flags = { 0 })
public class BlgXXNt extends PluginForDecrypt {

    public BlgXXNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String[] charcodes = br.getRegex(Pattern.compile("document.write\\(String.fromCharCode\\((.*?)\\)\\);", Pattern.DOTALL)).getMatch(0).split(",");
        String decrypted = "";
        for (String charcode : charcodes) {
            decrypted += (char) Integer.valueOf(charcode).intValue();
        }

        String password = new Regex(decrypted, Pattern.compile("<div class=\"d.*?>Password:</div>.*?<div class=\"d.*?>(.*?)</div>", Pattern.DOTALL)).getMatch(0);
        String[] links = new Regex(decrypted, "url=(.*?)\" target").getColumn(0);
        progress.setRange(links.length);
        DownloadLink dLink;
        for (String link : links) {
            decryptedLinks.add(dLink = createDownloadlink(link));
            dLink.addSourcePluginPassword(password);
            progress.increase(1);
        }

        return decryptedLinks;
    }

    // @Override

}
