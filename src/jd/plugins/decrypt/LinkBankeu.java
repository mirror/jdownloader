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

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;

public class LinkBankeu extends PluginForDecrypt {
    private static final String CHECK_MIRRORS = "CHECK_MIRRORS";
    static private final String host = "LinkBank.eu";

    private static final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?linkbank\\.eu/show\\.php\\?show=\\d+", Pattern.CASE_INSENSITIVE);

    public LinkBankeu() {
        super();
        setConfigElements();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        String page = br.getPage(parameter);
        String[][] links = new Regex(page, Pattern.compile("onclick='posli\\(\"([\\d]+)\",\"([\\d]+)\"\\);'", Pattern.CASE_INSENSITIVE)).getMatches();
        String[] mirrors = new Regex(page, Pattern.compile("onclick='mirror\\(\"(.*?)\"\\);'", Pattern.CASE_INSENSITIVE)).getColumn(-1);
        for (String[] element : links) {
            br.getPage("http://www.linkbank.eu/posli.php?match=" + element[0] + "&id=" + element[1]);
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
        }
        if (getPluginConfig().getBooleanProperty(CHECK_MIRRORS, false) == true) {
            for (String element : mirrors) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(element)));
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

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CHECK_MIRRORS, JDLocale.L("plugins.decrypt.linkbankeu", "Check Mirror Links")).setDefaultValue(false));
    }
}
