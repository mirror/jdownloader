package jd;

import javax.swing.JSpinner;
import javax.swing.JWindow;

import net.miginfocom.swing.MigLayout;

public class Tester2 {

    public static void main(String[] args) {
        JWindow w = new JWindow();
        w.setLocationRelativeTo(null);
        w.setLayout(new MigLayout("ins 0"));
        w.add(new JSpinner(), "w 200!");
        w.pack();
        w.setVisible(true);
    }

}
