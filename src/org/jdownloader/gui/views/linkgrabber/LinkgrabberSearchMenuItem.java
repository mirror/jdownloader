package org.jdownloader.gui.views.linkgrabber;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtSpinner;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.KeyUtils;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.bottombar.SelfLayoutInterface;

public class LinkgrabberSearchMenuItem extends MenuItemData implements MenuLink, SelfLayoutInterface {
    private static final String SHORTCUT2 = "shortcut";

    public LinkgrabberSearchMenuItem() {
        super();
        setName(_GUI.T.FilterMenuItem_FilterMenuItem());
        setIconKey(IconKey.ICON_SEARCH);
        setVisible(true);
        //
    }

    @Override
    public List<AppAction> createActionsToLink() {
        ArrayList<AppAction> ret = new ArrayList<AppAction>();
        LinkgrabberSearchField item = LinkgrabberSearchField.getInstance();
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        try {

            ActionData ad = this.getActionData();
            Object sc = ad.fetchSetup(SHORTCUT2);
            if (sc != null && sc instanceof String) {

                ks = KeyStroke.getKeyStroke((String) sc);
            }

        } catch (Throwable e) {
        }
        AppAction a = item.getFocusAction();
        a.setAccelerator(ks);
        ret.add(a);

        return ret;
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

    @Override
    public JComponent createSettingsPanel() {

        ActionData ad = this.getActionData();
        if (ad == null) {
            ad = new ActionData();
            setActionData(ad);
        }

        final ActionData actionData = ad;
        MigPanel p = new MigPanel("ins 0,wrap 2", "[grow,fill][100:n:n,fill]", "[]");
        SwingUtils.setOpaque(p, false);
        final ExtTextField shortcut = new ExtTextField();
        shortcut.setHelpText(_GUI.T.InfoPanel_InfoPanel_shortcuthelp2());
        shortcut.setEditable(false);
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        try {
            Object sc = ad.fetchSetup(SHORTCUT2);
            if (sc != null && sc instanceof String) {

                ks = KeyStroke.getKeyStroke((String) sc);
            }
        } catch (Throwable e) {
        }
        String msg1 = KeyUtils.getShortcutString(ks, true);

        shortcut.setText(msg1);
        shortcut.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent event) {
                String msg1 = KeyUtils.getShortcutString(event, true);
                KeyStroke currentShortcut = KeyStroke.getKeyStroke(event.getKeyCode(), event.getModifiersEx());
                shortcut.setText(msg1);
                actionData.putSetup(SHORTCUT2, currentShortcut == null ? null : currentShortcut.toString());

            }

        });

        p.add(new JLabel(_GUI.T.InfoPanel_InfoPanel_shortcuts()));
        p.add(shortcut, "split 2");
        JButton shortCutReset;
        p.add(shortCutReset = new JButton(new AppAction() {
            {
                setIconKey(IconKey.ICON_RESET);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
                        String msg1 = KeyUtils.getShortcutString(ks, true);
                        shortcut.setText(msg1);
                        actionData.putSetup(SHORTCUT2, null);
                        // managerFrame.fireUpdate();
                    }
                };
            }

        }), "width 22!,height 22!");

        p.add(new JLabel(_GUI.T.MenuEditors_boxwidth_min()), "newline");
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
        int width = 300;

        try {
            width = ((Number) getActionData().fetchSetup("prefWidth")).intValue();
        } catch (Throwable e) {
        }
        return width;
    }

    @Override
    public String createConstraints() {
        return "height 24!,aligny top,pushx,growx,width " + getMinWidth() + ":" + getPrefWidth() + ":" + getMaxWidth();
    }
    // @Override
    // public String createConstraints() {
    // return "height 24!,aligny top,gapleft 2,pushx,growx";
    // }

    public JComponent createItem() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        LinkgrabberSearchField ret = LinkgrabberSearchField.getInstance();

        return ret;
    }
}