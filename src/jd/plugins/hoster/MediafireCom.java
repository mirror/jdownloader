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
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
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
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.LnkCrptWs;
import jd.plugins.decrypter.MdfrFldr;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mediafire.com" }, urls = { "http://(www\\.)?mediafire\\.com/(download\\.php\\?|\\?JDOWNLOADER(?!sharekey)|file/).*?(?=http:|$|\r|\n)" }, flags = { 2 })
public class MediafireCom extends PluginForHost {

    /** start of random agents **/
    // A alternative solution for providing random user agents.
    // last updated: 11-07-2012
    // raztoki
    private static final ArrayList<String> stringAgent = new ArrayList<String>();
    static {
        // Internet Explorer
        // release:
        // ie9: "Stable release    9.0.7 / April 11, 2012; 2 months ago"
        // http://en.wikipedia.org/wiki/Internet_Explorer_9
        // ie10: "Preview release 10.0.8400.0 / May 31, 2012; 40 days ago"
        // http://en.wikipedia.org/wiki/Internet_Explorer_10
        // notes: only version 9 and 10
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Win64; x64; Trident/6.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; WOW64; Trident/6.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0; Trident/5.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0; WOW64; Trident/5.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; chromeframe/18.0.1025.162)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; chromeframe/18.0.1025.168)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; chromeframe/19.0.1084.46)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; chromeframe/19.0.1084.52)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; chromeframe/19.0.1084.56)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; chromeframe/20.0.1132.47)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; MANM)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; MATP; MATP)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; MSN Optimized;ENAU)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; xs-mpFUgPAPNBo;CiAVK0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; xs-mpFUgPAPNBo;CiAVLw)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; xs-mpFUgPAPNBo;CiAVM8)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; xs-mpFUgPAPNBo;CiAVMe)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; xs-mpFUgPAPNBo;CiAVMP)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; yie9)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0; BOIE9;ENUSMSNIP)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; BOIE9;ENAU)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; BOIE9;ENUS)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; chromeframe/18.0.1025.168)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; chromeframe/19.0.1084.46)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; chromeframe/19.0.1084.52)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; chromeframe/19.0.1084.56)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; chromeframe/20.0.1132.47)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; MASP)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; MATP)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; MSN Optimized;ENAU)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; NP06)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; NP08; MAAU; NP08)");
        // chrome
        // release: "Stable release  20.0.1132.47  (June 28, 2012; 12 days ago)"
        // http://en.wikipedia.org/wiki/Google_Chrome
        // notes: only allow the last 18-2x releases
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.163 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.165 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.53 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.54 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_5_8) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.163 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.165 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.43 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.53 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.54 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_0) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.163 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.163 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.165 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/536.8 (KHTML, like Gecko) Chrome/20.0.1108.0 Safari/536.8");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/20.0.1131.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1134.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.142 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.163 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.165 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.10 (KHTML, like Gecko) Chrome/20.0.1124.0 Safari/536.10");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.27 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.24 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.30 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.36 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.41 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.53 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.54 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.7 (KHTML, like Gecko) Chrome/20.0.1098.0 Safari/536.7");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.17 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.21 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.27 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.34 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.39 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.43 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.53 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.54 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1136.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1137.1 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1141.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1147.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1150.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1154.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1162.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1175.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/22.0.1186.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_0) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_0) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.17 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.21 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.27 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.34 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.39 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.15 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.30 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.36 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.2) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.2) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.2) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.2) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.2) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.2) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.2) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.10 (KHTML, like Gecko) Chrome/20.0.1123.1 Safari/536.10");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.10 (KHTML, like Gecko) Chrome/20.0.1123.4 Safari/536.10");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1130.1 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.11 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.17 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.21 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.27 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.3 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.34 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.39 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.41 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.42 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.43 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.8 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.15 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.24 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.30 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.36 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.41 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.6 (KHTML, like Gecko) Chrome/20.0.1096.1 Safari/536.6");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.8 (KHTML, like Gecko) Chrome/20.0.1105.0 Safari/536.8");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.8 (KHTML, like Gecko) Chrome/20.0.1105.2 Safari/536.8");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/536.9 (KHTML, like Gecko) Chrome/20.0.1115.1 Safari/536.9");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1145.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1155.2 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.142 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19 Comodo_Dragon/18.1.2.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.58 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.10 (KHTML, like Gecko) Chrome/20.0.1123.4 Safari/536.10");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.11 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.17 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.21 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.27 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.34 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.39 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.41 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.42 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.43 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11 Comodo_Dragon/20.0.1.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.15 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.24 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.30 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.36 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.41 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5 Comodo_Dragon/19.2.0.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.8 (KHTML, like Gecko) Chrome/20.0.1105.2 Safari/536.8");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.9 (KHTML, like Gecko) Chrome/20.0.1115.1 Safari/536.9");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1145.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1155.2 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1163.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1171.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.15 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.4 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.11 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.17 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.21 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.30 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1171.0 Safari/537.1");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/532.0 (KHTML, like Gecko) Chrome/3.0.195.27 Safari/532.0");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/533.4 (KHTML, like Gecko) Chrome/5.0.375.99 Safari/533.4");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/535.12 (KHTML, like Gecko) Maxthon/3.0 Chrome/18.0.966.0 Safari/535.12");
        stringAgent.add("Mozilla/5.0 (X11; Linux i686) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux i686) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/11.04 Chromium/18.0.1025.151 Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.142 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/11.10 Chromium/18.0.1025.142 Chrome/18.0.1025.142 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/11.10 Chromium/18.0.1025.151 Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/11.10 Chromium/18.0.1025.168 Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/12.04 Chromium/18.0.1025.151 Chrome/18.0.1025.151 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/12.04 Chromium/18.0.1025.168 Chrome/18.0.1025.168 Safari/535.19");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.47 Safari/536.11");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        // firefox
        // stable: "Stable release  13.0.1  (June 15, 2012; 25 days ago)"
        // http://en.wikipedia.org/wiki/Firefox
        // notes: 2011-present based on Gecko date
        stringAgent.add("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.8; en-GB; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Macintosh; U; PPC Mac OS X 10.5; en-US; rv:1.9.2.27) Gecko/20120216 Firefox/3.6.27");
        stringAgent.add("Mozilla/5.0 (Macintosh; U; PPC Mac OS X 10.5; en-US; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; rv:14.0) Gecko/20120512 Firefox/14.0a2");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; rv:15.0) Gecko/20120616 Firefox/15.0a2");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; rv:15.0) Gecko/20120627 Firefox/15.0a2");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:10.0.1) Gecko/20120212 Firefox/10.0.1");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:11.0) Gecko/20120313 Firefox/11.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:12.0) Gecko/20120427 Firefox/12.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:14.0) Gecko/20120523 Firefox/14.0a2");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:15.0) Gecko/20120624 Firefox/15.0a2");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.2.23) Gecko/20110920 Firefox/3.6.23");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.2.25) Gecko/20111212 Firefox/3.6.25 ( .NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.2.26) Gecko/20120128 Firefox/3.6.26 (.NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28 ( .NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28 (.NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.26) Gecko/20120128 Firefox/3.6.26");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.28) Gecko/20120306 AskTbFF/3.15.2.23037 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.9.1.18) Gecko/20110319 Firefox/3.5.18 ( .NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.0; en-GB; rv:1.9.2.24) Gecko/20111103 Firefox/3.6.24 ( .NET CLR 3.5.30729; .NET4.0C)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.0; en-GB; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28 ( .NET CLR 3.5.30729; .NET4.0C)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28 ( .NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.1.19) Gecko/20110420 Firefox/3.5.19 ( .NET CLR 3.5.30729; .NET4.0E)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.16) Gecko/20110319 Firefox/3.6.16");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.18) Gecko/20110614 Firefox/3.6.18 ( .NET CLR 3.5.30729; .NET4.0E)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.26) Gecko/20120128 Firefox/3.6.26");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28 ( .NET CLR 3.5.30729)");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.25) Gecko/20111212 Firefox/3.6.25");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.26) Gecko/20120128 Firefox/3.6.26");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28");
        stringAgent.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.28) Gecko/20120306 Firefox/3.6.28 ( .NET CLR 3.5.30729; .NET4.0C)");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64; rv:10.0.4) Gecko/20120424 Firefox/10.0.4");
        stringAgent.add("Mozilla/5.0 (X11; Linux x86_64; rv:10.0.5) Gecko/20120605 Firefox/10.0.5");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.16) Gecko/20120315 Iceweasel/3.5.16 (like Firefox/3.5.16)");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.16) Gecko/20120421 Iceweasel/3.5.16 (like Firefox/3.5.16)");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.16) Gecko/20120511 Iceweasel/3.5.16 (like Firefox/3.5.16)");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.16) Gecko/20120602 Iceweasel/3.5.16 (like Firefox/3.5.16)");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.24) Gecko/20111108 Fedora/3.6.24-1.fc14 Firefox/3.6.24");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.26) Gecko/20120216 CentOS/3.6-3.el4.centos Firefox/3.6.26");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.1.16) Gecko/20120315 Iceweasel/3.5.16 (like Firefox/3.5.16)");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.1.16) Gecko/20120421 Iceweasel/3.5.16 (like Firefox/3.5.16)");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.1.16) Gecko/20120511 Iceweasel/3.5.16 (like Firefox/3.5.16)");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.1.16) Gecko/20120602 Iceweasel/3.5.16 (like Firefox/3.5.16)");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.17) Gecko/20110422 Ubuntu/8.04 (hardy) Firefox/3.6.17");
        stringAgent.add("Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.26) Gecko/20120216 Red Hat/3.6.26-1.el6_2 Firefox/3.6.26");
        // safari
        // release: "Stable release  5.1.7  (May 9, 2012; 61 days ago)"
        // http://en.wikipedia.org/wiki/Safari_(web_browser)
        // notes: not sure on safari...

        // opera
        // release: "Stable release  12.00  (June 14, 2012; 25 days ago)"
        // http://en.wikipedia.org/wiki/Opera_(web_browser)
        // notes: 11.64-12.x (present)
        stringAgent.add("Opera/9.80 (Windows NT 5.1; U; en) Presto/2.10.229 Version/11.64");
        stringAgent.add("Opera/9.80 (Windows NT 5.1; U; en) Presto/2.10.289 Version/12.00");
        stringAgent.add("Opera/9.80 (Windows NT 6.0; U; en) Presto/2.10.229 Version/11.64");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; U; en-GB) Presto/2.10.229 Version/11.64");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; U; en-GB) Presto/2.10.289 Version/12.00");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; U; IBM EVV/3.0/EAK01AG9/LE; Edition Next; en) Presto/2.11.310 Version/12.50");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; WOW64; U; Edition Next; en) Presto/2.11.310 Version/12.50");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; WOW64; U; en) Presto/2.10.229 Version/11.64");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; WOW64; U; en) Presto/2.10.289 Version/12.00");
        stringAgent.add("Opera/9.80 (X11; Linux i686; U; en) Presto/2.10.289 Version/12.00");
        stringAgent.add("Opera/9.80 (X11; Linux zbov; U; en) Presto/2.10.254 Version/12.00");
    }

    /**
     * Returns a random User-Agent String (common browsers) of specified array. This array contains current user agents gathered from httpd
     * access logs. Benefits over RandomUserAgent.* are: versions and respective release dates are valid.
     * 
     * @return eg. "Opera/9.80 (X11; Linux i686; U; en) Presto/2.6.30 Version/10.63"
     */
    public static String stringUserAgent() {
        final Random rand = new Random();
        final int i = rand.nextInt(stringAgent.size());
        final String out = stringAgent.get(i);
        return out;
    }

    /* End of standard agents */

    /* Agents from portable devices */

    private static final ArrayList<String> portableAgent = new ArrayList<String>();
    static {
        portableAgent.add("Mozilla/5.0 (Android; Linux armv7l; rv:10.0.1) Gecko/20120208 Firefox/10.0.1 Fennec/10.0.1");
        portableAgent.add("Mozilla/5.0 (Android; Tablet; rv:10.0.4) Gecko/10.0.4 Firefox/10.0.4 Fennec/10.0.4");
        portableAgent.add("Mozilla/5.0 (hp-tablet; Linux; hpwOS/3.0.5; U; en-AU) AppleWebKit/534.6 (KHTML, like Gecko) wOSBrowser/234.83 Safari/534.6 TouchPad/1.0");
        portableAgent.add("Mozilla/5.0 (iPad; CPU OS 5_0_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A405 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (iPad; CPU OS 5_0_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9Z999 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (iPad; CPU OS 5_1_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B206 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (iPad; CPU OS 5_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Mobile/9B176");
        portableAgent.add("Mozilla/5.0 (iPad; CPU OS 5_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B176 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (iPad; U; CPU OS 4_3_5 like Mac OS X; en) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8L1 Safari/6533.18.5");
        portableAgent.add("Mozilla/5.0 (iPad; U; CPU OS 4_3_5 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8L1 Safari/6533.18.5");
        portableAgent.add("Mozilla/5.0 (iPad; U; CPU OS 5_0 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (iPad; U; CPU OS 5_1_1 like Mac OS X; en-gb) AppleWebKit/534.46.0 (KHTML, like Gecko) CriOS/19.0.1084.60 Mobile/9B206 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (iPad; U; CPU OS 5_1_1 like Mac OS X; en-us) AppleWebKit/534.46.0 (KHTML, like Gecko) CriOS/19.0.1084.60 Mobile/9B206 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (iPad; U; CPU OS 5_1 like Mac OS X; en-us) AppleWebKit/534.46.0 (KHTML, like Gecko) CriOS/19.0.1084.60 Mobile/9B176 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 5_0_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A405 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 5_0 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 5_1_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B206 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 5_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B176 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 5_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B179 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_3_3 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8J2 Safari/6533.18.5");
        portableAgent.add("Mozilla/5.0 (iPhone; U; CPU iPhone OS 5_1_1 like Mac OS X; en-us) AppleWebKit/534.46.0 (KHTML, like Gecko) CriOS/19.0.1084.60 Mobile/9B206 Safari/7534.48.3");
        portableAgent.add("Mozilla/5.0 (Linux; Android 4.0.1;  Galaxy Nexus Build/ITL41F) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133 Mobile Safari/535.19");
        portableAgent.add("Mozilla/5.0 (Linux; Android 4.0.3; ASUS Transformer Pad TF300T Build/IML74K) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166  Safari/535.19");
        portableAgent.add("Mozilla/5.0 (Linux; Android 4.0.3;  Transformer Prime TF201 Build/IML74K) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.133  Safari/535.19");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 2.2; en-au; HTC_Desire_A8183 V2.26.841.2 Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 2.3.3; en-au; GT-P1000 Build/GINGERBREAD) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 2.3.4; en-au; HTC_SensationXE_Beats_Z715a Build/GRJ22) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 2.3.5; en-au; GT-I9100T Build/GINGERBREAD) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 2.3.6; en-au; GT-I9210T Build/GINGERBREAD) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 2.3.6; en-au; GT-N7000 Build/GINGERBREAD) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 2.3.6; en-au; GT-P1000 Build/GINGERBREAD) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 3.2.2; en-au; XOOM 2 Build/1.6.0_268.4-MZ616) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Safari/533.1");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 3.2; en-au; GT-P6810 Build/HTJ85B) AppleWebKit/534.13 (KHTML, like Gecko) Version/4.0 Safari/534.13");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 4.0.3; en-au; GT-I9100 Build/IML74K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 4.0.3; en-au; GT-I9100T Build/IML74K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 4.0.3; en-au; HTC EVA_UL Build/IML74K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 4.0.3; en-au; HTC_SensationXE_Beats_Z715a Build/IML74K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 4.0.3; en-us; cm_tenderloin Build/GWK74) AppleWebKit/535.7 (KHTML, like Gecko) CrMo/16.0.912.77  Safari/535.7");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 4.0.3; en-us; Transformer Prime TF201 Build/IML74K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Safari/534.30");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 4.0.4; en-au; GT-I9300T Build/IMM76D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
        portableAgent.add("Mozilla/5.0 (Linux; U; Android 4.0.4; en-us; GT-I9100 Build/IMM76D; CyanogenMod-9) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
        portableAgent.add("Opera/9.80 (Android 2.3.3; Linux; Opera Mobi/ADR-1203051631; U; en) Presto/2.10.254 Version/12.00");
        portableAgent.add("Opera/9.80 (Android 2.3.3; Linux; Opera Mobi/ADR-1204201824; U; en) Presto/2.10.254 Version/12.00");
        portableAgent.add("Opera/9.80 (Android 2.3.3; Linux; Opera Mobi/ADR-1205181138; U; en) Presto/2.10.254 Version/12.00");
        portableAgent.add("Opera/9.80 (Android 2.3.3; Linux; Opera Tablet/ADR-1110171336; U; en) Presto/2.9.201 Version/11.50");
        portableAgent.add("Opera/9.80 (Android 4.0.3; Linux; Opera Mobi/ADR-1111101157; U; en) Presto/2.9.201 Version/11.50");
        portableAgent.add("Opera/9.80 (Android 4.0.3; Linux; Opera Mobi/ADR-1205181138; U; en) Presto/2.10.254 Version/12.00");
    }

    /**
     * Returns a random User-Agent String (from a portable device) of specified array. This array contains current user agents gathered from
     * httpd access logs. Benefits over RandomUserAgent.* are: versions and respective release dates are valid.
     * 
     * @return eg. "Opera/9.80 (Android 4.0.3; Linux; Opera Mobi/ADR-1205181138; U; en) Presto/2.10.254 Version/12.00"
     */
    public static String portableUserAgent() {
        final Random rand = new Random();
        final int i = rand.nextInt(portableAgent.size());
        final String out = portableAgent.get(i);
        return out;
    }

    /** end of random agents **/

    private static final String                              PRIVATEFILE           = JDL.L("plugins.hoster.mediafirecom.errors.privatefile", "Private file: Only downloadable for registered users");
    private static AtomicInteger                             maxPrem               = new AtomicInteger(1);
    private static final String                              PRIVATEFOLDERUSERTEXT = "This is a private folder. Re-Add this link while your account is active to make it work!";
    private String                                           SESSIONTOKEN          = null;
    private String                                           ERRORCODE             = null;
    /**
     * Map to cache the configuration keys
     */
    private static HashMap<Account, HashMap<String, String>> CONFIGURATION_KEYS    = new HashMap<Account, HashMap<String, String>>();

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

    private static String       agent             = stringUserAgent();

    static private final String offlinelink       = "tos_aup_violation";

    /** The name of the error page used by MediaFire */
    private static final String ERROR_PAGE        = "error.php";
    /**
     * The number of retries to be performed in order to determine if a file is available
     */
    private int                 NUMBER_OF_RETRIES = 3;

    private String              fileID;

    private String              dlURL;

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
        requestFileInformation(downloadLink);
        if (downloadLink.getBooleanProperty("privatefolder")) throw new PluginException(LinkStatus.ERROR_FATAL, PRIVATEFOLDERUSERTEXT);
        doFree(downloadLink, null);
    }

    public void doFree(final DownloadLink downloadLink, final Account account) throws Exception {
        String url = null;
        boolean captchaCorrect = false;
        if (account == null) this.br.getHeaders().put("User-Agent", MediafireCom.agent);
        for (int i = 0; i < NUMBER_OF_RETRIES; i++) {
            if (url != null) {
                break;
            }
            this.requestFileInformation(downloadLink);
            if (downloadLink.getBooleanProperty("privatefile") && account == null) throw new PluginException(LinkStatus.ERROR_FATAL, PRIVATEFILE);
            // Check for direct link
            try {
                br.setFollowRedirects(true);
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(downloadLink.getDownloadURL());
                    if (!con.getContentType().contains("html")) {
                        url = downloadLink.getDownloadURL();
                    } else {
                        br.followConnection();
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
                if (url == null) {
                    // TODO: This errorhandling is missing for premium users!
                    handleNonAPIErrors(downloadLink);
                    captchaCorrect = false;
                    Form form = br.getFormbyProperty("name", "form_captcha");
                    if (br.getRegex("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)").matches()) {
                        logger.info("Detected captcha method \"Re Captcha\" for this host");
                        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(this.br);
                        String id = br.getRegex("challenge\\?k=(.+?)\"").getMatch(0);
                        if (id != null) {
                            logger.info("CaptchaID found, Form found " + (form != null));
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
                                form = br.getFormbyProperty("name", "form_captcha");
                                id = br.getRegex("challenge\\?k=(.+?)\"").getMatch(0);
                                if (form != null && id == null) {
                                    logger.info("Form found but no ID");
                                    defect = true;
                                    logger.info("PluginError 672");
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
                    } else if (br.containsHTML("solvemedia\\.com/papi/")) {
                        logger.info("Detected captcha method \"solvemedia\" for this host");
                        final PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                        final jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((LnkCrptWs) solveplug).getSolveMedia(br);
                        final File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                        String code = getCaptchaCode(cf, downloadLink);
                        String chid = sm.getChallenge(code);
                        form.put("adcopy_challenge", chid);
                        form.put("adcopy_response", code.replace(" ", "+"));
                        br.submitForm(form);
                        if (br.getFormbyProperty("name", "form_captcha") != null) continue;
                    }
                }
            } catch (final Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                if (e instanceof PluginException) throw (PluginException) e;
            }
            captchaCorrect = true;
            if (url == null) {
                logger.info("Handle possible PW");
                this.handlePW(downloadLink);
                url = br.getRegex("kNO = \"(http://.*?)\"").getMatch(0);
                logger.info("Kno= " + url);
                if (url == null) {
                    final Browser brc = br.cloneBrowser();
                    this.fileID = getID(downloadLink);
                    URLConnectionAdapter con = null;
                    try {
                        logger.info("try dlget");
                        con = brc.openGetConnection("http://www.mediafire.com/dynamic/dlget.php?qk=" + fileID);
                        if (con.getResponseCode() != 404) {
                            brc.followConnection();
                        } else {
                            logger.info("Dynamic is 404");
                            continue;
                        }
                    } finally {
                        try {
                            con.disconnect();
                        } catch (final Throwable e) {
                        }
                    }
                    url = brc.getRegex("dllink\":\"(http:.*?)\"").getMatch(0);
                    if (url != null) {
                        logger.info("dllink= " + url);
                        url = url.replaceAll("\\\\", "");
                    } else {
                        logger.info("dllink failed " + brc.toString());
                        logger.info("Try fallback:");
                        try {
                            logger.info(brc.toString());
                        } catch (final Throwable e) {
                            logger.info(e.getMessage());
                        }
                    }
                    if (url == null) {
                        /* pw protected files can directly redirect to download */
                        url = br.getRedirectLocation();
                    }
                }
            }
        }
        if (url == null) {
            if (captchaCorrect) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            logger.info("PluginError 721");
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
        if (downloadLink.getBooleanProperty("privatefolder")) throw new PluginException(LinkStatus.ERROR_FATAL, PRIVATEFOLDERUSERTEXT);
        this.login(br, account, false);
        if (account.getBooleanProperty("freeaccount")) {
            doFree(downloadLink, account);
        } else {
            // TODO: See if there is a way to implement the premium API again: http://developers.mediafire.com/index.php/REST_API
            /** Problem: This API doesn't (yet) work with password protected links... */
            // getSessionToken(this.br, account);
            // apiRequest(this.br, "http://www.mediafire.com/api/file/get_links.php", "?link_type=direct_download&session_token=" +
            // this.SESSIONTOKEN + "&quick_key=" + getFID(downloadLink) + "&response_format=json");
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
                        url = br.getRedirectLocation();
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
                if ((br.containsHTML("Enter Password") && br.containsHTML("display:block;\">This file is"))) {
                    this.handlePremiumPassword(downloadLink, account);
                    return;
                }
                /* url should be downloadlink when directDownload is enabled */
                url = getURL(br);
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
        this.br.getPage(downloadLink.getDownloadURL());
        String url = br.getRedirectLocation();
        if (url != null) br.getPage(url);
        this.handlePW(downloadLink);
        url = getURL(br);
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

    private String getURL(Browser br) throws IOException {
        String url = br.getRedirectLocation();
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
        }
        return url;
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
                if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                form.put("login_email", Encoding.urlEncode(account.getUser()));
                form.put("login_pass", Encoding.urlEncode(account.getPass()));
                br.submitForm(form);
                br.getPage("https://www.mediafire.com/myfiles.php");
                final String cookie = br.getCookie("http://www.mediafire.com", "user");
                if ("x".equals(cookie) || cookie == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                br.setFollowRedirects(false);
                br.getPage("https://www.mediafire.com/myaccount/download_options.php");
                if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("upgrade?utm_source=myaccount")) {
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

    // private String getAPIKEY(Browser br) {
    // if (br == null) return null;
    // String configurationKey = this.br.getRegex("Configuration Key:.*? value=\"(.*?)\"").getMatch(0);
    // if (configurationKey == null) configurationKey = this.br.getRegex("Configuration Key.*? value=\"(.*?)\"").getMatch(0);
    // return configurationKey;
    // }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        this.br.setFollowRedirects(false);
        br.setCustomCharset("utf-8");
        downloadLink.setProperty("type", Property.NULL);
        if (downloadLink.getBooleanProperty("offline")) return AvailableStatus.FALSE;
        final String fid = getFID(downloadLink);
        if (downloadLink.getBooleanProperty("privatefolder")) {
            downloadLink.getLinkStatus().setStatusText(PRIVATEFOLDERUSERTEXT);
            downloadLink.setName(fid);
            return AvailableStatus.TRUE;
        }
        final Browser apiBR = br.cloneBrowser();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            getSessionToken(apiBR, aa);
        }
        apiRequest(apiBR, "http://www.mediafire.com/api/file/get_info.php", "?quick_key=" + fid);
        if ("114".equals(ERRORCODE)) {
            downloadLink.setProperty("privatefile", true);
            return AvailableStatus.TRUE;
        }
        downloadLink.setDownloadSize(SizeFormatter.getSize(getXML("size", apiBR.toString() + "b")));
        downloadLink.setName(Encoding.htmlDecode(getXML("filename", apiBR.toString())));
        downloadLink.setAvailable(true);
        return AvailableStatus.TRUE;
    }

    private String getXML(final String parameter, final String source) {
        return new Regex(source, "<" + parameter + ">([^<>\"]*?)</" + parameter + ">").getMatch(0);
    }

    private void apiRequest(final Browser br, final String url, final String data) throws IOException {
        if (SESSIONTOKEN == null)
            br.getPage(url + data);
        else
            br.getPage(url + data + "&session_token=" + SESSIONTOKEN);
        ERRORCODE = getXML("error", br.toString());
    }

    private void getSessionToken(final Browser apiBR, final Account aa) throws IOException {
        // Try to re-use session token as long as possible (it's valid for 10 minutes)
        final String savedusername = this.getPluginConfig().getStringProperty("username");
        final String savedpassword = this.getPluginConfig().getStringProperty("password");
        final String sessiontokenCreateDateObject = this.getPluginConfig().getStringProperty("sessiontokencreated2");
        long sessiontokenCreateDate = -1;
        if (sessiontokenCreateDateObject != null && sessiontokenCreateDateObject.length() > 0) {
            sessiontokenCreateDate = Long.parseLong(sessiontokenCreateDateObject);
        }
        if ((savedusername != null && savedusername.matches(aa.getUser())) && (savedpassword != null && savedpassword.matches(aa.getPass())) && System.currentTimeMillis() - sessiontokenCreateDate < 600000) {
            SESSIONTOKEN = this.getPluginConfig().getStringProperty("sessiontoken");
        } else {
            // Get token for user account
            apiRequest(apiBR, "https://www.mediafire.com/api/user/get_session_token.php", "?email=" + Encoding.urlEncode(aa.getUser()) + "&password=" + Encoding.urlEncode(aa.getPass()) + "&application_id=" + MdfrFldr.APPLICATIONID + "&signature=" + JDHash.getSHA1(aa.getUser() + aa.getPass() + MdfrFldr.APPLICATIONID + Encoding.Base64Decode(MdfrFldr.APIKEY)) + "&version=1");
            SESSIONTOKEN = getXML("session_token", apiBR.toString());
            this.getPluginConfig().setProperty("username", aa.getUser());
            this.getPluginConfig().setProperty("password", aa.getPass());
            this.getPluginConfig().setProperty("sessiontoken", SESSIONTOKEN);
            this.getPluginConfig().setProperty("sessiontokencreated2", "" + System.currentTimeMillis());
            this.getPluginConfig().save();
        }
    }

    private String getFID(final DownloadLink downloadLink) {
        return new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
    }

    private void handleNonAPIErrors(final DownloadLink dl) throws PluginException {
        if (br.getURL().contains("mediafire.com/error.php?errno=382")) {
            dl.getLinkStatus().setStatusText("File Belongs to Suspended Account.");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("class=\"error\\-title\">Temporarily Unavailable</p>")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file is temporarily unavailable!", 30 * 60 * 1000l);
        if (br.containsHTML("class=\"error_msg_title\">Permission Denied") || br.getURL().contains("mediafire.com/error.php?errno=388")) throw new PluginException(LinkStatus.ERROR_FATAL, "Download not possible, host says 'Permission Denied', pls contact the mediafire.com support!");
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