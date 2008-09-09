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

import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class VetaXin extends PluginForDecrypt {

    static private final String host = "vetax.in";
    static private final Pattern patternSupported_Download = Pattern.compile("http://[\\w\\.]*?vetax\\.in/(dload|mirror)/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupported_Page = Pattern.compile("http://[\\w\\.]*?vetax\\.in/view/\\d+", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupported = Pattern.compile(patternSupported_Page.pattern() + "|" + patternSupported_Download.pattern(), Pattern.CASE_INSENSITIVE);

    public VetaXin(String cfgName){
        super(cfgName);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        if (new Regex(parameter, patternSupported_Download).matches()) {
            String links[] = br.getRegex(Pattern.compile("<input name=\"feld.*?\" value=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
            if (links == null) { return null; }
            progress.setRange(links.length);
            for (String element : links) {
                decryptedLinks.add(createDownloadlink(element));
                progress.increase(1);
            }
        } else if (new Regex(parameter, patternSupported_Page).matches()) {
            String pw = br.getRegex(Pattern.compile("<strong>Passwort:</strong></td>.*?<strong>(.*?)</strong>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            String links[] = br.getRegex(Pattern.compile("<a href=\"(/(dload|mirror)/.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
            String rsdf = br.getRegex(Pattern.compile("<a href=\"(/crypt\\.php\\?.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (rsdf != null) {
                File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf");
                if (Browser.download(container, br.openGetConnection("http://vetax.in" + rsdf))) {
                    Vector<DownloadLink> dl_links = JDUtilities.getController().getContainerLinks(container);
                    container.delete();
                    for (DownloadLink dl_link : dl_links) {
                        dl_link.addSourcePluginPassword(pw);
                        decryptedLinks.add(dl_link);
                    }
                }
            }
            progress.setRange(links.length);
            for (String element : links) {
                DownloadLink dl_link = createDownloadlink("http://vetax.in" + element);
                dl_link.addSourcePluginPassword(pw);
                decryptedLinks.add(dl_link);
                progress.increase(1);
            }
        }

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
