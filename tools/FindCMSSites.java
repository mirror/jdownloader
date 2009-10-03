

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;

import jd.http.Browser;
import jd.plugins.decrypter.CMS;

public class FindCMSSites {
    public static void main(String[] args) {
        Vector<String> urls = new Vector<String>();
        for (int i = 0; i < 10; i++) {
            String url = "http://www.google.de/search?hl=de&q=\"Powered+by+Underground+CMS\"&start=" + i * 10 + "&sa=N";
            Browser br = new Browser();
            try {
                br.getPage(url);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // System.out.println(url);
            // System.out.println(br);
            String[] urlsc = br.getRegex("<a href=\"([^\"]*)\"\\s*class=l").getColumn(0);
            for (String string : urlsc) {
                String host;
                try {
                    host = new URI(string).getHost().toLowerCase();
                    if (host.matches(".*\\..*\\..*")) host = host.replaceFirst("[^.]*\\.", "");
                    // System.out.println(host);

                    if (!urls.contains(host)) urls.add(host);
                } catch (URISyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        }
        outer: for (String string : urls) {

            for (String decryptPluginWrapper : CMS.ANNOTATION_NAMES) {
                String host = decryptPluginWrapper.toLowerCase();
                if (host.matches(".*\\..*\\..*")) host = host.replaceFirst("[^.]*\\.", "");
                if (host.equals(string)) continue outer;
            }
            System.out.println("\"" + string + "\",");
        }
        /*
         * System.out.println(br); String[] links2 =br.getRegex(
         * "onclick=\"window.open\\(\\'([^']*)\\'\\)\\;\" value=\"Download\""
         * ).getColumn(0); for (String string : links2) {
         * System.out.println(string); }
         */

    }
}
