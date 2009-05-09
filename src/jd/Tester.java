package jd;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.miginfocom.swing.MigLayout;

public class Tester extends JFrame {
    public Tester() {
        super(); 
        this.setLayout(new MigLayout("ins 5,debug", "[grow,fill]", "[grow,fill]"));
        this.add(new JButton("Button"), "height 300!,width 300!");
        setMinimumSize(getPreferredSize());
        pack();
        setVisible(true);
    }
    public static void main(String[] args) {
        new Tester();

    }

}
