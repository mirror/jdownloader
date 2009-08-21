package jd;

import java.awt.Dimension;
import java.lang.reflect.Method;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class Tester extends JFrame {

    public static void main(String s[]) throws Exception {

        Class<?> slaf = Class.forName("de.javasoft.plaf.synthetica.SyntheticaLookAndFeel");

        Method method = slaf.getMethod("setLookAndFeel", new Class[] { String.class });
        method.invoke(null, new Object[] { "de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel" });
        Tester tester = new Tester();

        tester.setVisible(true);
        tester.pack();
    }

    private Tester() {

        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("Menu Label");
        menuBar.add(menu);

        JMenuItem item = new JMenuItem("Item Label");

        menu.add(item);

        setJMenuBar(menuBar);

        // submenu

        menuBar = new JMenuBar();

        menu = new JMenu("Menu Label");
        menuBar.add(menu);

        item = new JMenuItem("Item Label");

        menu.add(item);

        add(menuBar);

        this.setPreferredSize(new Dimension(500, 400));

    }

}
