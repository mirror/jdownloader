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
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

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

    protected KeyStroke       currentShortcut;
    private CustomPanel       customPanel;

    private JLabel            namelabel;

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
                item.clearCachedAction();
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
        add(new JButton(new AppAction() {
            {
                setIconKey("reset");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        if (MenuItemData.EMPTY_NAME.equals(name.getText())) {
                            item.setName("");
                            item.clearCachedAction();
                            updateInfo(item);
                            managerFrame.fireUpdate();
                        } else {
                            item.setName(MenuItemData.EMPTY_NAME);
                            item.clearCachedAction();
                            updateInfo(item);
                            managerFrame.fireUpdate();
                        }

                    }
                };
            }

        }), "width 22!,height 22!");

        add(iconChange, "newline");
        add(new JButton(new AppAction() {
            {
                setIconKey("reset");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        if (StringUtils.isNotEmpty(item.getIconKey())) {

                            item.setIconKey(null);
                            item.clearCachedAction();
                            updateInfo(item);
                            managerFrame.fireUpdate();

                        } else {
                            item.setIconKey(MenuItemData.EMPTY_NAME);
                            item.clearCachedAction();
                            updateInfo(item);
                            managerFrame.fireUpdate();
                        }

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
                currentShortcut = KeyStroke.getKeyStroke(event.getKeyCode(), event.getModifiersEx());
                System.out.println(":::" + event + " - " + currentShortcut + " - " + currentShortcut.getModifiers());
                shortcut.setText(msg1);
                save();
            }

        });
        if (managerFrame.getManager().isAcceleratorsEnabled()) {
            add(label(_GUI._.InfoPanel_InfoPanel_shortcuts()));
            add(shortcut, "newline");
            add(new JButton(new AppAction() {
                {
                    setIconKey("reset");
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            if (StringUtils.isNotEmpty(item.getShortcut())) {

                                item.setShortcut(null);
                                item.clearCachedAction();
                                updateInfo(item);
                                managerFrame.fireUpdate();

                            } else {
                                item.setShortcut(MenuItemData.EMPTY_NAME);
                                item.clearCachedAction();
                                updateInfo(item);
                                managerFrame.fireUpdate();
                            }

                        }
                    };
                }

            }), "width 22!,height 22!");
        }

        add(label(_GUI._.InfoPanel_InfoPanel_hidden_2()));
        add(visibleBox, "spanx");

        add(new JSeparator(), "spanx");
        customPanel = new CustomPanel(managerFrame);
        add(customPanel, "spanx,growx,pushx");
    }

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

    /**
     * @param lastPathComponent
     */
    public void updateInfo(final MenuItemData value) {
        // getParent().revalidate();
        // Component.revalidate is 1.7 only - that's why we have to cast
        JComponent p = (JComponent) getParent().getParent().getParent();

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
                action = mid.createAction();
                name.setText(action.getName());
                if (StringUtils.isEmpty(action.getName())) {
                    name.setText(MenuItemData.EMPTY_NAME);
                }
                if (MenuItemData.EMPTY_NAME.equals(mid.getShortcut())) {
                    action.setAccelerator(null);
                }
                KeyStroke ks = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
                if (ks != null) {
                    currentShortcut = ks;
                    if (currentShortcut != null) {
                        shortcut.setText(KeyUtils.getShortcutString(currentShortcut, true));
                    } else {
                        shortcut.setText("");
                    }
                } else {
                    currentShortcut = null;
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
                if (MenuItemData.EMPTY_NAME.equals(mid.getIconKey())) {
                    icon = null;
                }
                if (name.equals(MenuItemData.EMPTY_NAME)) {
                    name = mid.getActionData().getClazzName();
                    name = name.substring(name.lastIndexOf(".") + 1);
                    name += "(" + MenuItemData.EMPTY_NAME + ")";
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
        save();

    }

    private void save() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                item.setVisible(visibleBox.isSelected());
                item.setShortcut(currentShortcut == null ? null : currentShortcut.toString());
                managerFrame.repaint();
            }
        }.waitForEDT();

    }
}
