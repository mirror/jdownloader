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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class Divxvid extends PluginForDecrypt {
    static private final String host = "dxp.divxvid.org";

    static private final Pattern patternSupported = Pattern.compile("http://dxp\\.divxvid\\.org/[a-zA-Z0-9]{32}\\.html", Pattern.CASE_INSENSITIVE);

    private static Vector<String> passwords = new Vector<String>();

    private Pattern premiumdownloadlocation = Pattern.compile("form name=\"dxp\" action=\"(.*)\" method=\"post\"", Pattern.CASE_INSENSITIVE);

    public Divxvid() {
        super();
        passwords.add("dxd-tivi");
        passwords.add("dxp.divxvid.org");
        passwords.add("dxp");
        passwords.add("dxp-tivi");
        passwords.add("DivXviD");
        passwords.add("dxd.dl.am");
    }

    /*
     * Diese wichtigen Infos sollte man sich unbedingt durchlesen
     */

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);

            String cookie = HTTP.postRequestWithoutHtmlCode(url, null, null, null, false).getCookie();

            String hash = url.getFile();
            hash = hash.substring(1, hash.lastIndexOf("."));
            RequestInfo requestInfo = HTTP.getRequest((new URL("http://dxp.divxvid.org/script/old_loader.php")), cookie, parameter, false);

            String strgate = new Regex(requestInfo.getHtmlCode(), Pattern.compile("httpRequestObject.open.'POST', '([^\\s\"]*)'\\);", Pattern.CASE_INSENSITIVE)).getMatch(0);
            String outl = new Regex(requestInfo.getHtmlCode(), Pattern.compile("rsObject = window.open.\"([/|.|a-zA-Z0-9|_|-]*)", Pattern.CASE_INSENSITIVE)).getMatch(0);

            requestInfo = HTTP.postRequest((new URL("http://dxp.divxvid.org/" + strgate)), null, parameter, null, "hash=" + hash, false);

            int countHits = new Regex(requestInfo.getHtmlCode(), Pattern.compile("value=\"download\" onclick=\"javascript:download", Pattern.CASE_INSENSITIVE)).count();
            progress.setRange(countHits);
            for (int i = 0; i < countHits; i++) {
                requestInfo = HTTP.postRequestWithoutHtmlCode((new URL(new Regex(HTTP.getRequest((new URL("http://dxp.divxvid.org" + outl + "," + i + ",1," + hash + ".rs")), cookie, parameter, true).getHtmlCode(), premiumdownloadlocation).getMatch(0))), null, null, null, false);
                if (requestInfo != null) {
                    progress.increase(1);
                    DownloadLink dl_link = createDownloadlink(requestInfo.getLocation());
                    dl_link.addSourcePluginPasswords(passwords);
                    decryptedLinks.add(dl_link);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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
