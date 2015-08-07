package jd.plugins.components;

import java.util.ArrayList;
import java.util.Random;

public class UserAgents {

    private static final ArrayList<String> stringAgent   = new ArrayList<String>();
    static {
        // Internet Explorer
        // release:
        // ie9: "Stable release     9.0.38 (v9.0.8112.16421) (12 May 2015; 54 days ago)" http://en.wikipedia.org/wiki/Internet_Explorer_9
        // ie10: "Stable release    10.0.23 (v10.0.9200.17183) (9 December 2014; 6 months ago)"
        // http://en.wikipedia.org/wiki/Internet_Explorer_10
        // ie11: "Stable release 11.0.13 11.0.9600.17843 (update versions: 11.0.20) / 16 June 2015; 20 days ago //
        // http://en.wikipedia.org/wiki/Internet_Explorer_11
        // notes: only version 9 and 10
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0; Trident/5.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/7.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");

        stringAgent.add("Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; WOW64; Trident/6.0)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; WOW64; Trident/6.0; BOIE9;ENUS)");
        stringAgent.add("Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; WOW64; Trident/6.0)"); // 6

        // chrome
        // release: "Stable release 44.0.2403.107 (July 24, 2015; 4 days ago) http://en.wikipedia.org/wiki/Google_Chrome
        // notes: google changes version like it's going out of fashion! try and give balance in array. (39+)
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.18 Safari/537.36");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.30 Safari/537.36");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.39 Safari/537.36");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.52 Safari/537.36");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.61 Safari/537.36");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.18 Safari/537.36");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.30 Safari/537.36");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.39 Safari/537.36");
        stringAgent.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.89 Safari/537.36");
        stringAgent.add("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.107 Safari/537.36");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2431.0 Safari/537.36"); // 11

        // firefox
        // release: "Stable release 39.0 (July 2, 2015; 26 days ago)" http://en.wikipedia.org/wiki/Firefox
        // notes: version 34+
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:39.0) Gecko/20100101 Firefox/39.0");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:39.0) Gecko/20100101 Firefox/39.0");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:39.0) Gecko/20100101 Firefox/39.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 5.1; rv:39.0) Gecko/20100101 Firefox/39.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; rv:39.0) Gecko/20100101 Firefox/39.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.0; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; rv:39.0) Gecko/20100101 Firefox/39.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.3; Win64; x64; rv:39.0) Gecko/20100101 Firefox/39.0 Waterfox/39.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.3; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 10.0; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0");
        stringAgent.add("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:39.0) Gecko/20100101 Firefox/39.0");

        stringAgent.add("Mozilla/5.0 (Windows NT 6.3; Win64; x64; rv:40.0) Gecko/20100101 Firefox/40.0");
        stringAgent.add("Mozilla/5.0 (Windows NT 6.3; Win64; x64; rv:41.0) Gecko/20100101 Firefox/41.0"); // 14

        // safari
        // "Stable release http://en.wikipedia.org/wiki/Safari_(web_browser)
        // OS X Mavericks 7.1.7 (June 30, 2015; 28 days ago)
        // OS X Yosemite 8.0.7 (June 30, 2015; 28 days ago)
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_3) AppleWebKit/600.5.17 (KHTML, like Gecko) Version/8.0.5 Safari/600.5.17");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_3) AppleWebKit/600.6.3 (KHTML, like Gecko) Version/8.0.6 Safari/600.6.3");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_4) AppleWebKit/600.7.12 (KHTML, like Gecko) Version/8.0.7 Safari/600.7.12");

        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/600.5.17 (KHTML, like Gecko) Version/7.1.5 Safari/537.85.14");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/600.6.3 (KHTML, like Gecko) Version/7.1.6 Safari/537.85.15");
        stringAgent.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/600.7.12 (KHTML, like Gecko) Version/7.1.7 Safari/537.85.16"); // 6

        // opera
        // release: Presto "Stable release      12.17 (April 23, 2014;" http://en.wikipedia.org/wiki/Opera_(web_browser)
        // release: Blink (chrome) "Stable release      23.0.1522.58 (July 17, 2014;"
        // notes: 12.16+
        stringAgent.add("Opera/9.80 (Windows NT 5.1) Presto/2.12.388 Version/12.16");
        stringAgent.add("Opera/9.80 (Windows NT 5.1) Presto/2.12.388 Version/12.17");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; Win64; x64) Presto/2.12.388 Version/12.16");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; Win64; x64) Presto/2.12.388 Version/12.17");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; WOW64) Presto/2.12.388 Version/12.16");
        stringAgent.add("Opera/9.80 (Windows NT 6.1; WOW64) Presto/2.12.388 Version/12.17");
        stringAgent.add("Opera/9.80 (X11; Linux x86_64) Presto/2.12.388 Version/12.16"); // 7
    }

    private static final ArrayList<String> portableAgent = new ArrayList<String>();
    static {
        portableAgent.add("Mozilla/5.0 (Android; Tablet; rv:30.0) Gecko/30.0 Firefox/30.0");
        portableAgent.add("Mozilla/5.0 (Android; Tablet; rv:33.0) Gecko/33.0 Firefox/33.0");
        portableAgent.add("Mozilla/5.0 (Android; Tablet; rv:34.0) Gecko/34.0 Firefox/34.0");
        portableAgent.add("Mozilla/5.0 (Android; Tablet; rv:35.0) Gecko/35.0 Firefox/35.0");// 4

        // chrome 34(andriod) 33(ios)

        portableAgent.add("Mozilla/5.0 (Linux; Android 4.4.4; D5503 Build/14.4.A.0.133) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.93 Mobile Safari/537.36");
        portableAgent.add("Mozilla/5.0 (Linux; Android 4.4.4; Nexus 10 Build/KTU84P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.59 Safari/537.36");
        portableAgent.add("Mozilla/5.0 (Linux; Android 4.4.4; Nexus 7 Build/KTU84P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.59 Safari/537.36");
        portableAgent.add("Mozilla/5.0 (Linux; Android 4.4.4; Nexus 7 Build/KTU84P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.93 Safari/537.36");
        portableAgent.add("Mozilla/5.0 (Linux; Android 5.0.1; Nexus 10 Build/LRX22C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.59 Safari/537.36");
        portableAgent.add("Mozilla/5.0 (Linux; Android 5.0.1; Nexus 10 Build/LRX22C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.93 Safari/537.36");
        portableAgent.add("Mozilla/5.0 (Linux; Android 5.0.2; Nexus 10 Build/LRX22G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.93 Safari/537.36");
        portableAgent.add("Mozilla/5.0 (Linux; Android 5.0; Nexus 10 Build/LRX21P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.59 Safari/537.36");
        portableAgent.add("Mozilla/5.0 (Linux; Android 5.0; Nexus 10 Build/LRX21P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.93 Safari/537.36");
        portableAgent.add("Mozilla/5.0 (Linux; Android 5.0; Nexus 7 Build/LRX21P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.59 Safari/537.36");
        portableAgent.add("Mozilla/5.0 (Linux; Android 5.0; Nexus 7 Build/LRX21P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.93 Safari/537.36");
        portableAgent.add("Mozilla/5.0 (iPad; CPU OS 8_1_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) CriOS/39.0.2171.50 Mobile/12B440 Safari/600.1.4");
        portableAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_2 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) CriOS/39.0.2171.50 Mobile/11D257 Safari/9537.53");

        portableAgent.add("Mozilla/5.0 (Linux; Android 5.0.2; Nexus 10 Build/LRX22G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.109 Safari/537.36");
        portableAgent.add("Mozilla/5.0 (Linux; Android 5.0.2; Nexus 10 Build/LRX22G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.89 Safari/537.36"); // 15

        // safari
        portableAgent.add("Mozilla/5.0 (iPad; CPU OS 8_0_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12A405 Safari/600.1.4");
        portableAgent.add("Mozilla/5.0 (iPad; CPU OS 8_0 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12A365 Safari/600.1.4");
        portableAgent.add("Mozilla/5.0 (iPad; CPU OS 8_1_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12B435 Safari/600.1.4");
        portableAgent.add("Mozilla/5.0 (iPad; CPU OS 8_1_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12B440 Safari/600.1.4");
        portableAgent.add("Mozilla/5.0 (iPad; CPU OS 8_1_3 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12B466 Safari/600.1.4");
        portableAgent.add("Mozilla/5.0 (iPad; CPU OS 8_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12B410 Safari/600.1.4");
        portableAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 8_1_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12B435 Safari/600.1.4");
        portableAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 8_1_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12B436 Safari/600.1.4");
        portableAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 8_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12B410 Safari/600.1.4");
        portableAgent.add("Mozilla/5.0 (iPhone; CPU iPhone OS 8_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12B411 Safari/600.1.4"); // 10

        // opera
        portableAgent.add("Opera/9.80 (Android 2.3.3; Linux; Opera Mobi/ADR-1212030829) Presto/2.11.355 Version/12.10");
        portableAgent.add("Opera/9.80 (Android 2.3.3; Linux; Opera Mobi/ADR-1301080958) Presto/2.11.355 Version/12.10"); // 2
    }

    private static final ArrayList<String> hbbtvAgent    = new ArrayList<String>();
    static {
        hbbtvAgent.add("Mozilla/4.0 (compatible; MSIE 5.23; Macintosh; PPC) Escape 5.1.8");
        hbbtvAgent.add("Mozilla/5.0 (SMART-TV; X11; Linux i686) AppleWebKit/534.7 (KHTML, like Gecko) Version/5.0 Safari/534.7");
        hbbtvAgent.add("Mozilla/5.0 (X11; U; Linux i686; en-US) AppleWebKit/533.4 (KHTML, like Gecko) Chrome/5.0.375.127 Large Screen Safari/533.4 GoogleTV/162671");
        hbbtvAgent.add("Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2) Gecko/20100222 Firefox/3.6 Kylo/0.6.1.70394");
        hbbtvAgent.add("Opera/9.80 (Linux armv7l; Opera TV Store/5581) Presto/2.12.362 Version/12.11");
        hbbtvAgent.add("iTunes-AppleTV/4.1");
        hbbtvAgent.add("Mozilla/5.0 (CrKey armv7l 1.4.15250) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.0 Safari/537.36");
        hbbtvAgent.add("Mozilla/5.0 (X11; U; Linux i686; en-US) AppleWebKit/533.4 (KHTML, like Gecko) Chrome/5.0.375.127 Large Screen Safari/533.4 GoogleTV/ 162671");
        hbbtvAgent.add("Mozilla/5.0 (X11; U: Linux i686; en-US) AppleWebKit/533.4 (KHTML, like Gecko) Chrome/5.0.375.127 Large Screen Safari/533.4 GoogleTV/b39389");
        hbbtvAgent.add("Mozilla/5.0 (X11; U; Linux i686; en-US) AppleWebKit/534.1 (KHTML, like Gecko) HbbTV/1.1.1 (+PVR;Mstar;OWB;;;)");
        hbbtvAgent.add("Opera/9.80 (Linux sh4; U; ; en; CreNova Build) AppleWebKit/533.1 (KHTML like Gecko) Version/4.0 Mobile Safari/533.1 HbbTV/1.1 (;CreNova;CNV001;1.0;1.0; FXM-U2FsdGVkX19AfSGBrU5pNwqodai+lZp2xktKFNHDE46SbYGa7Wp+eG5Z56WMDCQu-END; en) Presto/2.9 Version");
        hbbtvAgent.add("Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.4 (KHTML, like Gecko) MWB/1.0 Safari/537.4 HbbTV/1.2.1 (; Mstar; MWB;;;)");
        hbbtvAgent.add("Mozilla/5.0 (Linux mips; U;HbbTV/1.1.1 (+RTSP;DMM;Dreambox;0.1a;1.0;) CE-HTML/1.0; en) AppleWebKit/535.19 no/Volksbox QtWebkit/2.2");
        hbbtvAgent.add("Mozilla/5.0 (Linux; U; Android 4.1.1; en-gb; POV_TV-HDMI-KB-01 Build/JRO03H) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Safari/534.30");
        hbbtvAgent.add("Mozilla/5.0 (DirectFB; U; Linux 35230; en) AppleWebKit/531.2+ (KHTML, like Gecko) Safari/531.2+ LG Browser/4.1.4(+3D+SCREEN+TUNER; LGE; 42LW5700-SA; 04.02.28; 0x00000001;); LG NetCast.TV-2011");
        hbbtvAgent.add("Mozilla/5.0 (DirectFB; U; Linux mips; en) AppleWebKit/531.2+ (KHTML, like Gecko) Safari/531.2+ LG Browser/4.0.10(+SCREEN+TUNER; LGE; 42LE5500-SA; 04.02.02; 0x00000001;); LG NetCast.TV-2010");
        hbbtvAgent.add("Mozilla/5.0 (Unknown; Linux armv7l) AppleWebKit/537.1+ (KHTML, like Gecko) Safari/537.1+ LG Browser/6.00.00(+mouse+3D+SCREEN+TUNER; LGE; GLOBAL-PLAT5; 03.07.01; 0x00000001;); LG NetCast.TV-2013/03.17.01 (LG, GLOBAL-PLAT4, wired)");
        hbbtvAgent.add("Mozilla/5.0 (DirectFB; Linux armv7l) AppleWebKit/534.26+ (KHTML, like Gecko) Version/5.0 Safari/534.26+ HbbTV/1.1.1 ( ;LGE ;NetCast 3.0 ;1.0 ;1.0M ;)");
        hbbtvAgent.add("Mozilla/5.0 (Linux; U; Android 3.2; en-us; GTV100 Build/MASTER) AppleWebKit/534.13 (KHTML, like Gecko) Version/4.0 Safari/534.13");
        hbbtvAgent.add("Opera/9.80 (Linux i686; U; fr) Presto/2.10.287 Version/12.00 ; SC/IHD92 STB");
        hbbtvAgent.add("Mozilla/5.0 (FreeBSD; U; Viera; fr-FR) AppleWebKit/535.1 (KHTML, like Gecko) Viera/1.5.2 Chrome/14.0.835.202 Safari/535.1");
        hbbtvAgent.add("Mozilla/5.0 (X11; FreeBSD; U; Viera; de-DE) AppleWebKit/537.11 (KHTML, like Gecko) Viera/3.10.0 Chrome/23.0.1271.97 Safari/537.11");
        hbbtvAgent.add("Opera/9.70 (Linux armv6l ; U; CE-HTML/1.0 NETTV/2.0.2; en) Presto/2.2.1");
        hbbtvAgent.add("Opera/9.80 (Linux armv6l ; U; CE-HTML/1.0 NETTV/3.0.1;; en) Presto/2.6.33 Version/10.60");
        hbbtvAgent.add("Opera/9.80 (Linux mips; HbbTV/1.2.1 (; Philips; ; ; ; ) CE-HTML/1.0 NETTV/4.2.0 PHILIPSTV/1.1.1 Firmware/171.56.0 (PhilipsTV, 1.1.1,) en) Presto/2.12.362 Version/12.11");
        hbbtvAgent.add("Mozilla/5.0 (Linux; U; Android 4.1.1; nl-nl; POV_TV-HDMI-200BT Build/JRO03H) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Safari/534.30");
        hbbtvAgent.add("Roku/DVP-5.2 (025.02E03197A)");
        hbbtvAgent.add("Roku/DVP-5.0 (025.00E08043A)");
        hbbtvAgent.add("HbbTV/1.1.1 (;Samsung;SmartTV2013;T-FXPDEUC-1102.2;;) WebKit");
        hbbtvAgent.add("Mozilla/5.0 (SmartHub; SMART-TV; U; Linux/SmartTV) AppleWebKit/531.2+ (KHTML, like Gecko) WebBrowser/1.0 SmartTV Safari/531.2+");
        hbbtvAgent.add("Mozilla/5.0 (SMART-TV; X11; Linux i686) AppleWebKit/535.20+ (KHTML, like Gecko) Version/5.0 Safari/535.20+");
        hbbtvAgent.add("Mozilla/5.0 (SmartHub; SMART-TV; U; Linux/SmartTV; Maple2012)");
        hbbtvAgent.add("Mozilla/5.0 (SmartHub; SMART-TV; U; Linux/SmartTV; Maple2012) AppleWebKit/534.7 (KHTML, like Gecko) SmartTV Safari/534.7");
        hbbtvAgent.add("Mozilla/4.0 (compatible; Gecko/20041115) Maple 5.0.0 Navi");
        hbbtvAgent.add("Mozilla/5.0 (DTV) AppleWebKit/531.2+ (KHTML, like Gecko) Espial/6.1.5 AQUOSBrowser/2.0 (US01DTV;V;0001;0001)");
        hbbtvAgent.add("Mozilla/5.0 (DTV) AppleWebKit/531.2+ (KHTML, like Gecko) Espial/6.0.4");
        hbbtvAgent.add("Opera/9.80 (Linux armv7l; InettvBrowser/2.2 (00014A;SonyDTV115;0002;0100) KDL42W650A; CC/GRC) Presto/2.12.362 Version/12.11");
        hbbtvAgent.add("Opera/9.80 (Linux armv6l; Opera TV Store/5599; (SonyBDP/BDV13)) Presto/2.12.362 Version/12.11");
        hbbtvAgent.add("Opera/9.80 (Linux sh4; U; HbbTV/1.1.1 (;;;;;); CE-HTML; TechniSat Digit ISIO S; de) Presto/2.9.167 Version/11.50");
        hbbtvAgent.add("Mozilla/5.0 (DTV; TSBNetTV/T32013713.0203.7DD; TVwithVideoPlayer; like Gecko) NetFront/4.1 DTVNetBrowser/2.2 (000039;T32013713;0203;7DD) InettvBrowser/2.2 (000039;T32013713;0203;7DD)");
        hbbtvAgent.add("Mozilla/5.0 (Linux; GoogleTV 3.2; VAP430 Build/MASTER) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.77 Safari/534.24");

    }

    /**
     * Returns a random User-Agent String (from a HbbTV supported device) of specified array. This array contains current user agents
     * gathered from httpd access logs. Benefits over RandomUserAgent.* are: versions and respective release dates are valid. Source:
     * https://udger.com/resources/ua-list/device-detail?device=Smart+TV
     *
     * @return eg. "Opera/9.80 (Linux mips ; U; hbbtv/1.1.1 (;Philips;;;; ) CE-HTML/1.0 NETTV/3.0.0; en) Presto/2.6.33 Version/10.70"
     */
    public static String hbbtvUserAgent() {
        final Random rand = new Random();
        final int i = rand.nextInt(hbbtvAgent.size());
        final String ret = hbbtvAgent.get(i);
        return ret;
    }

    /**
     * Returns a random User-Agent String (from a portable device) of specified array. This array contains current user agents gathered from
     * httpd access logs. Benefits over RandomUserAgent.* are: versions and respective release dates are valid.
     *
     * @return eg. "Opera/9.80 (Android 4.0.3; Linux; Opera Mobi/ADR-1205181138; U; en) Presto/2.10.254 Version/12.00"
     * @author raztoki
     */
    public static String portableUserAgent() {
        final Random rand = new Random();
        final int i = rand.nextInt(portableAgent.size());
        final String ret = portableAgent.get(i);
        return ret;
    }

    /**
     * Returns a random User-Agent String (common browsers) of specified array. This array contains current user agents gathered from httpd
     * access logs. Benefits over RandomUserAgent.* are: versions and respective release dates are valid.
     *
     * @return eg. "Opera/9.80 (X11; Linux i686; U; en) Presto/2.6.30 Version/10.63"
     * @author raztoki
     */
    public static String stringUserAgent() {
        final Random rand = new Random();
        final int i = rand.nextInt(stringAgent.size());
        final String ret = stringAgent.get(i);
        return ret;
    }

}
