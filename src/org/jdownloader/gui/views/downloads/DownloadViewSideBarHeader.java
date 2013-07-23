package org.jdownloader.gui.views.downloads;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;


import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class DownloadViewSideBarHeader extends MigPanel {

    public DownloadViewSideBarHeader() {
        super("ins 0 0 1 0", "[][grow,fill]", "[grow,fill]");

        // setBackground(Color.RED);
        // setOpaque(true);
        JLabel lbl = new JLabel(_GUI._.LinkGrabberSideBarHeader_LinkGrabberSideBarHeader());

        add(lbl, "height 18!");
        add(Box.createHorizontalGlue());
        setOpaque(true);
        SwingUtils.setOpaque(lbl, false);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, (LAFOptions.getInstance().getColorForPanelHeaderLine())));

        setBackground((LAFOptions.getInstance().getColorForPanelHeaderBackground()));

        // add(bt, "width 20!,height 20!");
    }

}
