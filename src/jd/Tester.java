package jd;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.UIManager;

import jd.gui.swing.components.linkbutton.JLink;
import net.miginfocom.swing.MigLayout;
import de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel;

public class Tester {
    public static void main(String args[]) throws Exception {
        // UIManager.put("Synthetica​.rootPane​.titlePane​.title​.center",
        // Boolean.TRUE);
        UIManager.put("Synthetica.window.decoration", false);
        UIManager.setLookAndFeel(new SyntheticaStandardLookAndFeel());

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("TESTER");
                frame.setPreferredSize(new Dimension(400, 300));
                frame.setLayout(new MigLayout("ins 5,wrap 1"));
                frame.add(new JLabel("TEstlink2"));
                frame.add(new JLink("TEstlink", "http://google.de"));
                frame.pack();
                frame.setVisible(true);

            }
        });

    }

}
