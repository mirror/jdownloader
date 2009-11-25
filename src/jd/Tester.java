package jd;

import javax.swing.JFrame;
import javax.swing.JLabel;

import jd.utils.JDTheme;

import org.jdesktop.swingx.border.IconBorder;

public class Tester {

    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame();
        JLabel label = new JLabel("IconBorder Test");
        label.setIcon(JDTheme.II("gui.images.premium", 16, 16));
        label.setBorder(new IconBorder(JDTheme.II("gui.images.resume", 16, 16), JLabel.EAST));
        frame.add(label);
        frame.pack();
        frame.setVisible(true);
    }

}