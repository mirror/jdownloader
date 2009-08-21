package jd;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class Tester extends JFrame {

    public static void main(String s[]) throws Exception {
        de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setLookAndFeel("de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel", false, true);
        Tester tester = new Tester();
        tester.setVisible(true);
        tester.pack();
    }

    private Tester() {
        //Main menu initialisieren
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu Label");
        menuBar.add(menu);
        JMenuItem item = new JMenuItem("Item Label");
        menu.add(item);
        setJMenuBar(menuBar);

        // weitere jmenubar irgendwo hinsetzen

        menuBar = new JMenuBar();
        menu = new JMenu("Menu Label");
        menuBar.add(menu);
        item = new JMenuItem("Item Label");
        menu.add(item);
        add(menuBar);

        this.setPreferredSize(new Dimension(500, 400));

    }

}
