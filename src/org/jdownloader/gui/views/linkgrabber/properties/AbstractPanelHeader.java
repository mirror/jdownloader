package org.jdownloader.gui.views.linkgrabber.properties;

import java.awt.event.ActionEvent;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.overviewpanel.CloseButton;
import org.jdownloader.gui.views.downloads.overviewpanel.SettingsButton;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

import sun.swing.SwingUtilities2;

public abstract class AbstractPanelHeader extends MigPanel {
    private JButton   bt;
    private ExtButton options;
    private JLabel    lbl;
    private JLabel    icon;
    private String    labelString;

    private void updateLabelString() {
        if (labelString != null) {
            ;
            lbl.setText("");
            lbl.setText(SwingUtilities2.clipStringIfNecessary(lbl, lbl.getFontMetrics(getFont()), labelString, lbl.getWidth() - 15));
        }
    }

    protected void setIcon(ImageIcon icon) {
        this.icon.setIcon(icon);
    }

    protected void setText(String str) {
        labelString = str;
        updateLabelString();
    }

    public AbstractPanelHeader(String title, ImageIcon imageIcon) {
        super("ins 0 0 1 0", "[]2[grow,fill][]0[]0", "[grow,fill]");

        lbl = SwingUtils.toBold(new JLabel(""));
        this.addHierarchyBoundsListener(new HierarchyBoundsListener() {

            @Override
            public void ancestorResized(HierarchyEvent e) {
                updateLabelString();
            }

            @Override
            public void ancestorMoved(HierarchyEvent e) {
            }
        });
        LAFOptions.getInstance().applyHeaderColorBackground(lbl);
        add(icon = new JLabel(NewTheme.I().getIcon("download", 16)), "gapleft 1");
        add(lbl, "height 17!");

        options = new SettingsButton(new AppAction() {
            {
                //

                setTooltipText(_GUI._.AbstractPanelHeader_AbstractPanelHeader_settings_tt());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                onSettings(options);
            }
        });

        setOpaque(true);
        SwingUtils.setOpaque(lbl, false);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, (LAFOptions.getInstance().getColorForPanelHeaderLine())));

        setBackground((LAFOptions.getInstance().getColorForPanelHeaderBackground()));
        bt = new CloseButton(new AppAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                onCloseAction();
            }

        });

        add(options, "height 17!,width 24!");
        add(bt, "width 17!,height 17!");
        setText(title);
        setIcon(imageIcon);
    }

    abstract protected void onSettings(ExtButton options);

    abstract protected void onCloseAction();
}
