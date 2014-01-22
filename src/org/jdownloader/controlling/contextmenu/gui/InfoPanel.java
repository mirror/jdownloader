package org.jdownloader.controlling.contextmenu.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.GetterSetter;
import org.appwork.utils.KeyUtils;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class InfoPanel extends MigPanel implements ActionListener, Scrollable {
    public Dimension getPreferredScrollableViewportSize() {

        return this.getPreferredSize();
    }

    public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        return Math.max(visibleRect.height * 9 / 10, 1);
    }

    public boolean getScrollableTracksViewportHeight() {

        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        return Math.max(visibleRect.height / 10, 1);
    }

    private JLabel            label;

    private MenuItemData      item;
    private ExtTextField      name;
    private ExtButton         iconChange;

    private MenuManagerDialog managerFrame;

    private JCheckBox         visibleBox;

    private ExtTextField      shortcut;

    private CustomPanel       customPanel;

    private JLabel            namelabel;

    private JButton           iconKeyReset;

    private JButton           nameReset;

    private JButton           shortCutReset;

    private JLabel            shortcutLabel;

    public Dimension getPreferredSize() {
        Dimension ret = super.getPreferredSize();
        ret.width = Math.max(ret.width, 300);
        return ret;
        // return super.getPreferredSize();
    }

    public InfoPanel(MenuManagerDialog m) {
        super("ins 5,wrap 2", "[grow,fill][]", "[22!][]");
        this.managerFrame = m;
        label = SwingUtils.toBold(new JLabel());

        add(label);

        add(new JSeparator(), "spanx");
        add(SwingUtils.toBold(new JLabel(_GUI._.InfoPanel_InfoPanel_properties_())), "spanx");
        // MenuItemProperty.HIDE_IF_DISABLED;
        // MenuItemProperty.HIDE_IF_OPENFILE_IS_UNSUPPORTED;
        // MenuItemProperty.HIDE_IF_OUTPUT_NOT_EXISTING;

        visibleBox = new JCheckBox();
        visibleBox.addActionListener(this);
        name = new ExtTextField() {

            @Override
            public void onChanged() {
                item.setName(name.getText());

                updateResetButtons(item);
                updateHeaderLabel(item);
                managerFrame.fireUpdate();

            }

        };
        name.setHelpText(_GUI._.InfoPanel_InfoPanel_customname_help());
        iconChange = new ExtButton(new AppAction() {
            {
                setName(_GUI._.InfoPanel_changeicon());
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    final JPopupMenu p = new JPopupMenu();

                    URL url = NewTheme.I().getURL("images/", "help", ".png");

                    File imagesDir;

                    imagesDir = new File(url.toURI()).getParentFile();

                    String[] names = imagesDir.list(new FilenameFilter() {

                        public boolean accept(File dir, String name) {
                            return name.endsWith(".png");
                        }
                    });

                    final JList list = new JList(names);
                    list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
                    final ListCellRenderer org = list.getCellRenderer();
                    list.setCellRenderer(new ListCellRenderer() {

                        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                            String key = value.toString().substring(0, value.toString().length() - 4);
                            JLabel ret = (JLabel) org.getListCellRendererComponent(list, "", index, isSelected, cellHasFocus);
                            ret.setIcon(NewTheme.I().getIcon(key, 20));
                            return ret;
                        }
                    });
                    list.setFixedCellHeight(22);
                    list.setFixedCellWidth(22);
                    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

                        public void valueChanged(ListSelectionEvent e) {
                            String v = list.getSelectedValue().toString();
                            v = v.substring(0, v.length() - 4);
                            item.setIconKey(v);

                            updateInfo(item);
                            p.setVisible(false);
                            managerFrame.fireUpdate();
                        }
                    });
                    p.add(list);
                    p.show(iconChange, 0, iconChange.getHeight());
                } catch (URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });
        // icon=new JLabel(9)
        add(namelabel = label(_GUI._.InfoPanel_InfoPanel_itemname()));
        add(name, "newline");
        add(nameReset = new JButton(new AppAction() {
            {
                setIconKey("reset");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        // String newName = null;
                        // String oldName = item.getName();
                        // if (StringUtils.isNotEmpty(oldName)) {
                        // if (MenuItemData.isEmptyValue(oldName)) {
                        // newName = null;
                        // } else {
                        // newName = MenuItemData.EMPTY;
                        // }
                        // } else {
                        // newName = MenuItemData.EMPTY;
                        // }
                        name.setText(resetName);
                        item.setName(resetName);
                        item.clearCachedAction();
                        updateInfo(item);
                        managerFrame.fireUpdate();
                    }
                };
            }

        }), "width 22!,height 22!");

        add(iconChange, "newline");
        add(iconKeyReset = new JButton(new AppAction() {
            {
                setIconKey("reset");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        // String newIconKey = null;
                        // String oldIconKey = item.getIconKey();
                        // if (StringUtils.isNotEmpty(oldIconKey)) {
                        // if (MenuItemData.isEmptyValue(oldIconKey)) {
                        // newIconKey = null;
                        // } else {
                        // newIconKey = MenuItemData.EMPTY;
                        // }
                        // } else {
                        // newIconKey = MenuItemData.EMPTY;
                        // }
                        item.setIconKey(resetIconKey);
                        item.clearCachedAction();
                        updateInfo(item);
                        managerFrame.fireUpdate();
                    }
                };
            }

        }), "width 22!,height 22!");
        shortcut = new ExtTextField();
        shortcut.setHelpText(_GUI._.InfoPanel_InfoPanel_shortcuthelp2());
        shortcut.setEditable(false);
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
                item.setShortcut(currentShortcut == null ? null : currentShortcut.toString());

                // managerFrame.repaint();
                updateResetButtons(item);
            }

        });
        if (managerFrame.getManager().isAcceleratorsEnabled()) {
            add(shortcutLabel = label(_GUI._.InfoPanel_InfoPanel_shortcuts()), "hidemode 3");
            add(shortcut, "newline,hidemode 3");
            add(shortCutReset = new JButton(new AppAction() {
                {
                    setIconKey("reset");
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            // String newShortcut = null;
                            // String oldShortcut = item.getShortcut();
                            // if (StringUtils.isNotEmpty(oldShortcut)) {
                            // if (MenuItemData.isEmptyValue(oldShortcut)) {
                            // newShortcut = null;
                            // } else {
                            // newShortcut = MenuItemData.EMPTY;
                            // }
                            // } else {
                            // newShortcut = MenuItemData.EMPTY;
                            // }
                            item.setShortcut(resetShortcut);

                            updateInfo(item);
                            managerFrame.fireUpdate();
                        }
                    };
                }

            }), "width 22!,height 22!,hidemode 3");
        }

        add(label(_GUI._.InfoPanel_InfoPanel_hidden_2()));
        add(visibleBox, "spanx");

        add(new JSeparator(), "spanx");
        customPanel = new CustomPanel(managerFrame);
        add(customPanel, "spanx,growx,pushx");
    }

    private String resetIconKey;

    private String resetName;
    private String resetShortcut;

    private JLabel label(String infoPanel_InfoPanel_hideIfDisabled) {
        return new JLabel(infoPanel_InfoPanel_hideIfDisabled);
    }

    private class Entry {

        private MenuItemData  mid;
        private ActionContext so;
        private GetterSetter  gs;

        public Entry(MenuItemData mid, ActionContext so, GetterSetter gs) {
            this.mid = mid;
            this.so = so;
            this.gs = gs;
        }

    }

    public void updateResetButtons(final MenuItemData value) {
        if (value == null) {
            iconKeyReset.setEnabled(false);
            iconKeyReset.setToolTipText(null);
            nameReset.setEnabled(false);
            nameReset.setToolTipText(null);
            if (shortCutReset != null) {
                shortCutReset.setEnabled(false);
                shortCutReset.setToolTipText(null);
            }
        } else {
            try {

                if (value.getActionData() == null) return;
                ActionData actionData = value.getActionData();

                Class<?> clazz = actionData._getClazz();
                Constructor<?> c = clazz.getConstructor(new Class[] {});
                CustomizableAppAction ret = (CustomizableAppAction) c.newInstance(new Object[] {});
                ret.setMenuItemData(value);
                // do not apply to get the defaults
                // ret.applyMenuItemData();
                ret.initContextDefaults();
                ret.loadContextSetups();
                iconKeyReset.setEnabled(true);
                nameReset.setEnabled(true);
                shortCutReset.setEnabled(true);

                if (StringUtils.equals(name.getText(), ret.getName())) {
                    resetName = MenuItemData.EMPTY;
                } else {
                    resetName = ret.getName();
                }

                if (StringUtils.equals(value.getShortcut(), ret.getValue(Action.ACCELERATOR_KEY) + "")) {
                    resetShortcut = MenuItemData.EMPTY;
                } else {
                    resetShortcut = ret.getValue(Action.ACCELERATOR_KEY) + "";
                }

                if (StringUtils.equals(value.getIconKey(), ret.getIconKey())) {
                    resetIconKey = MenuItemData.EMPTY;
                } else {
                    resetIconKey = ret.getIconKey();
                }

                iconKeyReset.setToolTipText(_GUI._.ManagerFrame_layoutPanel_resettodefault_parametered(resetIconKey));

                nameReset.setToolTipText(_GUI._.ManagerFrame_layoutPanel_resettodefault_parametered(resetName));
                shortCutReset.setToolTipText(_GUI._.ManagerFrame_layoutPanel_resettodefault_parametered(resetShortcut));

            } catch (Throwable e) {
                LogController.CL().log(e);
            }
        }
    }

    /**
     * @param lastPathComponent
     */
    public void updateInfo(final MenuItemData value) {
        // getParent().revalidate();
        // Component.revalidate is 1.7 only - that's why we have to cast
        JComponent p = (JComponent) getParent().getParent().getParent();
        updateResetButtons(value);
        p.revalidate();
        this.item = value;
        if (value == null) {
            label.setText("");
            return;
        }
        visibleBox.setSelected(value.isVisible());

        MenuItemData mid = ((MenuItemData) value);
        Rectangle bounds = null;

        String n = mid.getName();

        name.setText(n);

        // renderer.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED));
        updateHeaderLabel(mid);
        customPanel.removeAll();

        CustomizableAppAction action;
        try {
            if (mid.getActionData() != null) {
                shortcutLabel.setVisible(!(mid instanceof MenuLink));
                shortcut.setVisible(!(mid instanceof MenuLink));
                shortCutReset.setVisible(!(mid instanceof MenuLink));
                action = mid.createAction();
                name.setText(action.getName());
                if (StringUtils.isEmpty(action.getName())) {
                    name.setText(MenuItemData.EMPTY);
                }
                if (MenuItemData.isEmptyValue(mid.getShortcut())) {
                    action.setAccelerator(null);
                }
                KeyStroke ks = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
                if (ks != null) {

                    shortcut.setText(KeyUtils.getShortcutString(ks, true));

                } else {

                    shortcut.setText("");
                }
                List<ActionContext> sos = action.getSetupObjects();
                if (sos != null) {
                    ArrayList<Entry> lst = new ArrayList<Entry>();
                    for (ActionContext so : sos) {

                        ArrayList<GetterSetter> gss = new ArrayList<GetterSetter>(ReflectionUtils.getGettersSetteres(so.getClass()));
                        for (GetterSetter gs : gss) {

                            if (gs.hasGetter() && gs.hasSetter()) {
                                if (gs.hasAnnotation(Customizer.class)) {
                                    lst.add(new Entry(mid, so, gs));

                                }
                            }
                        }
                    }
                    Collections.sort(lst, new Comparator<Entry>() {

                        @Override
                        public int compare(Entry o1, Entry o2) {
                            try {
                                String lbl1 = o1.gs.getKey();
                                String lbl2 = o2.gs.getKey();
                                Customizer oc1 = o1.gs.getAnnotation(Customizer.class);
                                Customizer oc2 = o2.gs.getAnnotation(Customizer.class);
                                if (oc1 != null) lbl1 = oc1.name();
                                if (oc2 != null) lbl2 = oc2.name();

                                return lbl1.compareToIgnoreCase(lbl2);
                            } catch (Throwable e) {
                                e.printStackTrace();
                                return 0;
                            }
                        }
                    });
                    for (Entry e : lst) {
                        customPanel.add(e.mid.getActionData(), action, e.so, e.gs);
                    }
                }
            } else {
                shortcut.setText("");

                shortcutLabel.setVisible(false);
                shortcut.setVisible(false);
                shortCutReset.setVisible(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        revalidate();

    }

    public void updateHeaderLabel(MenuItemData mid) {

        String type = null;
        String name = mid.getName();
        Icon icon = null;
        if (mid.getIconKey() != null) {
            icon = (MenuItemData.getIcon(mid.getIconKey(), 20));
        }

        if (mid instanceof MenuContainer) {

            type = _GUI._.InfoPanel_update_submenu();

            // label.setText(_GUI._.InfoPanel_updateInfo_header_actionlabel(, ));

        } else if (mid instanceof SeperatorData) {

            name = _GUI._.Renderer_getTreeCellRendererComponent_seperator();

        } else {
            if (mid instanceof MenuLink) {
                type = _GUI._.InfoPanel_update_link();

            } else {
                if (mid._isValidated()) {
                    try {
                        AppAction action = mid.createAction();

                        if (StringUtils.isEmpty(name)) {
                            name = action.getName();
                        }
                        type = _GUI._.InfoPanel_update_action();
                        if (icon == null) {
                            icon = action.getSmallIcon();
                        }
                    } catch (Exception e) {

                    }
                }
                if (StringUtils.isEmpty(name)) {
                    name = mid.getActionData().getName();
                }
                if (icon == null) {
                    if (mid.getActionData().getIconKey() != null) {
                        icon = NewTheme.I().getIcon(mid.getActionData().getIconKey(), 18);
                    }
                }
                if (StringUtils.isEmpty(name)) {

                    name = mid.getActionData().getClazzName();
                    name = name.substring(name.lastIndexOf(".") + 1);

                }
                if (MenuItemData.isEmptyValue(mid.getIconKey())) {
                    icon = null;
                }
                if (MenuItemData.isEmptyValue(name)) {
                    name = mid.getActionData().getClazzName();
                    name = name.substring(name.lastIndexOf(".") + 1);
                    name += "(" + MenuItemData.EMPTY + ")";
                }

            }

        }
        if (StringUtils.isNotEmpty(type)) {
            label.setText(_GUI._.InfoPanel_updateInfo_header_actionlabel(name, type));
        } else {
            label.setText(name);
        }
        label.setIcon(icon);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        item.setVisible(visibleBox.isSelected());

        managerFrame.repaint();

    }

}
