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
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class ScumIn extends PluginForDecrypt {

    static private String host = "scum.in";

    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?scum\\.in/index\\.php\\?id=\\d+", Pattern.CASE_INSENSITIVE);

    public ScumIn() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        try {
            RequestInfo reqinfo = HTTP.getRequest(new URL(parameter));
            String cookie = reqinfo.getCookie();
            String id = parameter.split("=")[1];

            String urlString = URLDecoder.decode("http://scum.in/share/includes/captcha.php?t=", "UTF-8");
            URL url = new URL(urlString);
            HTTPConnection con = new HTTPConnection(url.openConnection());
            con.setReadTimeout(HTTP.getReadTimeoutFromConfiguration());
            con.setConnectTimeout(HTTP.getConnectTimeoutFromConfiguration());
            con.setInstanceFollowRedirects(false);
            con.setRequestProperty("Referer", parameter);
            con.setRequestProperty("Cookie", cookie);
            con.setRequestProperty("Accept", "image/png,*/*;q=0.5");
            con.setRequestProperty("Accept-Encoding", "gzip,deflate");
            con.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
            con.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
            con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");

            File captchaFile = this.getLocalCaptchaFile(this);
            if (!Browser.download(captchaFile, con) || !captchaFile.exists()) { return null; }
            String captchaCode = Plugin.getCaptchaCode(captchaFile, this);

            reqinfo = HTTP.postRequest(new URL("http://scum.in/plugins/home/links.callback.php"), cookie, parameter, null, "id=" + id + "&captcha=" + captchaCode, false);

            String links[][] = new Regex(reqinfo.getHtmlCode(), "href=\"(.*?)\"", Pattern.CASE_INSENSITIVE).getMatches();
            progress.setRange(links.length);
            for (String[] element : links) {
                progress.increase(1);
                DownloadLink dl_link = createDownloadlink(element[0]);
                dl_link.addSourcePluginPassword("scum.in");
                decryptedLinks.add(dl_link);
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