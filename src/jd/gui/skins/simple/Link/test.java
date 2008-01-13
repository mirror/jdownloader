package jd.gui.skins.simple.Link;

import java.awt.GridLayout;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JFrame;

public class test extends JButton {

  /**
     * 
     */
    private static final long serialVersionUID = -3572661017084847518L;

public static void main(String[] a) {
    JFrame f = new JFrame();
    f.getContentPane().setLayout(new GridLayout(0,2));
    f.getContentPane().add(new JLinkButton("http://rapidshare.com/de/faq.html"));
    try {
        f.getContentPane().add(new JLinkButton("AGB", new URL("http://rapidshare.com/de/faq.html")));
    } catch (MalformedURLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    f.setSize(600, 200);
    f.setVisible(true);
  }
}