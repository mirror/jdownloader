package jd;

import java.awt.Cursor;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Tester {

    private static final int MASS = 50000;

    /**
     * @param args
     */
    public static void main(String[] args) {

        JFrame frame = new JFrame();
        frame.setLayout(new GridLayout(2, 2));
        frame.add(new JButton("1"));
        frame.add(new JButton("2"));
        /**
         * create component with Handcursor
         */
        JLabel lbl;
        frame.add(lbl = new JLabel("CURSOR"));
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        frame.add(new JButton("3"));
        frame.add(new JButton("4"));
        frame.add(new JButton("5"));

        JPanel glass = new JPanel();
        glass.setOpaque(false);
        glass.add(new JLabel("I'm glassy"));

        frame.setGlassPane(glass);
         frame.getGlassPane().setVisible(true);

        frame.setVisible(true);
        frame.pack();
    }

}
