package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;

import org.appwork.app.gui.MigPanel;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class CustomFilterHeader extends MigPanel {

    private ExtCheckBox       checkBox;
    private JLabel            lbl;
    private BooleanKeyHandler keyHandler;
    private JLabel            counter;
    private ExtButton         config;

    public CustomFilterHeader() {
        super("ins 0", "2[][grow,fill][]8[]4", "[]");
        setOpaque(false);
        setBackground(null);
        // add(new JSeparator(), "gapleft 15");

        config = new ExtButton(new BasicAction() {
            {
                setSmallIcon(NewTheme.I().getIcon("exttable/columnButton", 14));
            }

            public void actionPerformed(ActionEvent e) {
                System.out.println("Go to LinkFIlter");
            }
        });
        config.setRolloverEffectEnabled(true);
        add(config, "height 16!,width 16!");
        // LinkFilterSettings.LG_QUICKFILTER_EXCEPTIONS_VISIBLE
        lbl = SwingUtils.toBold(new JLabel(_GUI._.LinkGrabberSidebar_LinkGrabberSidebar_exceptionfilter()));
        counter = SwingUtils.toBold(new JLabel(""));
        // lbl.setForeground(new
        // Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderLineColor()));
        add(counter);
        add(lbl);
        keyHandler = LinkFilterSettings.LG_QUICKFILTER_EXCEPTIONS_VISIBLE;
        checkBox = new ExtCheckBox(LinkFilterSettings.LG_QUICKFILTER_EXCEPTIONS_VISIBLE, lbl, counter);
        add(checkBox);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new JTable().getGridColor()));

    }

    public BooleanKeyHandler getKeyHandler() {
        return keyHandler;
    }

    public JCheckBox getCheckBox() {
        return checkBox;
    }

    public void setFilterCount(final int size) {

    }

}
