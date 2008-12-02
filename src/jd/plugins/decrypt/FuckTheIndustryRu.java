//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.decrypt;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.io.JDIO;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class FuckTheIndustryRu extends PluginForDecrypt {

    private Pattern patternDLC = Pattern.compile("href=\"(http://92\\.241\\.164\\.63/store/_dlc//forcedl\\.php\\?file=(.*?)\\.dlc)\"", Pattern.CASE_INSENSITIVE);
    private Pattern patternPW = Pattern.compile("\\<input.*?id=\"pw_2_copy\".*?value=\"(.*?)\".*\\>", Pattern.CASE_INSENSITIVE);

    public FuckTheIndustryRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        String name = br.getRegex(patternDLC).getMatch(1);
        String link = br.getRegex(patternDLC).getMatch(0);
        String pass = br.getRegex(patternPW).getMatch(0);

        File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
        Vector<DownloadLink> links = null;
        Browser.download(container, link);
        links = JDUtilities.getController().getContainerLinks(container);
        container.delete();

        if (links != null) {
            FilePackage fp = new FilePackage();
            fp.setName(name);
            fp.setPassword(pass);
            fp.setComment("from " + parameter);
            for (DownloadLink dLink : links) {
                dLink.setSourcePluginComment("from " + parameter);
                fp.add(dLink);
            }
            decryptedLinks.addAll(links);
        } else {
            return null;
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}