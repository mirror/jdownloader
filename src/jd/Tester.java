package jd;

import javax.swing.JFrame;
import javax.swing.JSpinner;

import net.miginfocom.swing.MigLayout;

public class Tester {

    public static void main(String[] args) throws Exception {
        JFrame w = new JFrame();
        w.setUndecorated(true);
        w.setLocationRelativeTo(null);
        w.setLayout(new MigLayout("ins 0"));
        w.add(new JSpinner(), "w 200!");
        w.pack();
        w.setVisible(true);
    }

}