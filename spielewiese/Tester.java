import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.ViewportLayout;

import net.miginfocom.swing.MigLayout;

public class Tester extends JFrame {

    private static final long serialVersionUID = -2399077821905795553L;
    private JViewport vp;

    public Tester() {
        super();
        setLayout(new MigLayout("ins 5,wrap 2", "[]"));
        JScrollPane sp;
        // vp = new JViewport();
        // JScrollBar sb = new ScrollBar(JScrollBar.VERTICAL);
        // vp.setView();
        this.add(sp = new JScrollPane(), "growx,pushx");
        sp.setViewport(vp = new JViewport() {

            private static final long serialVersionUID = 1L;

            public void setViewSize(Dimension newSize) {
                newSize.width = Tester.this.getSize().width;
                super.setViewSize(newSize);
            }
        });
        vp.setView(createPanel());

        sp.getViewport().setLayout(new ViewportLayout() {

            private static final long serialVersionUID = 1L;

            public Dimension preferredLayoutSize(Container parent) {

                Dimension ret = super.preferredLayoutSize(parent);
                ret.width = 0;
                return ret;

            }

        });
        // this.add(sb, "pushy,growy");
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
        panel.add(ta, "growx,pushx");
        panel.add(new JButton("Click me or not"), "alignx right");

        return panel;
    }

    public static void main(String[] args) {
        new Tester();
    }
}
