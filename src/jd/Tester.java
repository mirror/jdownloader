package jd;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

public class Tester extends JFrame {

    private static final long serialVersionUID = -2399077821905795553L;

    public Tester() {
        super();
        this.add(new JScrollPane(createPanel()));
        pack();
        setSize(new Dimension(300, 300));
        setVisible(true);
    }

    private Component createPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 5,wrap 1", "[]"));
        JTextArea ta = new JTextArea();
        ta.setLineWrap(true);
        
        ta.setText("This is just a very long dummytext. it is very long, so the textarea tries to be bigger than the jframe\rn\r\n\r\n\r\n\r\n\r\n\n\r\n\r\n\r\n\r\n\n\r\n\r\n\r\n\r\n\n\r\n\r\n\r\n\r\n\n\r\n\r\n\r\n\r\nHere is some text, too");
        panel.add(ta,"growx,pushx");
        panel.add(new JButton("Click me or not"), "alignx right");
        return panel;
    }

    public static void main(String[] args) {
        new Tester();
    }

}
