import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jd.parser.Regex;
import jd.plugins.Plugin;

import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.ParseException;

public class header {

    /**
     * @param args
     * @throws ParseException
     * @throws UnsupportedEncodingException
     * @throws java.text.ParseException
     */
    public static void main(String[] args) throws ParseException, UnsupportedEncodingException, java.text.ParseException {
        // TODO Auto-generated method stub
        

        if (true) {

            Date dt = new Date();
            // Festlegung des Formats:
            SimpleDateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy hh:mm:ss z");
            // df.setTimeZone( TimeZone.getDefault() ); // nicht mehr unbedingt
            // notwendig seit JDK 1.2
            // Formatierung zu String:
            System.out.println("Date = " + df.format(dt)); // z.B. '2001-01-26
                                                           // 19:03:56.731'
            // Ausgabe für andere Zeitzone:
            dt = df.parse("Fri, 07-Nov-2008 12:54:19 GMT");
            System.out.println("parse = " + df.format(dt)); // z.B. '2001-02-03
                                                            // 04:05:06.7'

        }
        if (false) {
            String runtimeVersion = "1.4.2_0-b12".toLowerCase();
            if (!new Regex(runtimeVersion, "1\\.(5|6)").matches()) {

                System.out.println("Wrong Java Version");

            } else {
                System.out.println("Right Java Version");
            }
        }
        if (false) {
            System.out.println(new Regex("<p style=\"color:red;\">You have reached the download limit for free-users. Would you like more?</p>", ".*download.{0,3}limit.{1,50}free.{0,3}users.*").matches() + "");
            String l[] = { "=?UTF-8?attachment;filename=\"Dead.Space.Downfall.2008.DVDRip.XViD-WPi.by.flavioms.avi\";?=", "attachment; filename =     \"foo - %c3%a4.html'  ;", "attachment; filename* =UTF-8''foo - %c3%a4.html  ;", "attachment; filename='Spec T feat. Cherish - Killa (Lil' Prince Crunk Remix) 2oo8-GsE.mp3'", "attachment; filename=Spec T feat. Cherish - Killa (Lil' Prince Crunk Remix) 2oo8-GsE.mp3", "attachment; filename=\"Spec T feat. Cherish - Killa (Lil' Prince Crunk Remix) 2oo8-GsE.mp3\"", "attachment; filename =     \"foo - %c3%a4.html'  ;", "attachment; filename* =UTF-8''foo - %c3%a4.html;", "attachment; filename*=\" UTF-8''foo - %c3%a4.html\"", "attachment; filename*= UTF-8''foo - %c3%a4.html", "attachment; filename=\"foo-%c3%a4-%e2%82%ac.html\"", "inline; filena=foo  g .html", "inline; filename=foo  g .html", "inline; filename=\"foo  g .html\"",
                    "inline; filename=\"foo.html\"", "attachment; filename=\"foo.html\"", "attachment; filename=foo.html", "attachment; filename=\"foo-ä.html\"", "attachment; filename=\"foo-Ã¤.html\"", "attachment; filename=\"foo-%41.html\"", "attachment; filename=\"foo-%c3%a4-%e2%82%ac.html\"", "attachment; filename =\"foo.html\"", "attachment; filename= \"foo.html\"", "attachment; filename*=iso-8859-1''foo-%E4.html", "attachment; filename*=UTF-8''foo-%c3%a4-%e2%82%ac.html", "attachment; filename*=UTF-8''foo-a%cc%88.html", "attachment; filename*= UTF-8''foo-%c3%a4.html" };
            for (String kk : l) {
                System.out.print(kk + ">>");
                System.out.println(Plugin.getFileNameFromDispositionHeader(kk));
            }
        }
    }

}
