package jd.gui.skins.simple;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import jd.utils.JDLocale;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class JDCollapser extends JPanel {

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

        add(title = new JLabel(""), "split 3,gapleft 5,gapbottom 0,gaptop 0");
        title.setIcon(JDTheme.II("gui.images.sort", 24, 24));
        title.setIconTextGap(15);
        add(new JSeparator(), "growx,pushx,gapright 5");
        JButton bt;
        add(bt = new JButton(JDTheme.II("gui.images.close", 16, 16)), "gapright 10");
        bt.setContentAreaFilled(false);
        bt.setBorder(null);
        bt.setOpaque(false);
        bt.setBorderPainted(false);
        bt.setToolTipText(JDLocale.L("gui.tooltips.infocollapser", "Click to close and save"));
        bt.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setCollapsed(true);
            }

        });
        content = new JPanel();
        add(content);
        this.setVisible(true);

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

        content.add(panel);
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
