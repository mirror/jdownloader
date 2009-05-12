package jd.gui.skins.simple;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class JDCollapser extends JPanel implements MouseListener {

    private static final long serialVersionUID = 6864885344815243560L;
    private static JDCollapser INSTANCE = null;

    public static JDCollapser getInstance() {
        if (INSTANCE == null) INSTANCE = new JDCollapser();
        return INSTANCE;
    }

    private JTabbedPanel panel;
    private JLabel title;
    private JPanel content;

    private JDCollapser() {
        super(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[]5[fill,grow]"));
        // this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
        // getBackground().darker()));
        add(title = new JLabel(""), "split 3,gapleft 10,gapbottom 5,gaptop 5");
        title.setIcon(JDTheme.II("gui.images.sort", 24, 24));
        title.setIconTextGap(15);
        add(new JSeparator(), "growx,pushx,gapright 10");
        JButton bt;
        add(bt = new JButton(JDTheme.II("gui.images.bad", 16, 16)), "gapright 10");
        // bt.setContentAreaFilled(false);
        // bt.setBorder(null);
        // bt.setOpaque(false);
        // bt.setBorderPainted(false);
        bt.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setCollapsed(true);
            }

        });
        content = new JPanel();
        add(content);
        this.setVisible(true);
        // this.setCollapsed(true);
        this.addMouseListener(this);

    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {

        this.setCollapsed(true);

    }

    public void setCollapsed(boolean b) {
        this.setVisible(!b);

        if (b) setContentPanel(null);
    }

    public void setContentPanel(JTabbedPanel panel2) {
        if (panel2 == this.panel) return;

        if (this.panel != null) {
            this.panel.onHide();

        }
        content.removeAll();
        this.panel = panel2;
        if (panel == null) return;

        content.setLayout(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[fill,grow]"));

        panel.onDisplay();

        // JScrollPane sp;

        content.add(panel);

        // getContentPane().add(panel);
        setCollapsed(false);
        revalidate();
        content.revalidate();
    }

    public void setTitle(String l) {
        title.setText(l);

    }

    public void setIcon(ImageIcon ii) {
        title.setIcon(ii);

    }
}
