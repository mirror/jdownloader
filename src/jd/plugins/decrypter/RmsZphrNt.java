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
import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "roms.zophar.net" }, urls = { "http://[\\w\\.]*?roms\\.zophar\\.net/(.+)/(.+\\.7z)" }, flags = { 0 })
public class RmsZphrNt extends PluginForDecrypt {

    static private final Pattern patternSupported = Pattern.compile("http://[\\w.]*?roms\\.zophar\\.net/(.+)/(.+\\.7z)", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternDownload = Pattern.compile("http://[\\w.]*?roms\\.zophar\\.net/download-file/([0-9]{1,})", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternFilesize = Pattern.compile("http://[\\w.]*?roms\\.zophar\\.net/download-file/[0-9]{1,}\"><b>.+</b></a> \\(([0-9]{1,}\\.[0-9]{1,} (GB|MB|KB|B))\\)</p>", Pattern.CASE_INSENSITIVE);

    public RmsZphrNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(param.toString());

        String filename = new Regex(parameter, patternSupported).getMatch(1);
        String file = br.getRegex(patternDownload).getMatch(0);
        long filesize = SizeFormatter.getSize(br.getRegex(patternFilesize).getMatch(0));
        DownloadLink dlLink = createDownloadlink("http://roms.zophar.net/download-file/" + file);
        dlLink.setDownloadSize(filesize);
        dlLink.setName(filename);

        decryptedLinks.add(dlLink);

        return decryptedLinks;
    }

    // @Override

}
