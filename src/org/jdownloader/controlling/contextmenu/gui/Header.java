package org.jdownloader.controlling.contextmenu.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import jd.gui.swing.laf.LAFOptions;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;

public class Header extends MigPanel {

    public Header(String layoutManager, ImageIcon icon) {
        super("ins 0 0 1 0", "[]2[][][grow,fill][]0", "[grow,fill]");

        // setBackground(Color.RED);
        // setOpaque(true);

        JLabel lbl = SwingUtils.toBold(new JLabel(layoutManager));
        LookAndFeelController.getInstance().getLAFOptions().applyDownloadOverviewHeaderColor(lbl);
        add(new JLabel(icon), "gapleft 1");
        add(lbl, "height 17!");

        add(Box.createHorizontalGlue());
        setOpaque(true);
        SwingUtils.setOpaque(lbl, false);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LAFOptions.createColor(LookAndFeelController.getInstance().getLAFOptions().getColorForPanelHeaderLine())));

        setBackground(LAFOptions.createColor(LookAndFeelController.getInstance().getLAFOptions().getColorForPanelHeaderBackground()));

    }
}
