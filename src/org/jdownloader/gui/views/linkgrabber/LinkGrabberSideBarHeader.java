package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class LinkGrabberSideBarHeader extends MigPanel {

    private JButton bt;

    public LinkGrabberSideBarHeader(LinkGrabberTable table) {
        super("ins 0 0 1 0,debug", "[][grow,fill][]", "[grow,fill]");

        // setBackground(Color.RED);
        // setOpaque(true);
        JLabel lbl = new JLabel(_GUI._.LinkGrabberSideBarHeader_LinkGrabberSideBarHeader());

        add(lbl, "height 17!,gapleft 10");
        add(Box.createHorizontalGlue());
        setOpaque(true);
        SwingUtils.setOpaque(lbl, false);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderLineColor())));

        setBackground(new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderColor()));

        bt = new JButton(NewTheme.I().getIcon("close", -1)) {

            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x + 4, y, width, height);
            }

        };
        bt.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
                bt.setIcon(NewTheme.I().getIcon("close", -1));
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                bt.setIcon(NewTheme.I().getIcon("close.on", -1));

            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }
        });
        bt.setBorderPainted(false);
        bt.setContentAreaFilled(false);
        bt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE.setValue(false);
            }
        });
        add(bt, "width 17!,height 17!");
    }

}
