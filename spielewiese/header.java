import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import jd.http.Browser;
import jd.http.JDProxy;
import jd.plugins.Plugin;
import jd.update.JDUpdateUtils;

public class header {

    /**
     * @param args
     * @throws UnsupportedEncodingException
     * @throws java.text.ParseException
     * @throws ParseException
     * @throws UnsupportedEncodingException
     * @throws java.text.ParseException
     */
    public static String base64totext(String t) {
        String b64s = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_\"";
        int m = 0;
        int a = 0;
        String r = "";
        for (int n = 0; n < t.length(); n++) {
            int c = b64s.indexOf(t.charAt(n));
            if (c >= 0) {
                if (m != 0) {
                    int ch = (c << (8 - m)) & 255 | a;
                    char s = (char) ch;
                    r += s;
                }
                a = c >> m;
                m = m + 2;
                if (m == 8) m = 0;
            }
        }
        return r;
    }

    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub
        if (true) {
            //    

            Browser br = new Browser();
            Browser.init();
            br.setDebug(true);

            JDProxy p = new JDProxy(JDProxy.Type.SOCKS, "localhost", 1080);
            p.setUser("");
            p.setPass("");
            br.setProxy(p);
            br.setAuth("service.jdownloader.org", "", "");
           
            br.getPage("https://ssl.rapidshare.com/premiumzone.html");
            jd.parser.html.Form forms = br.getForms()[0];
            forms.put("login", "");
            forms.put("password", "");
            //br.submitForm(forms);

            System.out.println(br + "");

        }
        if (false) {
            SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy");
            String expires = "Sat Dec 13 21:27:14 CET 2008";
            try {
                System.out.println(DATE_FORMAT.parse(expires));
            } catch (ParseException e) {
                e.printStackTrace();
                try {
                    System.out.println(new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z").parse(expires));
                } catch (ParseException e2) {
                    e2.printStackTrace();

                }
            }
        }

        // if (false) {
        //
        // Date dt = new Date();
        // // Festlegung des Formats:
        // SimpleDateFormat df = new
        // SimpleDateFormat("EEE, dd-MMM-yyyy hh:mm:ss z");
        // // df.setTimeZone( TimeZone.getDefault() ); // nicht mehr unbedingt
        // // notwendig seit JDK 1.2
        // // Formatierung zu String:
        // System.out.println("Date = " + df.format(dt)); // z.B. '2001-01-26
        // // 19:03:56.731'
        // // Ausgabe für andere Zeitzone:
        // dt = df.parse("Fri, 07-Nov-2008 12:54:19 GMT");
        // System.out.println("parse = " + df.format(dt)); // z.B. '2001-02-03
        // // 04:05:06.7'
        //
        // }
        // if (false) {
        // String runtimeVersion = "1.4.2_0-b12".toLowerCase();
        // if (!new Regex(runtimeVersion, "1\\.(5|6)").matches()) {
        //
        // System.out.println("Wrong Java Version");
        //
        // } else {
        // System.out.println("Right Java Version");
        // }
        // }
        if (false) {
            JDUpdateUtils.setUpdateUrl("http://service.jdownloader.org/update/update.zip");
            String jj = JDUpdateUtils.get_AddonList();
            String kk = JDUpdateUtils.get_UpdateList();
        }
        if (false) {
            System.out.println(base64totext("kALaLX9NF0SfcVSf99CsZ5umsHeyg4tGhJ\"ePoCNNxv6\"mIeln34L0YRoEGBwB6lB3snQVhp1yCSznAgpjFWa\"oC_P5bc0dNsFTc6sw3YqNfRD\"hJ_RZTmQD\"s\"CwGUnTxxDPQ3zfGW8Z0A9mqLP95Qg9zwr4YLuhSIC9gOHf7TawDE\"vDUs_jI36\"CbIy\"n1I3o03BD05k6mOx3nr6C9b8tqBwhgbxV17MnrGRO0IRX2WD06r8eQndm5q9Yt8I1qRNeKMUpKIqrpGj"));
        }
        if (false) {
            // System.out.println(new Regex(
            // "<p style=\"color:red;\">You have reached the download limit for free-users. Would you like more?</p>"
            // , ".*download.{0,3}limit.{1,50}free.{0,3}users.*").matches() +
            // "");
            String l[] = { "filename= \"=?UTF-8?B?WmZyIFByb2plY3QgLSBBZGRpY3RlZCBNZiAoT3JpZ2luYWwgTWl4KS0tLXd3dy50ZWNobm9yb2NrZXIuaW5mby5tcDM=?=\"", "filename==?UTF-8?B?YWlhZGNhZGlnMWR4bi5wYXJ0Mi5yYXI=?=" };
            // String l[] = {
            // "filename==?UTF-8?B?WmZyIFByb2plY3QgLSBBZGRpY3RlZCBNZiAoT3JpZ2luYWwgTWl4KS0tLXd3dy50ZWNobm9yb2NrZXIuaW5mby5tcDM=?="
            // ,
            // "=?UTF-8?attachment;filename=\"Dead.Space.Downfall.2008.DVDRip.XViD-WPi.by.flavioms.avi\";?="
            // , "attachment; filename =     \"foo - %c3%a4.html'  ;",
            // "attachment; filename* =UTF-8''foo - %c3%a4.html  ;",
            // "attachment; filename='Spec T feat. Cherish - Killa (Lil' Prince Crunk Remix) 2oo8-GsE.mp3'"
            // ,
            // "attachment; filename=Spec T feat. Cherish - Killa (Lil' Prince Crunk Remix) 2oo8-GsE.mp3"
            // ,
            // "attachment; filename=\"Spec T feat. Cherish - Killa (Lil' Prince Crunk Remix) 2oo8-GsE.mp3\""
            // , "attachment; filename =     \"foo - %c3%a4.html'  ;",
            // "attachment; filename* =UTF-8''foo - %c3%a4.html;",
            // "attachment; filename*=\" UTF-8''foo - %c3%a4.html\"",
            // "attachment; filename*= UTF-8''foo - %c3%a4.html",
            // "attachment; filename=\"foo-%c3%a4-%e2%82%ac.html\"",
            // "inline; filena=foo  g .html", "inline; filename=foo  g .html",
            // "inline; filename=\"foo  g .html\"",
            // "inline; filename=\"foo.html\"",
            // "attachment; filename=\"foo.html\"",
            // "attachment; filename=foo.html",
            // "attachment; filename=\"foo-ä.html\"",
            // "attachment; filename=\"foo-Ã¤.html\"",
            // "attachment; filename=\"foo-%41.html\"",
            // "attachment; filename=\"foo-%c3%a4-%e2%82%ac.html\"",
            // "attachment; filename =\"foo.html\"",
            // "attachment; filename= \"foo.html\"",
            // "attachment; filename*=iso-8859-1''foo-%E4.html",
            // "attachment; filename*=UTF-8''foo-%c3%a4-%e2%82%ac.html",
            // "attachment; filename*=UTF-8''foo-a%cc%88.html",
            // "attachment; filename*= UTF-8''foo-%c3%a4.html" };
            for (String kk : l) {
                System.out.print(kk + ">>");
                System.out.println(Plugin.getFileNameFromDispositionHeader(kk));
            }
        }
        // String k = "Test : ö & üä ! \" § $ & / ( ) = ? ~ ^ ° ¬æ " +
        // Encoding.urlEncode("%");
        // System.out.println(k);
        // System.out.println((k = Encoding.urlEncode_light(k)));
        // System.out.println((k = Encoding.urlEncode_light(k)));
        // System.out.println(Encoding.urlDecode(k, false));

        //        
        // System.out.println(Encoding.urlEncode(
        // "http://srv2.shragle.com/dl/free/UjdV0050/Der KÃ¶nig der LÃ¶wen.part1.rar?v=1"
        // ));
        // System.out.println(Encoding.urlEncode(
        // "http://srv2.shragle.com/dl/free/BGFQ4357/Der König der Löwen.part1.rar"
        // ));
        // System.out.println(new String(Plugin.extractFileNameFromURL(
        // "http://free.srv2.shragle.com/BGFQ4357/Der%20K%c3%b6nig%20der%20L%c3%b6wen.part1.rar"
        // ).getBytes("ISO-8859-1"), "UTF-8"));
        //System.out.println(Encoding.urlEncode("die ist ein test und / hallöle"
        // ));
    }

}
