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
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class FileFactoryFolder extends PluginForDecrypt {

    public FileFactoryFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String pages[] = br.getRegex(Pattern.compile("<a href=\"\\?page=(\\d+)\">", Pattern.CASE_INSENSITIVE)).getColumn(0);
        progress.setRange(0);
        add(decryptedLinks, progress);
        if (pages.length > 1) {
            for (int i = 2; i <= Integer.parseInt(pages[pages.length - 1]); i++) {
                br.getPage(parameter + "/?page=" + i);
                add(decryptedLinks, progress);
            }
        }
        return decryptedLinks;
    }

    private void add(ArrayList<DownloadLink> declinks, ProgressController progress) {
        String links[] = br.getRegex(Pattern.compile("<td class=\"name\"><a href=\"(/file/.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
        progress.increase(links.length);
        for (String element : links) {
            declinks.add(createDownloadlink("http://www.filefactory.com" + element));
            progress.increase(1);
        }
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}