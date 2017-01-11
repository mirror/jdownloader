package org.jdownloader.controlling.contextmenu.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.FileHandler;
import org.appwork.utils.Files;
import org.appwork.utils.GetterSetter;
import org.appwork.utils.KeyUtils;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.CustomSettingsPanelInterface;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.controlling.contextmenu.SeparatorData;
import org.jdownloader.gui.IconKey;
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
        add(SwingUtils.toBold(new JLabel(_GUI.T.InfoPanel_InfoPanel_properties_())), "spanx");
        // MenuItemProperty.HIDE_IF_DISABLED;
        // MenuItemProperty.HIDE_IF_OPENFILE_IS_UNSUPPORTED;
        // MenuItemProperty.HIDE_IF_OUTPUT_NOT_EXISTING;

        visibleBox = new JCheckBox();
        visibleBox.addActionListener(this);
        name = new ExtTextField() {

            @Override
            public void onChanged() {
                if (item == null) {
                    return;
                }
                item.setName(name.getText());

                updateResetButtons(item);
                updateHeaderLabel(item);
                managerFrame.fireUpdate();

            }

        };
        name.setHelpText(_GUI.T.InfoPanel_InfoPanel_customname_help());
        iconChange = new ExtButton(new AppAction() {
            {
                setName(_GUI.T.InfoPanel_changeicon());
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                final JPopupMenu p = new JPopupMenu();

                final File imagesDir = NewTheme.I().getImagesDirectory();

                final ArrayList<File> files = new ArrayList<File>();
                Files.internalWalkThroughStructure(new FileHandler<RuntimeException>() {

                    @Override
                    public void intro(File f) throws RuntimeException {
                    }

                    @Override
                    public boolean onFile(File f, int depths) throws RuntimeException {
                        final String name = f.getName().toLowerCase(Locale.ENGLISH);
                        if ("fav".equals(name) && f.isDirectory()) {
                            return false;
                        }
                        if (name.endsWith(".png") || name.endsWith(".svg")) {
                            files.add(f);
                        }
                        return true;
                    }

                    @Override
                    public void outro(File f) throws RuntimeException {
                    }
                }, imagesDir, 5);
                final JList list = new JList(files.toArray(new File[] {}));
                list.setLayoutOrientation(JList.VERTICAL_WRAP);
                list.setVisibleRowCount(30);
                final ListCellRenderer org = list.getCellRenderer();
                list.setCellRenderer(new ListCellRenderer() {

                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        File f = (File) value;
                        // String key = value.toString().substring(0, value.toString().length() - 4);
                        JLabel ret = (JLabel) org.getListCellRendererComponent(list, "", index, isSelected, cellHasFocus);
                        try {
                            ret.setIcon(IconIO.getImageIcon(f.toURI().toURL(), 20));
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        return ret;
                    }
                });
                list.setFixedCellHeight(24);
                list.setFixedCellWidth(24);
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

                    public void valueChanged(ListSelectionEvent e) {
                        File v = (File) list.getSelectedValue();
                        String rel = Files.getRelativePath(imagesDir, v);
                        rel = rel.substring(0, rel.length() - 4);
                        item.setIconKey(rel);

                        updateInfo(item);
                        p.setVisible(false);
                        managerFrame.fireUpdate();
                    }
                });

                // list.setMinimumSize(new Dimension(64, 64));
                p.setLayout(new MigLayout("ins 5", "[grow,fill]", "[grow,fill]"));
                p.add(list, "width 32:n:n");
                p.show(iconChange, 0, iconChange.getHeight());

            }
        });
        // icon=new JLabel(9)
        add(namelabel = label(_GUI.T.InfoPanel_InfoPanel_itemname()));
        add(name, "newline");
        add(nameReset = new JButton(new AppAction() {
            {
                setIconKey(IconKey.ICON_RESET);
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
                setIconKey(IconKey.ICON_RESET);
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
        shortcut.setHelpText(_GUI.T.InfoPanel_InfoPanel_shortcuthelp2());
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
            add(shortcutLabel = label(_GUI.T.InfoPanel_InfoPanel_shortcuts()), "hidemode 3");
            add(shortcut, "newline,hidemode 3");
            add(shortCutReset = new JButton(new AppAction() {
                {
                    setIconKey(IconKey.ICON_RESET);
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            item.setShortcut(resetShortcut);
                            updateInfo(item);
                            managerFrame.fireUpdate();
                        }
                    };
                }

            }), "width 22!,height 22!,hidemode 3");
        }

        add(label(_GUI.T.InfoPanel_InfoPanel_hidden_2()));
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

                if (value.getActionData() == null || !value.getActionData()._isValidDataForCreatingAnAction() || (value instanceof MenuLink)) {
                    return;
                }
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
                if (shortCutReset != null) {
                    shortCutReset.setEnabled(true);
                }

                if (StringUtils.equals(name.getText(), ret.getName())) {
                    resetName = MenuItemData.EMPTY;
                } else {
                    resetName = ret.getName();
                }

                if (StringUtils.equals(value.getIconKey(), ret.getIconKey())) {
                    resetIconKey = MenuItemData.EMPTY;
                } else {
                    resetIconKey = ret.getIconKey();
                }
                iconKeyReset.setToolTipText(_GUI.T.ManagerFrame_layoutPanel_resettodefault_parametered(resetIconKey));
                nameReset.setToolTipText(_GUI.T.ManagerFrame_layoutPanel_resettodefault_parametered(resetName));

                if (shortCutReset != null) {
                    if (StringUtils.equals(value.getShortcut(), ret.getValue(Action.ACCELERATOR_KEY) + "")) {
                        resetShortcut = MenuItemData.EMPTY;
                    } else {
                        resetShortcut = ret.getValue(Action.ACCELERATOR_KEY) + "";
                    }
                    shortCutReset.setToolTipText(_GUI.T.ManagerFrame_layoutPanel_resettodefault_parametered(resetShortcut));
                }

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

        MenuItemData mid = (value);
        Rectangle bounds = null;

        String n = mid.getName();

        name.setText(n);

        // renderer.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED));
        updateHeaderLabel(mid);
        customPanel.removeAll();

        CustomizableAppAction action = null;
        try {
            if (mid.getActionData() != null && mid.getActionData()._isValidDataForCreatingAnAction() && !(mid instanceof MenuLink)) {
                if (shortcutLabel != null) {
                    shortcutLabel.setVisible(true);
                }
                if (shortcut != null) {
                    shortcut.setVisible(true);
                }
                if (shortCutReset != null) {
                    shortCutReset.setVisible(true);
                }

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
                                if (oc1 != null) {

                                    lbl1 = CustomPanel.getNameForCustomizer(o1.gs);
                                }
                                if (oc2 != null) {
                                    lbl2 = CustomPanel.getNameForCustomizer(o2.gs);
                                }

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
                if (shortCutReset != null) {
                    shortCutReset.setVisible(false);
                }
            }
            if (mid instanceof CustomSettingsPanelInterface) {
                JComponent panel = ((CustomSettingsPanelInterface) mid).createSettingsPanel();
                if (panel != null) {
                    customPanel.add(panel, "pushx,growx");
                }
            } else if (action != null && action instanceof CustomSettingsPanelInterface) {
                JComponent panel = ((CustomSettingsPanelInterface) action).createSettingsPanel();
                if (panel != null) {
                    customPanel.add(panel, "pushx,growx");
                }
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

            type = _GUI.T.InfoPanel_update_submenu();

            // label.setText(_GUI.T.InfoPanel_updateInfo_header_actionlabel(, ));

        } else if (mid instanceof SeparatorData) {

            name = _GUI.T.Renderer_getTreeCellRendererComponent_separator();

        } else {
            if (mid instanceof MenuLink) {
                type = _GUI.T.InfoPanel_update_link();

            } else {
                if (mid._isValidated()) {
                    try {
                        AppAction action = mid.createAction();

                        if (StringUtils.isEmpty(name)) {
                            name = action.getName();
                        }
                        type = _GUI.T.InfoPanel_update_action();
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
            label.setText(_GUI.T.InfoPanel_updateInfo_header_actionlabel(name, type));
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
