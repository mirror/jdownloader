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
import java.util.regex.Pattern;

import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class StacheldrahtTo extends PluginForDecrypt {
    final static String host = "stacheldraht.to";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?stacheldraht\\.to/index\\.php\\?folder=.+", Pattern.CASE_INSENSITIVE);

    public StacheldrahtTo(String cfgName){
        super(cfgName);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        try {
            RequestInfo ri = HTTP.getRequest(new URL(parameter));
            String cookie = ri.getCookie().split(";")[0];
            String links[][] = new Regex(ri.getHtmlCode(), "var InputVars = \"(.*?)\"", Pattern.CASE_INSENSITIVE).getMatches();

            progress.setRange(links.length / 2);
            for (int i = 0; i < links.length; i = i + 2) {
                HTTPConnection httpConnection = new HTTPConnection(new URL("http://www.stacheldraht.to/php_docs/ajax/link_get.php?" + links[i][0]).openConnection());
                httpConnection.setReadTimeout(HTTP.getReadTimeoutFromConfiguration());
                httpConnection.setConnectTimeout(HTTP.getConnectTimeoutFromConfiguration());
                httpConnection.setRequestMethod("GET");
                httpConnection.setInstanceFollowRedirects(true);
                httpConnection.setRequestProperty("Host", "stacheldraht.to");
                httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
                httpConnection.setRequestProperty("Accept", "text/javascript, text/html, application/xml, text/xml, */*");
                httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
                httpConnection.setRequestProperty("Accept-Encoding", "gzip,deflate");
                httpConnection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
                httpConnection.setRequestProperty("Keep-Alive", "300");
                httpConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
                httpConnection.setRequestProperty("X-Prototype-Version", "1.6.0.2");
                httpConnection.setRequestProperty("Referer", parameter);
                httpConnection.setRequestProperty("Cookie", cookie);
                RequestInfo reqinfo = HTTP.readFromURL(httpConnection);
                reqinfo.setConnection(httpConnection);

                progress.increase(1);
                decryptedLinks.add(createDownloadlink(reqinfo.getHtmlCode().trim()));
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
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}