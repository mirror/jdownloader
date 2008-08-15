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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class Divxvid extends PluginForDecrypt {
    static private final String host = "dxp.divxvid.org";

    static private final Pattern patternSupported = Pattern.compile("http://dxp\\.divxvid\\.org/[a-zA-Z0-9]{32}\\.html", Pattern.CASE_INSENSITIVE);
    private Pattern gate = Pattern.compile("httpRequestObject.open.'POST', '([^\\s\"]*)'\\);", Pattern.CASE_INSENSITIVE);

    private Pattern outputlocation = Pattern.compile("rsObject = window.open.\"([/|.|a-zA-Z0-9|_|-]*)", Pattern.CASE_INSENSITIVE);

    private Pattern premiumdownload = Pattern.compile("value=\"download\" onclick=\"javascript:download", Pattern.CASE_INSENSITIVE);

    /*
     * ist leider notwendig da wir das Dateiformat nicht kennen!
     */
    private Pattern premiumdownloadlocation = Pattern.compile("form name=\"dxp\" action=\"(.*)\" method=\"post\"", Pattern.CASE_INSENSITIVE);

    public Divxvid() {
        super();
        default_password.add("dxd-tivi");
        default_password.add("dxp.divxvid.org");
        default_password.add("dxp");
        default_password.add("dxp-tivi");
        default_password.add("DivXviD");
        default_password.add("dxd.dl.am");
    }

    /*
     * Diese wichtigen Infos sollte man sich unbedingt durchlesen
     */

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        String cryptedLink = parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        URL url;
        try {
            url = new URL(cryptedLink);

            String cookie = HTTP.postRequestWithoutHtmlCode(url, null, null, null, false).getCookie();

            String hash = url.getFile();
            hash = hash.substring(1, hash.lastIndexOf("."));
            RequestInfo requestInfo = HTTP.getRequest((new URL("http://dxp.divxvid.org/script/old_loader.php")), cookie, cryptedLink, false);

            String input = requestInfo.getHtmlCode();
            String strgate = SimpleMatches.getFirstMatch(input, gate, 1);
            String outl = SimpleMatches.getFirstMatch(input, outputlocation, 1);

            requestInfo = HTTP.postRequest((new URL("http://dxp.divxvid.org/" + strgate)), null, cryptedLink, null, "hash=" + hash, false);

            /*
             * es werden dank divxvid.org hier nur die menge der links gezaehlt
             */
            int countHits = SimpleMatches.countOccurences(requestInfo.getHtmlCode(), premiumdownload);
            progress.setRange(countHits);
            for (int i = 0; i < countHits; i++) {
                requestInfo = HTTP.postRequestWithoutHtmlCode((new URL(SimpleMatches.getFirstMatch(HTTP.getRequest((new URL("http://dxp.divxvid.org" + outl + "," + i + ",1," + hash + ".rs")), cookie, cryptedLink, true).getHtmlCode(), premiumdownloadlocation, 1))), null, null, null, false);
                if (requestInfo != null) {
                    progress.increase(1);
                    decryptedLinks.add(createDownloadlink(requestInfo.getLocation()));
                }
            }
            progress.finalize();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
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
