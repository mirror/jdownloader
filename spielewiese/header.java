import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import javax.swing.JFrame;

import org.jdesktop.swingx.JXLoginDialog;

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

    }

 
}
