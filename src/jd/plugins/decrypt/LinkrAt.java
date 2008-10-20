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

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class LinkrAt extends PluginForDecrypt {
    private static final Pattern PATTERN_MAIN_FRAME = Pattern.compile("<frame src=\"(.+?)\" name=\"Mainframe\" scrolling=\"no\">");
    private static final Pattern PATTERN_DOWNLOAD_LINK = Pattern.compile("window.open\\('(.+?)'\\);");

    public LinkrAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = cryptedLink.getCryptedUrl();
        String redirect = null;
        while (url != null) {
            redirect = !url.equals(cryptedLink.getCryptedUrl()) ? url : "keine rekursion";
            br.getPage(url);
            url = br.getRedirectLocation();
        }
        br.getPage(redirect);
        for (Form form : Form.getForms(br.getRequest())) {
            String mainFrame = br.getPage(new Regex(br.submitForm(form), PATTERN_MAIN_FRAME).getMatch(0));
            decryptedLinks.add(createDownloadlink(new Regex(mainFrame, PATTERN_DOWNLOAD_LINK).getMatch(0)));
        }
        return decryptedLinks.size() > 0 ? decryptedLinks : null;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
