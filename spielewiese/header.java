import java.awt.BorderLayout;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXCollapsiblePane;

import jd.http.Browser;
import jd.http.JDProxy;

import jd.parser.Regex;

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
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f = new JFrame("Test Oriented Collapsible Pane");

                f.add(new JLabel("Press Ctrl+F or Ctrl+G to collapse panes."), BorderLayout.NORTH);

                JTree tree1 = new JTree();
                tree1.setBorder(BorderFactory.createEtchedBorder());
                f.add(tree1);

                JXCollapsiblePane pane = new JXCollapsiblePane();
                pane.setCollapsed(true);
                JTree tree2 = new JTree();
                tree2.setBorder(BorderFactory.createEtchedBorder());
                pane.add(tree2);
                f.add(pane, BorderLayout.SOUTH);

                pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl F"), JXCollapsiblePane.TOGGLE_ACTION);

                pane = new JXCollapsiblePane();
                JTree tree3 = new JTree();
                pane.add(tree3);
                tree3.setBorder(BorderFactory.createEtchedBorder());
                f.add(pane, BorderLayout.WEST);

                pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl G"), JXCollapsiblePane.TOGGLE_ACTION);

                f.setSize(640, 480);
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setVisible(true);
            }
        });

    }

}
