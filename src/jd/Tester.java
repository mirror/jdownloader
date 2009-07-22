package jd;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.UIManager;

import de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel;

public class Tester {
    public static void main(String args[]) throws Exception {
        UIManager.put("Synthetica​.rootPane​.titlePane​.title​.center", Boolean.TRUE);
        UIManager.setLookAndFeel(new SyntheticaStandardLookAndFeel());
      
       

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("TESTER");
                frame.setPreferredSize(new Dimension(400, 300));
                frame.setVisible(true);
                frame.pack();

            }
        });

    }

}
