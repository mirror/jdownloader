package jd;

import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;


public class Tester {
    public static void main(String ss[]) throws Exception {
        JFrame parent = new JFrame("Test");
        parent.setSize(500, 400);
        parent.setVisible(true);
        System.out.println(parent.isAlwaysOnTop());
        parent.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        new DDialog(parent);
    }

    static class DDialog extends JDialog {
        public DDialog(JFrame parent) {
            super(parent);
            this.setAlwaysOnTop(true);
            System.out.println(parent.isAlwaysOnTop());

            this.setModal(true);
            this.setMinimumSize(new Dimension(200, 200));
            this.setVisible(true);
            System.out.println(parent.isAlwaysOnTop());

        }

    }
}

