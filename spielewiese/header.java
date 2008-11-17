import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import jd.plugins.Plugin;



public class header {

    /**
     * @param args
     * @throws java.text.ParseException
     * @throws ParseException
     * @throws UnsupportedEncodingException
     * @throws java.text.ParseException
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

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
        if (true) {
            //System.out.println(new Regex("<p style=\"color:red;\">You have reached the download limit for free-users. Would you like more?</p>", ".*download.{0,3}limit.{1,50}free.{0,3}users.*").matches() + "");
            String l[] = {"filename==?UTF-8?B?WmZyIFByb2plY3QgLSBBZGRpY3RlZCBNZiAoT3JpZ2luYWwgTWl4KS0tLXd3dy50ZWNobm9yb2NrZXIuaW5mby5tcDM=?="};
            //String l[] = {"filename==?UTF-8?B?WmZyIFByb2plY3QgLSBBZGRpY3RlZCBNZiAoT3JpZ2luYWwgTWl4KS0tLXd3dy50ZWNobm9yb2NrZXIuaW5mby5tcDM=?=", "=?UTF-8?attachment;filename=\"Dead.Space.Downfall.2008.DVDRip.XViD-WPi.by.flavioms.avi\";?=", "attachment; filename =     \"foo - %c3%a4.html'  ;", "attachment; filename* =UTF-8''foo - %c3%a4.html  ;", "attachment; filename='Spec T feat. Cherish - Killa (Lil' Prince Crunk Remix) 2oo8-GsE.mp3'", "attachment; filename=Spec T feat. Cherish - Killa (Lil' Prince Crunk Remix) 2oo8-GsE.mp3", "attachment; filename=\"Spec T feat. Cherish - Killa (Lil' Prince Crunk Remix) 2oo8-GsE.mp3\"", "attachment; filename =     \"foo - %c3%a4.html'  ;", "attachment; filename* =UTF-8''foo - %c3%a4.html;", "attachment; filename*=\" UTF-8''foo - %c3%a4.html\"", "attachment; filename*= UTF-8''foo - %c3%a4.html", "attachment; filename=\"foo-%c3%a4-%e2%82%ac.html\"", "inline; filena=foo  g .html", "inline; filename=foo  g .html", "inline; filename=\"foo  g .html\"",
             //       "inline; filename=\"foo.html\"", "attachment; filename=\"foo.html\"", "attachment; filename=foo.html", "attachment; filename=\"foo-ä.html\"", "attachment; filename=\"foo-Ã¤.html\"", "attachment; filename=\"foo-%41.html\"", "attachment; filename=\"foo-%c3%a4-%e2%82%ac.html\"", "attachment; filename =\"foo.html\"", "attachment; filename= \"foo.html\"", "attachment; filename*=iso-8859-1''foo-%E4.html", "attachment; filename*=UTF-8''foo-%c3%a4-%e2%82%ac.html", "attachment; filename*=UTF-8''foo-a%cc%88.html", "attachment; filename*= UTF-8''foo-%c3%a4.html" };
            for (String kk : l) {
                System.out.print(kk + ">>");
                System.out.println(Plugin.getFileNameFromDispositionHeader(kk));
            }
        }
    }

}
