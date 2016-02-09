package org.jdownloader.gui.views.downloads.bottombar;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtSpinner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class HorizontalBoxItem extends MenuItemData implements MenuLink, SelfLayoutInterface {
    public HorizontalBoxItem() {
        super();
        setName(_GUI.T.HorizontalBoxItem_HorizontalBoxItem());
        setVisible(false);
        setIconKey(IconKey.ICON_RIGHT);
    }

    @Override
    public List<AppAction> createActionsToLink() {
        return null;
    }

    @Override
    public JComponent createSettingsPanel() {

        ActionData ad = getActionData();

        final ActionData actionData = ad;
        MigPanel p = new MigPanel("ins 0,wrap 2", "[grow,fill][]", "[]");
        SwingUtils.setOpaque(p, false);
        p.add(new JLabel(_GUI.T.MenuEditors_boxwidth_min()));
        int width = getMinWidth();

        final ExtSpinner minSpin = new ExtSpinner(new SpinnerNumberModel(width, -1, 10000, 1));
        minSpin.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                actionData.putSetup("minWidth", ((Number) minSpin.getValue()).intValue());
            }
        });
        p.add(minSpin);
        //

        p.add(new JLabel(_GUI.T.MenuEditors_boxwidth_pref()));
        width = getPrefWidth();

        final ExtSpinner prefSpin = new ExtSpinner(new SpinnerNumberModel(width, 0, 10000, 1));
        prefSpin.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                actionData.putSetup("prefWidth", ((Number) prefSpin.getValue()).intValue());
            }
        });
        p.add(prefSpin);
        //
        p.add(new JLabel(_GUI.T.MenuEditors_boxwidth_max()));
        width = getMaxWidth();

        final ExtSpinner maxSpin = new ExtSpinner(new SpinnerNumberModel(width, 0, 10000, 1));
        maxSpin.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                actionData.putSetup("maxWidth", ((Number) maxSpin.getValue()).intValue());
            }
        });
        p.add(maxSpin);
        return p;
    }

    protected int getPrefWidth() {
        int width = 0;

        try {
            width = ((Number) getActionData().fetchSetup("prefWidth")).intValue();
        } catch (Throwable e) {
        }
        return width;
    }

    protected int getMaxWidth() {
        int width = 10000;

        try {
            width = ((Number) getActionData().fetchSetup("maxWidth")).intValue();
        } catch (Throwable e) {
        }
        return width;
    }

    protected int getMinWidth() {
        int width = 0;

        try {
            width = ((Number) getActionData().fetchSetup("minWidth")).intValue();
        } catch (Throwable e) {
        }
        return width;
    }

    public JComponent createItem() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        MigPanel ret = new MigPanel("ins 0", "[]", "[]");
        ret.setBackground(Color.RED);
        ret.setOpaque(false);
        return ret;
    }

    @Override
    public String createConstraints() {
        return "height 24!,aligny top,gapleft 2,pushx,growx,width " + getMinWidth() + ":" + getPrefWidth() + ":" + getMaxWidth();
    }

}
