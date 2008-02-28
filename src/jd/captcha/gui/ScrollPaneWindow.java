package jd.captcha.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Image;


import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Die Klasse dient als WIndow mit Scrollpane.
 * 
 * @author JD-Team
 */
@SuppressWarnings("serial")
public class ScrollPaneWindow extends BasicWindow {

 
    private JPanel panel;

    /**
     * @param owner
     */
    public ScrollPaneWindow(Object owner) {
        super(owner);
        panel = new JPanel();
        this.setLayout(new BorderLayout());
        panel.setLayout(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(panel);
        this.add(scrollPane, BorderLayout.CENTER);
        setSize(200, 200);
        setLocation(0, 0);
        setTitle("new ScrollPaneWindow");
        setVisible(true);
        pack();
        repack();

    }
    /**
     * Fügt relative Threadsafe  an x,y die Kompoente cmp ein
     * @param x
     * @param y
     * @param cmp
     */
    public void setComponent(final int x, final int y, final Component cmp) {
        if(cmp==null)return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                panel.add(cmp, getGBC(x, y, 1, 1));
               
            }
        });
    }
    /**
     * Fügt relative Threadsafe  an x,y den text cmp ein
     * @param x
     * @param y
     * @param cmp
     */
    public void setText(final int x, final int y, final Object cmp) {
        if(cmp==null)return;
        // final ScrollPaneWindow _this=this;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JTextField tf = new JTextField();
                tf.setText(cmp.toString());
                panel.add(tf, getGBC(x, y, 1, 1));
              ;
            }
        });

    }
    /**
     * Fügt relative Threadsafe  an x,y das Bild img ein
     * @param x
     * @param y
     * @param img 
     */
    public void setImage(final int x, final int y, final Image img) {
        if(img==null)return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                panel.add(new ImageComponent(img), getGBC(x, y, 1, 1));

            }
        });
    }

}