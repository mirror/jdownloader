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

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.ext.BasicBrowserEnviroment;
import jd.http.ext.ExtBrowser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.HTMLLinkElementImpl;
import org.lobobrowser.html.domimpl.HTMLScriptElementImpl;
import org.lobobrowser.html.style.AbstractCSS2Properties;
import org.w3c.dom.html2.HTMLCollection;

//import org.lobobrowser.html.domimpl.HTMLDivElementImpl;
//import org.lobobrowser.html.domimpl.HTMLLinkElementImpl;
//import org.lobobrowser.html.style.AbstractCSS2Properties;
//import org.w3c.dom.html2.HTMLCollection;
//API:  http://support.mediafire.com/index.php?_m=knowledgebase&_a=viewarticle&kbarticleid=68

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mediafire.com" }, urls = { "http://[\\w\\.]*?mediafire\\.com/(download\\.php\\?|\\?JDOWNLOADER(?!sharekey)|file/).*?(?=http:|$|\r|\n)" }, flags = { 2 })
public class MediafireCom extends PluginForHost {

    /** start of random stringUserAgent **/
    // A alternative solution for providing random user agents.
    // last updated: 17-05-2012
    // raztoki
    private static ArrayList<String> stringAgent = new ArrayList<String>();
    static {
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Win64; x64; Trident/6.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; WOW64; Trident/6.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0; Trident/5.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0; WOW64; Trident/5.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; chromeframe/18.0.1025.162)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; chromeframe/18.0.1025.168)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; chromeframe/19.0.1084.46)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; MATP; MATP)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; MSN Optimized;ENAU)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; yie9)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0; BOIE9;ENUSMSNIP)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; chromeframe/18.0.1025.168)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; MASP)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; MATP)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; MSN Optimized;ENAU)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; NP06)");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.79 Safari/535.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.163 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.165 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.5; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.5; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.5; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.5; rv:6.0.2) Gecko/20100101 Firefox/6.0.2");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/534.52.7 (KHTML, like Gecko) Version/5.1.2 Safari/534.52.7");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/534.54.16 (KHTML, like Gecko) Version/5.1.4 Safari/534.54.16");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/534.55.3 (KHTML, like Gecko) Version/5.1.5 Safari/534.55.3");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/534.57.2 (KHTML, like Gecko) Version/5.1.7 Safari/534.57.2");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.83 Safari/535.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.163 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.165 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.163 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/534.51.22 (KHTML, like Gecko) Version/5.1.1 Safari/534.51.22");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/534.52.7 (KHTML, like Gecko) Version/5.1.2 Safari/534.52.7");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.163 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.165 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/536.8 (KHTML, like Gecko) Chrome/20.0.1108.0 Safari/536.8");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/20.0.1131.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1134.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/534.53.11 (KHTML, like Gecko) Version/5.1.3 Safari/534.53.10");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/534.54.16 (KHTML, like Gecko) Version/5.1.4 Safari/534.54.16");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/534.55.3 (KHTML, like Gecko) Version/5.1.3 Safari/534.53.10");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/534.55.3 (KHTML, like Gecko) Version/5.1.5 Safari/534.55.3");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.79 Safari/535.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.83 Safari/535.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.142 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.163 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.165 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.10 (KHTML, like Gecko) Chrome/20.0.1124.0 Safari/536.10");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.24 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.30 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.36 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.41 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.7 (KHTML, like Gecko) Chrome/20.0.1098.0 Safari/536.7");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/534.56.5 (KHTML, like Gecko) Version/5.1.6 Safari/534.56.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/534.57.2 (KHTML, like Gecko) Version/5.1.7 Safari/534.57.2");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1136.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1137.1 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7) AppleWebKit/534.48.3 (KHTML, like Gecko) Version/5.1 Safari/534.48.3");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:10.0) Gecko/20100101 Firefox/10.0");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:2.0.1) Gecko/20100101 Firefox/4.0.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:7.0.1) Gecko/20100101 Firefox/7.0.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_0) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_3; en-us) AppleWebKit/531.21.11 (KHTML, like Gecko) Version/4.0.4 Safari/531.21.10");
        stringAgent.add("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_3; en-us) AppleWebKit/533.16 (KHTML, like Gecko) Version/5.0 Safari/533.16");
        stringAgent.add("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_4; en-us) AppleWebKit/533.18.1 (KHTML, like Gecko) Version/5.0.2 Safari/533.18.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_8; en-us) AppleWebKit/533.21.1 (KHTML, like Gecko) Version/5.0.5 Safari/533.21.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; en-US; rv:1.9.2.10) Gecko/20100914 Firefox/3.6.10");
        stringAgent.add("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; en-US; rv:1.9.2.4) Gecko/20100503 Firefox/3.6.4");
        stringAgent.add("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.8; en-GB; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Macintosh; U; PPC Mac OS X 10.5; en-US; rv:1.9.2.27) Gecko/20120216 Firefox/3.6.27");
        stringAgent.add("Mozilla/5.0 (Macintosh; U; PPC Mac OS X 10.5; en-US; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.0; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.0; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.60 Safari/534.24");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/534.52.7 (KHTML, like Gecko) Version/5.1.2 Safari/534.52.7");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.215 Safari/535.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.15 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.30 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.36 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1; rv:10.0.3) Gecko/20100101 Firefox/10.0.3");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1; rv:10.0.4) Gecko/20100101 Firefox/10.0.4");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1; rv:5.0) Gecko/20100101 Firefox/5.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1; rv:7.0.1) Gecko/20100101 Firefox/7.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1; rv:8.0.1) Gecko/20100101 Firefox/8.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1; rv:8.0) Gecko/20100101 Firefox/8.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1; rv:9.0.1) Gecko/20100101 Firefox/9.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.2) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.2) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.2) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.2; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.2; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; rv:5.0) Gecko/20100101 Firefox/5.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; rv:6.0.2) Gecko/20100101 Firefox/6.0.2");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64; rv:7.0.1) Gecko/20100101 Firefox/7.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/534.30 (KHTML, like Gecko) Chrome/12.0.742.100 Safari/534.30");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.78 Safari/535.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.83 Safari/535.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.63 Safari/535.7");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.7 (KHTML, like Gecko) Comodo_Dragon/16.1.1.0 Chrome/16.0.912.63 Safari/535.7");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.10 (KHTML, like Gecko) Chrome/20.0.1123.1 Safari/536.10");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.10 (KHTML, like Gecko) Chrome/20.0.1123.4 Safari/536.10");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1130.1 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.3 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.8 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.15 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.24 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.30 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.36 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.41 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.6 (KHTML, like Gecko) Chrome/20.0.1096.1 Safari/536.6");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.8 (KHTML, like Gecko) Chrome/20.0.1105.0 Safari/536.8");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.8 (KHTML, like Gecko) Chrome/20.0.1105.2 Safari/536.8");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.9 (KHTML, like Gecko) Chrome/20.0.1115.1 Safari/536.9");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; en; rv:2.0) Gecko/20100101 Firefox/4.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; rv:10.0.1) Gecko/20100101 Firefox/10.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; rv:10.0) Gecko/20100101 Firefox/10.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; rv:13.0) Gecko/20100101 Firefox/13.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; rv:8.0.1) Gecko/20100101 Firefox/8.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; rv:8.0) Gecko/20100101 Firefox/8.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; rv:9.0.1) Gecko/20100101 Firefox/9.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:10.0.1) Gecko/20120212 Firefox/10.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:11.0) Gecko/20120313 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.71 Safari/534.24");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.78 Safari/535.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.83 Safari/535.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.11 (KHTML, like Gecko)  Iron/17.0.1000.1 Chrome/17.0.1000.1 Safari/535.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.142 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19 Comodo_Dragon/18.1.2.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.58 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.7 (KHTML, like Gecko) Iron/16.0.950.0 Chrome/16.0.950.0 Safari/535.7");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.7 (KHTML, like Gecko) RockMelt/0.16.91.381 Chrome/16.0.912.77 Safari/535.7");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.7 (KHTML, like Gecko) RockMelt/0.16.91.385 Chrome/16.0.912.77 Safari/535.7");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.7 (KHTML, like Gecko) RockMelt/0.16.91.456 Chrome/16.0.912.77 Safari/535.7");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.10 (KHTML, like Gecko) Chrome/20.0.1123.4 Safari/536.10");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.15 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.24 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.30 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.36 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.41 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.8 (KHTML, like Gecko) Chrome/20.0.1105.2 Safari/536.8");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.9 (KHTML, like Gecko) Chrome/20.0.1115.1 Safari/536.9");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64; en; rv:2.0) Gecko/20100101 Firefox/4.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:10.0.1) Gecko/20100101 Firefox/10.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:13.0) Gecko/20100101 Firefox/13.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:2.0.1) Gecko/20100101 Firefox/4.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:7.0.1) Gecko/20100101 Firefox/7.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:8.0.1) Gecko/20100101 Firefox/8.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.30 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.2.23) Gecko/20110920 Firefox/3.6.23");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.2.25) Gecko/20111212 Firefox/3.6.25 ( .NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.2.26) Gecko/20120128 Firefox/3.6.26 (.NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28 (.NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3 ( .NET CLR 3.5.30729; .NET4.0E)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/533.4 (KHTML, like Gecko) Chrome/5.0.375.99 Safari/533.4");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/535.12 (KHTML, like Gecko) Maxthon/3.0 Chrome/18.0.966.0 Safari/535.12");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/535.3 (KHTML, like Gecko) Maxthon/3.0 Safari/535.3");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.19) Gecko/2010031422 Firefox/3.0.19 (.NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.10) Gecko/20100914 Firefox/3.6.10 ( .NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.12) Gecko/20101026 Firefox/3.6.12 ( .NET4.0E)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.26) Gecko/20120128 Firefox/3.6.26");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.8.1.24pre) Gecko/20100228 K-Meleon/1.5.4");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.9.0.19) Gecko/2010031422 Firefox/3.0.19");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.9.1.18) Gecko/20110319 Firefox/3.5.18 ( .NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.0; en-GB; rv:1.9.2.10) Gecko/20100914 Firefox/3.6.10 (.NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.0; en-GB; rv:1.9.2.24) Gecko/20111103 Firefox/3.6.24 ( .NET CLR 3.5.30729; .NET4.0C)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.0; en-GB; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28 ( .NET CLR 3.5.30729; .NET4.0C)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28 ( .NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.0.19) Gecko/2010031422 Firefox/3.0.19");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.1.19) Gecko/20110420 Firefox/3.5.19 ( .NET CLR 3.5.30729; .NET4.0E)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.10) Gecko/20100914 Firefox/3.6.10");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.16) Gecko/20110319 Firefox/3.6.16");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.26) Gecko/20120128 Firefox/3.6.26");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28 ( .NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.6) Gecko/20100625 Firefox/3.6.6");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.0.19) Gecko/2010031422 Firefox/3.0.19 (.NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.25) Gecko/20111212 Firefox/3.6.25");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28 ( .NET CLR 3.5.30729; .NET4.0C)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.6) Gecko/20100625 Firefox/3.6.6");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8");
        stringAgent.add("Mozilla/5.0 (X11; Linux armv7l; rv:2.0.1) Gecko/20100101 Firefox/4.0.1 MB860/Version.4.5.91.MB860.ATT.en.US");
        stringAgent.add("Mozilla/5.0 (X11; Linux i686) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11");
        stringAgent.add("Mozilla/5.0 (X11; Linux i686) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux i686) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/11.04 Chromium/18.0.1025.151 Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux i686) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.202 Safari/535.1");
        stringAgent.add("Mozilla/5.0 (X11; Linux i686; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (X11; Linux i686; rv:9.0) Gecko/20100101 Firefox/9.0");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.142 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/11.10 Chromium/18.0.1025.142 Chrome/18.0.1025.142 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/11.10 Chromium/18.0.1025.151 Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/11.10 Chromium/18.0.1025.168 Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/12.04 Chromium/18.0.1025.151 Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/12.04 Chromium/18.0.1025.168 Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64; rv:10.0.3) Gecko/20100101 Firefox/10.0.3 Iceweasel/10.0.3");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64; rv:10.0.4) Gecko/20120424 Firefox/10.0.4");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64; rv:7.0.1) Gecko/20100101 Firefox/7.0.1");
        stringAgent.add("Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:10.0) Gecko/20100101 Firefox/10.0");
        stringAgent.add("Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:10.0.2) Gecko/20100101 Firefox/10.0.2");
        stringAgent.add("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:11.0) Gecko/20100101 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.16) Gecko/20120315 Iceweasel/3.5.16 (like Firefox/3.5.16)");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.16) Gecko/20120421 Iceweasel/3.5.16 (like Firefox/3.5.16)");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.26) Gecko/20120216 CentOS/3.6-3.el4.centos Firefox/3.6.26");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.1.16) Gecko/20120315 Iceweasel/3.5.16 (like Firefox/3.5.16)");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.1.16) Gecko/20120421 Iceweasel/3.5.16 (like Firefox/3.5.16)");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.26) Gecko/20120216 Red Hat/3.6.26-1.el6_2 Firefox/3.6.26");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; U; en-GB) Presto/2.10.229 Version/11.62");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; U; en-GB) Presto/2.7.62 Version/11.01");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; U; en) Presto/2.10.229 Version/11.62");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; WOW64; U; en) Presto/2.10.229 Version/11.62");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; WOW64; U; en) Presto/2.10.229 Version/11.64");
    }

    /**
     * Returns a random user agent String from specified array. This array contains current user agents gathered from httpd access logs. Benefits over
     * RandomUserAgent.* are: versions and respective release dates are valid.
     * 
     * @return eg. "Opera/9.80 (X11; Linux i686; U; en) Presto/2.6.30 Version/10.63"
     */
    public static String stringUserAgent() {
        final Random rand = new Random();
        final int i = rand.nextInt(stringAgent.size());
        final String out = stringAgent.get(i);
        return out;
    }

    /** end of random stringUserAgent **/

    private static final String  PRIVATEFILE = JDL.L("plugins.hoster.mediafirecom.errors.privatefile", "Private file: Only downloadable for registered users");
    private static AtomicInteger maxPrem     = new AtomicInteger(1);

    public static abstract class PasswordSolver {

        protected Browser       br;
        protected PluginForHost plg;
        protected DownloadLink  dlink;
        private final int       maxTries;
        private int             currentTry;

        public PasswordSolver(final PluginForHost plg, final Browser br, final DownloadLink downloadLink) {
            this.plg = plg;
            this.br = br;
            this.dlink = downloadLink;
            this.maxTries = 3;
            this.currentTry = 0;
        }

        abstract protected void handlePassword(String password) throws Exception;

        // do not add @Override here to keep 0.* compatibility
        public boolean hasAutoCaptcha() {
            return false;
        }

        // do not add @Override here to keep 0.* compatibility
        public boolean hasCaptcha() {
            // Usually not
            return false;
        }

        abstract protected boolean isCorrect();

        public void run() throws Exception {
            while (this.currentTry++ < this.maxTries) {
                String password = null;
                if ((password = this.dlink.getStringProperty("pass", null)) != null) {
                } else {
                    password = Plugin.getUserInput(JDL.LF("PasswordSolver.askdialog", "Downloadpassword for %s/%s", this.plg.getHost(), this.dlink.getName()), this.dlink);
                }
                if (password == null) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong")); }
                this.handlePassword(password);
                if (!this.isCorrect()) {
                    this.dlink.setProperty("pass", Property.NULL);
                    continue;
                } else {
                    this.dlink.setProperty("pass", password);
                    return;
                }

            }
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
        }
    }

    private static final String                                    agent              = stringUserAgent();

    static private final String                                    offlinelink        = "tos_aup_violation";

    /** The name of the error page used by MediaFire */
    private static final String                                    ERROR_PAGE         = "error.php";
    /**
     * The number of retries to be performed in order to determine if a file is available
     */
    private static final int                                       NUMBER_OF_RETRIES  = 3;

    /**
     * Map to cache the configuration keys
     */
    private static final HashMap<Account, HashMap<String, String>> CONFIGURATION_KEYS = new HashMap<Account, HashMap<String, String>>();

    private static int covertToPixel(final String top) {
        if (top == null) { return 0; }
        if (top.toLowerCase().trim().endsWith("px")) { return Integer.parseInt(top.substring(0, top.length() - 2)); }
        final String value = new Regex(top, "([\\-\\+]?\\s*\\d+)").getMatch(0);
        if (value == null) { return 0; }
        return Integer.parseInt(value);
    }

    private static ArrayList<HTMLElementImpl> getPath(final HTMLElementImpl impl) {
        final ArrayList<HTMLElementImpl> styles = new ArrayList<HTMLElementImpl>();

        HTMLElementImpl p = impl;
        while (p != null) {
            styles.add(0, p);
            p = p.getParent("*");
        }
        return styles;
    }

    public static boolean isVisible(final HTMLElementImpl impl) {

        final ArrayList<HTMLElementImpl> styles = MediafireCom.getPath(impl);
        int x = 0;
        int y = 0;
        for (final HTMLElementImpl p : styles) {
            final AbstractCSS2Properties style = p.getComputedStyle(null);

            if ("none".equalsIgnoreCase(style.getDisplay())) {
                //
                System.out.println("NO DISPLAY");
                return false;
            }
            if ("absolute".equalsIgnoreCase(style.getPosition())) {
                x = y = 0;
            }
            if (style.getTop() != null) {
                y += MediafireCom.covertToPixel(style.getTop());
            }
            if (style.getLeft() != null) {
                x += MediafireCom.covertToPixel(style.getLeft());

            }

        }
        if (y < 0) {
            System.out.println("y<0" + " " + x + " - " + y);
            return false;
        }
        return true;
    }

    private String fileID;

    private String dlURL;

    public MediafireCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000);
        this.enablePremium("https://www.mediafire.com/register.php");

    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("http://media", "http://www.media"));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            this.login(br, account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        if (account.getBooleanProperty("freeaccount")) {
            ai.setStatus("Registered (free) User");
            ai.setUnlimitedTraffic();
            try {
                maxPrem.set(10);
                account.setMaxSimultanDownloads(10);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
        } else {
            br.setFollowRedirects(true);
            this.br.getPage("http://www.mediafire.com/myaccount.php");
            String trafficleft = this.br.getRegex("View Statistics.*?class=\"lg-txt\">(.*?)</div").getMatch(0);
            if (trafficleft != null) {
                trafficleft = trafficleft.trim();
                if (Regex.matches(trafficleft, Pattern.compile("(tb|tbyte|terabyte|tib)", Pattern.CASE_INSENSITIVE))) {
                    String[] trafficleftArray = trafficleft.split(" ");
                    double trafficsize = Double.parseDouble(trafficleftArray[0]);
                    trafficsize *= 1024;
                    trafficleft = Double.toString(trafficsize) + " GB";
                }
                ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
            }
            ai.setStatus("Premium User");
            try {
                maxPrem.set(-1);
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.mediafire.com/terms_of_service.php";
    }

    private String getDownloadUrl(DownloadLink downloadLink) throws Exception {
        // if (Integer.parseInt(JDUtilities.getRevision().replace(".", "")) <
        // 10000) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT,
        // "Use Nightly"); }
        String fileID = getID(downloadLink);
        if (fileID != null) {
            fileID = fileID.toLowerCase(Locale.ENGLISH);
        }
        final ExtBrowser eb = new ExtBrowser();
        try {

            //
            // Set the browserenviroment. We blacklist a few urls here, because
            // we do not need css, and several other sites
            // we enable css evaluation, because we need this to find invisible
            // links
            // internal css is enough.
            eb.setBrowserEnviroment(new BasicBrowserEnviroment(new String[] { ".*?googleapis.com.+", ".*?master_.+", ".*?connect.facebook.net.+", ".*?encoder.*?", ".*?google.+", ".*facebook.+", ".*pmsrvr.com.+", ".*yahoo.com.+", ".*templates/linkto.+", ".*cdn.mediafire.com/css/.+", ".*/blank.html", ".*twitter.com.+" }, null) {

                @Override
                public boolean doLoadContent(Request request) {
                    return super.doLoadContent(request);
                }

                @Override
                public String doScriptFilter(HTMLScriptElementImpl htmlScriptElementImpl, String text) {
                    // if (text.startsWith("BUILD_VERSION='68578';")) { return
                    // super.doScriptFilter(htmlScriptElementImpl, text); }
                    // if (text.contains("google-analytics.com/ga.js")) return
                    // "";
                    // if
                    // (text.contains("top.location.href=self.location.href"))
                    // return "";
                    // if (text.contains("FBAppId='124578887583575'")) return
                    // "";
                    // if (text.contains("notloggedin_wrapper")) return "";
                    // if (text.contains("download-dark-screen")) return "";
                    // // if
                    // //
                    // (text.contains("var gV=document.getElementById('pagename')"))
                    // // return "";
                    // if (text.contains("FB.getLoginStatus")) return "";
                    // if (text.contains("$(document).ready(function()")) return
                    // "";
                    // text = text.replace("function vg()",
                    // "function DoNotCallMe()");

                    // do notexecute these scripts
                    if (text.contains("</div>")) { return ""; }
                    if (text.startsWith("$(document)")) { return ""; }
                    if (text.startsWith("setTimeout(function(){$(")) { return ""; }
                    if (text.contains("jQuery(")) { return ""; }
                    if (text.startsWith("try{DoShow(\"notloggedin_wrapper\");")) { return ""; }
                    if (text.startsWith("var gV=document.getElementById('pagename');")) { return ""; }
                    if (text.contains("var templates=LoadTemplatesFromSource();")) { return ""; }
                    if (text.startsWith("(function(p,D){")) { return ""; }

                    return super.doScriptFilter(htmlScriptElementImpl, text);
                }

                @Override
                public boolean isInternalCSSEnabled() {
                    return true;
                }

                @Override
                public void prepareContents(Request request) {

                    super.prepareContents(request);
                }

            });
            // Start Evaluation of br
            String html = br.getRequest().getHtmlCode();

            // replace global variables
            String pkr = br.getRegex("pKr='(.*?)'").getMatch(0);
            html = html.replace("'+pKr", pkr + "'");
            br.getRequest().setHtmlCode(html);
            eb.eval(this.br);
            // wait for workframe2, but max 30 seconds
            eb.waitForFrame("workframe2", 30000);
            // dummy waittime. sometimes it seems that wiat for frame is not
            // enough

            sleep(5000, downloadLink);
            eb.getHtmlText();
            // get all links now
            final HTMLCollection links = eb.getDocument().getLinks();

            for (int i = 0; i < links.getLength(); i++) {
                final HTMLLinkElementImpl l = (HTMLLinkElementImpl) links.item(i);
                // check if the link is visible in browser
                System.out.println(l.getOuterHTML());
                if (l.getInnerHTML().toLowerCase().contains("download")) {
                    System.out.println("Download start");
                    if (MediafireCom.isVisible(l)) {
                        System.out.println("visible");
                        if (new Regex(l.getAbsoluteHref(), "http://.*?/[a-z0-9]+/[a-z0-9]+/.*").matches()) {
                            // we do not know yet, why there are urls with ip
                            // only, and urls with domain
                            // if (new Regex(l.getAbsoluteHref(),
                            // "http://\\d\\.\\d\\.\\d\\.\\d/[a-z0-9]+/[a-z0-9]+/.*").matches())
                            // { throw new
                            // PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,
                            // "Server error", 2 * 60000l); }
                            System.out.println("contains mf");
                            String url = l.getAbsoluteHref();
                            String tmpurl = url.toLowerCase(Locale.ENGLISH);
                            if (fileID != null) {
                                if (tmpurl.contains(fileID + "/")) { return url; }
                            } else {
                                return url;
                            }
                        }
                    }
                }
            }
        } catch (final Exception e) {
            logger.info(eb.getHtmlText());
            e.printStackTrace();
        } finally {
            try {
                eb.getDocument().close();
            } catch (final Throwable e) {
            }
            try {
                /*
                 * this call will stop/kill all remaining js from previous extBrowser!
                 */
                Browser loboCleanup = new Browser();
                eb.eval(loboCleanup);
            } catch (final Throwable e) {
            }

        }
        if (br.containsHTML("No servers are currently available with the requested data on them.")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No servers are currently available with the requested data on them", 30 * 60 * 1000l);
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        doFree(downloadLink, null);
    }

    public void doFree(final DownloadLink downloadLink, final Account account) throws Exception {

        String url = null;
        this.br.setDebug(true);
        boolean captchaCorrect = false;
        this.br.getHeaders().put("User-Agent", MediafireCom.agent);
        for (int i = 0; i < MediafireCom.NUMBER_OF_RETRIES; i++) {
            if (url != null) {
                break;
            }
            this.requestFileInformation(downloadLink);
            if (downloadLink.getBooleanProperty("privatefile") && account == null) throw new PluginException(LinkStatus.ERROR_FATAL, PRIVATEFILE);
            try {
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(this.br);
                Form form = this.br.getFormbyProperty("name", "form_captcha");
                String id = this.br.getRegex("e\\?k=(.+?)\"").getMatch(0);
                if (id != null) {
                    rc.setId(id);
                    final InputField challenge = new InputField("recaptcha_challenge_field", null);
                    final InputField code = new InputField("recaptcha_response_field", null);
                    form.addInputField(challenge);
                    form.addInputField(code);
                    rc.setForm(form);
                    rc.load();
                    final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
                    boolean defect = false;
                    try {
                        final String c = this.getCaptchaCode(cf, downloadLink);
                        rc.setCode(c);
                        form = this.br.getFormbyProperty("name", "form_captcha");
                        id = this.br.getRegex("e\\?k=(.+?)\"").getMatch(0);
                        if (form != null && id == null) {
                            defect = true;
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        if (id != null) {
                            /* captcha wrong */
                            continue;
                        }
                    } catch (final PluginException e) {
                        if (defect) throw e;
                        /**
                         * captcha input timeout run out.. try to reconnect
                         */
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                    }
                }
            } catch (final Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                if (e instanceof PluginException) throw (PluginException) e;
            }
            captchaCorrect = true;
            if (downloadLink.getStringProperty("type", "").equalsIgnoreCase("direct")) {
                logger.info("DirectDownload");
                url = dlURL;
            } else {
                this.handlePW(downloadLink);
                url = br.getRegex("kNO = \"(http://.*?)\"").getMatch(0);
                if (url == null) {
                    Browser brc = br.cloneBrowser();
                    this.fileID = getID(downloadLink);
                    brc.getPage("http://www.mediafire.com/dynamic/dlget.php?qk=" + fileID);
                    url = brc.getRegex("dllink\":\"(http:.*?)\"").getMatch(0);
                    if (url != null) {
                        url = url.replaceAll("\\\\", "");
                    } else {
                        logger.info("Try fallback: " + brc.toString());
                    }
                    if (url == null) {
                        /* pw protected files can directly redirect to download */
                        url = br.getRedirectLocation();
                    }
                    if (url == null) {
                        url = this.getDownloadUrl(downloadLink);
                    }
                }
            }
        }
        if (url == null) {
            if (captchaCorrect) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.setFollowRedirects(true);
        this.br.setDebug(true);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url, true, 0);
        if (!this.dl.getConnection().isContentDisposition()) {
            if (dl.getConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error (404), ", 30 * 60 * 1000l);
            logger.info("Error (3)");
            logger.info(dl.getConnection() + "");
            this.br.followConnection();
            if (br.containsHTML("We apologize, but we are having difficulties processing your download request")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Please be patient while we try to repair your download request", 2 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    private String getID(DownloadLink link) {
        String fileID = new Regex(link.getDownloadURL(), "\\?([a-zA-Z0-9]+)").getMatch(0);
        if (fileID == null) {
            fileID = new Regex(link.getDownloadURL(), "file/([a-zA-Z0-9]+)").getMatch(0);
        }
        return fileID;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        this.requestFileInformation(downloadLink);
        this.login(br, account, false);
        if (account.getBooleanProperty("freeaccount")) {
            doFree(downloadLink, account);
        } else {
            String url = dlURL;
            boolean passwordprotected = false;
            boolean useAPI = false;
            if (url == null && useAPI) {
                this.fileID = getID(downloadLink);
                this.br.postPageRaw("http://www.mediafire.com/basicapi/premiumapi.php", "premium_key=" + MediafireCom.CONFIGURATION_KEYS.get(account) + "&files=" + this.fileID);
                url = this.br.getRegex("<url>(http.*?)</url>").getMatch(0);
                if ("-202".equals(this.br.getRegex("<flags>(.*?)</").getMatch(0))) {
                    br.setFollowRedirects(false);
                    br.getPage("http://www.mediafire.com/?" + fileID);
                    url = br.getRedirectLocation();
                    if (url == null || !url.contains("download")) {
                        this.handlePW(downloadLink);
                        url = this.getDownloadUrl(downloadLink);
                    }
                    if (url == null) throw new PluginException(LinkStatus.ERROR_FATAL, "Private file. No Download possible");
                }
                if ("-204".equals(this.br.getRegex("<flags>(.*?)</").getMatch(0))) {
                    passwordprotected = true;
                    new PasswordSolver(this, this.br, downloadLink) {

                        @Override
                        protected void handlePassword(final String password) throws Exception {
                            this.br.postPageRaw("http://www.mediafire.com/basicapi/premiumapi.php", "file_1=" + MediafireCom.this.fileID + "&password_1=" + password + "&premium_key=" + MediafireCom.CONFIGURATION_KEYS.get(account) + "&files=" + MediafireCom.this.fileID);

                        }

                        @Override
                        protected boolean isCorrect() {
                            return this.br.getRegex("<url>(http.*?)</url>").getMatch(0) != null;
                        }

                    }.run();

                    url = this.br.getRegex("<url>(http.*?)</url>").getMatch(0);

                }
                if ("-105".equals(this.br.getRegex("<flags>(.*?)</").getMatch(0))) {
                    logger.info("Insufficient bandwidth");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
            } else if (url == null && useAPI == false) {
                this.fileID = getID(downloadLink);
                br.setFollowRedirects(false);
                br.getPage("http://www.mediafire.com/?" + fileID);
                /* url should be downloadlink when directDownload is enabled */
                url = br.getRedirectLocation();
                if (url == null) {
                    url = br.getRegex("kNO = \"(http://.*?)\"").getMatch(0);
                }
                if (url == null) {
                    /* try the same */
                    Browser brc = br.cloneBrowser();
                    brc.getPage("http://www.mediafire.com/dynamic/dlget.php?qk=" + fileID);
                    url = brc.getRegex("dllink\":\"(http:.*?)\"").getMatch(0);
                    if (url != null) {
                        url = url.replaceAll("\\\\", "");
                    }
                    if (url == null && brc.containsHTML("Unable to access")) {
                        this.handlePremiumPassword(downloadLink, account);
                        return;
                    } else {

                    }
                }
            }
            if (url == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            this.br.setFollowRedirects(true);
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url, true, 0);
            if (!this.dl.getConnection().isContentDisposition()) {
                logger.info("Error (4)");
                logger.info(dl.getConnection() + "");
                this.br.followConnection();
                if (this.br.getRequest().getHttpConnection().getResponseCode() == 403) {
                    logger.info("Error (3)");
                } else if (this.br.getRequest().getHttpConnection().getResponseCode() == 200 && passwordprotected) {
                    // workaround for api error:
                    // try website password solving
                    this.handlePremiumPassword(downloadLink, account);
                    return;
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.dl.startDownload();
        }
    }

    private void handlePremiumPassword(final DownloadLink downloadLink, final Account account) throws Exception {
        // API currently does not work
        // http://support.mediafire.com/index.php?_m=knowledgebase&_a=viewarticle&kbarticleid=68
        this.br.getPage(downloadLink.getDownloadURL());
        String url = br.getRedirectLocation();
        if (url != null) br.getPage(url);
        this.handlePW(downloadLink);
        url = br.getRedirectLocation();
        if (url == null) url = this.getDownloadUrl(downloadLink);
        if (url == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        this.br.setFollowRedirects(true);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, url, true, 0);

        if (!this.dl.getConnection().isContentDisposition()) {
            logger.info("Error (3)");
            logger.info(dl.getConnection() + "");
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl.startDownload();
    }

    private void handlePW(final DownloadLink downloadLink) throws Exception {
        if (this.br.containsHTML("dh\\(''\\)")) {
            new PasswordSolver(this, this.br, downloadLink) {
                String curPw = null;

                @Override
                protected void handlePassword(final String password) throws Exception {
                    curPw = password;
                    final Form form = this.br.getFormbyProperty("name", "form_password");
                    form.put("downloadp", curPw);
                    this.br.submitForm(form);
                }

                @Override
                protected boolean isCorrect() {
                    Form form = this.br.getFormbyProperty("name", "form_password");
                    boolean b = this.br.containsHTML("dh\\(''\\)");
                    if (b == false) {
                        String dh = br.getRegex("dh\\('(.*?)'\\)").getMatch(0);
                        if (dh != null && dh.trim().equals(curPw)) {
                            b = true;
                        }
                    }
                    if (form == null && b == false) return true;
                    return form != null && !b;
                }

            }.run();
        }

    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 250);
    }

    public void login(final Browser br, final Account account, boolean force) throws Exception {
        boolean red = br.isFollowingRedirects();
        synchronized (CONFIGURATION_KEYS) {
            try {
                HashMap<String, String> cookies = null;
                if (force == false && (cookies = MediafireCom.CONFIGURATION_KEYS.get(account)) != null) {
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie("http://www.mediafire.com/", key, value);
                        }
                        return;
                    }
                }
                this.setBrowserExclusive();
                br.setFollowRedirects(true);
                br.getPage("http://www.mediafire.com/");
                Form form = br.getFormbyProperty("name", "form_login1");
                if (form == null) {
                    form = br.getFormBySubmitvalue("login_email");
                }
                if (form == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                form.put("login_email", Encoding.urlEncode(account.getUser()));
                form.put("login_pass", Encoding.urlEncode(account.getPass()));
                br.submitForm(form);
                br.getPage("https://www.mediafire.com/myfiles.php");
                final String cookie = br.getCookie("http://www.mediafire.com", "user");
                if ("x".equals(cookie) || cookie == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                br.setFollowRedirects(false);
                br.getPage("https://www.mediafire.com/myaccount/download_options.php");
                if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("select_account_type")) {
                    account.setProperty("freeaccount", true);
                } else {
                    account.setProperty("freeaccount", Property.NULL);
                    String di = br.getRegex("di='(.*?)'").getMatch(0);
                    br.getPage("http://www.mediafire.com/dynamic/download_options.php?enable_me_from_me=0&nocache=" + new Random().nextInt(1000) + "&di=" + di);
                    // String configurationKey = getAPIKEY(br);
                    // if (configurationKey == null) throw new
                    // PluginException(LinkStatus.ERROR_PREMIUM,
                    // PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("http://www.mediafire.com");
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                MediafireCom.CONFIGURATION_KEYS.put(account, cookies);
            } catch (final PluginException e) {
                MediafireCom.CONFIGURATION_KEYS.remove(account);
                throw e;
            } finally {
                br.setFollowRedirects(red);
            }
        }
    }

    private String getAPIKEY(Browser br) {
        if (br == null) return null;
        String configurationKey = this.br.getRegex("Configuration Key:.*? value=\"(.*?)\"").getMatch(0);
        if (configurationKey == null) configurationKey = this.br.getRegex("Configuration Key.*? value=\"(.*?)\"").getMatch(0);
        return configurationKey;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        this.setBrowserExclusive();
        this.br.setFollowRedirects(false);
        br.forceDebug(true);
        downloadLink.setProperty("type", "");
        final String url = downloadLink.getDownloadURL();
        dlURL = null;
        for (int i = 0; i < MediafireCom.NUMBER_OF_RETRIES; i++) {
            try {
                this.br.getPage(url);
                String redirectURL = this.br.getRedirectLocation();
                if (redirectURL != null && redirectURL.indexOf(MediafireCom.ERROR_PAGE) > 0) {
                    /* check for offline status */
                    final String errorCode = redirectURL.substring(redirectURL.indexOf("=") + 1, redirectURL.length());
                    if ("320".equals(errorCode)) {
                        logger.warning("The requested file ['" + url + "'] is invalid");
                    } else if ("382".equals(errorCode)) {
                        logger.warning("The requested file ['" + url + "'] belongs to suspended account");
                    } else if ("386".equals(errorCode)) {
                        logger.warning("The requested file ['" + url + "'] blocked for violation");
                    }
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }

                if (redirectURL != null && this.br.getCookie("http://www.mediafire.com", "ukey") != null) {
                    if (url.contains("download.php") || url.contains("fire.com/file/")) {
                        /* new redirect format */
                        if (new Regex(redirectURL, "http://download\\d+\\.mediafire").matches()) {
                            URLConnectionAdapter con = null;
                            try {
                                con = br.openGetConnection(redirectURL);
                                if (con.isContentDisposition()) {
                                    dlURL = redirectURL;
                                    downloadLink.setProperty("type", "direct");
                                    downloadLink.setDownloadSize(con.getLongContentLength());
                                    downloadLink.setFinalFileName(Plugin.getFileNameFromHeader(con));
                                    return AvailableStatus.TRUE;
                                } else {
                                    br.followConnection();
                                    break;
                                }
                            } finally {
                                try {
                                    con.disconnect();
                                } catch (final Throwable e) {
                                }
                            }
                        }
                    }

                    URLConnectionAdapter con = null;
                    try {
                        /* here we also can have direct link */
                        con = br.openGetConnection(redirectURL);
                        if (con.isContentDisposition()) {
                            downloadLink.setProperty("type", "direct");
                            dlURL = redirectURL;
                            downloadLink.setDownloadSize(con.getLongContentLength());
                            downloadLink.setFinalFileName(Plugin.getFileNameFromHeader(con));
                            return AvailableStatus.TRUE;
                        } else {
                            br.followConnection();
                        }
                    } finally {
                        try {
                            con.disconnect();
                        } catch (final Throwable e) {
                        }
                    }

                    redirectURL = this.br.getRedirectLocation();
                    downloadLink.setProperty("privatefile", false);
                    if (redirectURL != null && redirectURL.contains("mediafire.com/error.php?errno=999")) {
                        downloadLink.getLinkStatus().setStatusText(PRIVATEFILE);
                        final String name = new Regex(url, "download\\.php\\?(.+)").getMatch(0);
                        if (name != null) downloadLink.setName(name);
                        downloadLink.setProperty("privatefile", true);
                    } else if (redirectURL != null && redirectURL.indexOf(MediafireCom.ERROR_PAGE) > 0) {
                        /* check for offline status */
                        final String errorCode = redirectURL.substring(redirectURL.indexOf("=") + 1, redirectURL.length());
                        if ("320".equals(errorCode)) {
                            logger.warning("The requested file ['" + url + "'] is invalid");
                        } else if ("382".equals(errorCode)) {
                            logger.warning("The requested file ['" + url + "'] belongs to suspended account");
                        } else if ("386".equals(errorCode)) {
                            logger.warning("The requested file ['" + url + "'] blocked for violation");
                        }
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else {
                        String name = br.getRegex("<div class=\"download_file_title\"> (.*?) </div>").getMatch(0);
                        String size = br.getRegex(" <input type=\"hidden\" id=\"sharedtabsfileinfo1-fs\" value=\"(.*?)\">").getMatch(0);
                        if (size == null) size = br.getRegex("(?i)\\(([\\d\\.]+ (KB|MB|GB|TB))\\)").getMatch(0);
                        if (size == null) {
                            String fileID = getID(downloadLink);
                            if (fileID != null) size = br.getRegex("FileSharePopup.*?'" + fileID + "','(.*?)','(\\d+)'").getMatch(1);
                        }
                        if (name != null) {
                            downloadLink.setFinalFileName(Encoding.htmlDecode(name.trim()));
                            if (size != null) downloadLink.setDownloadSize(SizeFormatter.getSize(size));
                            return AvailableStatus.TRUE;
                        }
                        if (!downloadLink.getStringProperty("origin", "").equalsIgnoreCase("decrypter")) {
                            downloadLink.setName(Plugin.extractFileNameFromURL(redirectURL));
                        }
                        con = null;
                        try {
                            con = br.cloneBrowser().openGetConnection(redirectURL);
                            if (con.isContentDisposition()) {
                                downloadLink.setProperty("type", "direct");
                                dlURL = redirectURL;
                                downloadLink.setDownloadSize(con.getLongContentLength());
                                downloadLink.setFinalFileName(Plugin.getFileNameFromHeader(con));
                                return AvailableStatus.TRUE;
                            } else {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                        } finally {
                            try {
                                con.disconnect();
                            } catch (final Throwable e) {
                            }
                        }
                    }
                }
                break;
            } catch (final IOException e) {
                if (e.getMessage().contains("code: 500")) {
                    logger.info("ErrorCode 500! Wait a moment!");
                    Thread.sleep(200);
                    continue;
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.setProperty("type", "");
    }

    @Override
    public void resetPluginGlobals() {
    }

}