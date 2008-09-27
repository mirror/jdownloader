import java.io.UnsupportedEncodingException;

import jd.plugins.Plugin;

import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.ParseException;

public class header {

    /**
     * @param args
     * @throws ParseException
     * @throws UnsupportedEncodingException
     */
    public static void main(String[] args) throws ParseException, UnsupportedEncodingException {
        // TODO Auto-generated method stub
        String l[] = { "attachment; filename*= UTF-8''foo - %c3%a4.html" ,"attachment; filename=\"foo-%c3%a4-%e2%82%ac.html\"", "inline; filena=foo  g .html","inline; filename=foo  g .html", "inline; filename=\"foo  g .html\"","inline; filename=\"foo.html\"", "attachment; filename=\"foo.html\"", "attachment; filename=foo.html", "attachment; filename=\"foo-ä.html\"", "attachment; filename=\"foo-Ã¤.html\"", "attachment; filename=\"foo-%41.html\"", "attachment; filename=\"foo-%c3%a4-%e2%82%ac.html\"", "attachment; filename =\"foo.html\"", "attachment; filename= \"foo.html\"", "attachment; filename*=iso-8859-1''foo-%E4.html", "attachment; filename*=UTF-8''foo-%c3%a4-%e2%82%ac.html", "attachment; filename*=UTF-8''foo-a%cc%88.html", "attachment; filename*= UTF-8''foo-%c3%a4.html" };
        for (String kk : l) {
            System.out.print(kk + ">>");
            System.out.println(Plugin.getFileNameFromDispositionHeader(kk));
        }
    }

}
