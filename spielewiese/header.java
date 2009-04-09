import java.awt.Canvas;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
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
        GraphicsEnvironment ge = GraphicsEnvironment.
        getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (int j = 0; j < gs.length; j++) { 
           GraphicsDevice gd = gs[j];
            {
              JFrame f = new
              JFrame(gd.getDefaultConfiguration());
              Canvas c = new Canvas(gd.getDefaultConfiguration()); 
              Rectangle gcBounds = gd.getDefaultConfiguration().getBounds();
              int xoffs = gcBounds.x;
              int yoffs = gcBounds.y;
                f.getContentPane().add(c);
                f.setLocation((10*50)+xoffs, (10*60)+yoffs);
              f.show();
            }  
        }

    }

 
}
