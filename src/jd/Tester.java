package jd;

import javax.swing.JDialog;
import javax.swing.JTextPane;

import net.miginfocom.swing.MigLayout;

public class Tester {

   
    public static void main(String[] args) {

        JDialog frame = new JDialog();

        frame.setLayout(new MigLayout("ins 0,debug", "[fill,grow]", "[fill,grow]"));
        JTextPane txt = new JTextPane();
        txt.setOpaque(false);
        txt.setText("This ");
txt.setBounds(0, 0, 100, 100);
        frame.add(txt, "width n:n:150");

        frame.pack();
        frame.setVisible(true);
    }
}
