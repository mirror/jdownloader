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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "putenbrust.net" }, urls = { "http://putenbrust\\.net/download\\.php\\?file=.*" }) 
public class PutenbrustNet extends PluginForDecrypt {

    public PutenbrustNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final Pattern linkPattern = Pattern.compile(".*(download\\.php\\?go=\\d+&amp;file=\\d+&amp;mirror=\\d+).*");
    private final Pattern namePattern = Pattern.compile("<title>.*\\|(.*)</title>");

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String pageContent = br.getPage(param.getCryptedUrl());
        final Matcher nameMatcher = namePattern.matcher(pageContent);
        final Matcher linkMatcher = linkPattern.matcher(pageContent);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        String dlName = "";
        if (nameMatcher.find()) {
            dlName = nameMatcher.group(1).trim();
        }

        while (linkMatcher.find()) {
            final String thisMatch = linkMatcher.group(1);
            final String intermediateURL = "http://putenbrust.net/" + thisMatch.replace("&amp;", "&");

            final Browser b2 = new Browser();
            b2.setFollowRedirects(true);
            final String intermediatePage = b2.getPage(intermediateURL);

            final String finalURL = b2.getURL();
            final DownloadLink decryptedLink = createDownloadlink(finalURL);
            decryptedLink.setName(dlName);
            decryptedLinks.add(decryptedLink);
        }
        return decryptedLinks;
    }
}
