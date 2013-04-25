package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.appwork.exceptions.WTFException;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.contextmenumanager.ActionClassNotAvailableException;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainer;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemProperty;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuLink;
import org.jdownloader.gui.views.downloads.contextmenumanager.SeparatorData;
import org.jdownloader.images.NewTheme;

public class InfoPanel extends MigPanel implements ActionListener {

    private JLabel       label;

    private JCheckBox    hideIfDisabled;
    private JCheckBox    hideIfOpenFileIsUnsupported;
    private JCheckBox    hideIfOutputNotExists;
    private JCheckBox    linkContext;
    private JCheckBox    packageContext;
    private MenuItemData item;
    private ExtTextField name;
    private ExtButton    iconChange;

    private JLabel       iconlabel;

    private ManagerFrame managerFrame;

    public InfoPanel(ManagerFrame m) {
        super("ins 5,wrap 2", "[grow,fill][]", "[22!][]");
        this.managerFrame = m;
        label = SwingUtils.toBold(new JLabel());

        add(label);

        add(new JSeparator(), "spanx");
        add(SwingUtils.toBold(new JLabel(_GUI._.InfoPanel_InfoPanel_properties_())), "spanx");
        // MenuItemProperty.HIDE_IF_DISABLED;
        // MenuItemProperty.HIDE_IF_OPENFILE_IS_UNSUPPORTED;
        // MenuItemProperty.HIDE_IF_OUTPUT_NOT_EXISTING;
        hideIfDisabled = new JCheckBox();
        hideIfDisabled.addActionListener(this);
        hideIfOpenFileIsUnsupported = new JCheckBox();
        hideIfOpenFileIsUnsupported.addActionListener(this);
        hideIfOutputNotExists = new JCheckBox();
        hideIfOutputNotExists.addActionListener(this);
        linkContext = new JCheckBox();
        linkContext.addActionListener(this);
        packageContext = new JCheckBox();
        packageContext.addActionListener(this);

        name = new ExtTextField() {

            @Override
            public void onChanged() {
                item.setName(name.getText());

                try {
                    updateHeaderLabel(item.lazyReal());
                    managerFrame.fireUpdate();
                } catch (ActionClassNotAvailableException e) {
                    e.printStackTrace();
                }

            }

        };
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
        add(label(_GUI._.InfoPanel_InfoPanel_itemname_()));
        add(name, "newline,spanx");
        add(iconlabel = label(_GUI._.InfoPanel_InfoPanel_icon()), "height 22!");
        add(iconChange, "newline,spanx");
        add(label(_GUI._.InfoPanel_InfoPanel_hideIfDisabled()));
        add(hideIfDisabled, "spanx");
        add(label(_GUI._.InfoPanel_InfoPanel_hideIfOpenFileIsUnsupported()));
        add(hideIfOpenFileIsUnsupported, "spanx");
        add(label(_GUI._.InfoPanel_InfoPanel_hideIfFileNotExists()));
        add(hideIfOutputNotExists, "spanx");
        add(label(_GUI._.InfoPanel_InfoPanel_linkContext2()));
        add(linkContext, "spanx");
        add(label(_GUI._.InfoPanel_InfoPanel_packageContext2()));
        add(packageContext, "spanx");
    }

    private JLabel label(String infoPanel_InfoPanel_hideIfDisabled) {
        return new JLabel(infoPanel_InfoPanel_hideIfDisabled);
    }

    /**
     * @param lastPathComponent
     */
    public void updateInfo(final MenuItemData value) {
        try {
            this.item = value;
            if (value == null) {

                label.setText("");
                return;
            }
            MenuItemData mid = ((MenuItemData) value).lazyReal();
            Rectangle bounds = null;
            if (mid.getIconKey() != null) {

                iconlabel.setIcon(NewTheme.I().getIcon(mid.getIconKey(), 22));
            } else {
                iconlabel.setIcon(null);
            }

            name.setText(mid.getName());
            link(mid, hideIfDisabled, MenuItemProperty.HIDE_IF_DISABLED);
            link(mid, hideIfOpenFileIsUnsupported, MenuItemProperty.HIDE_IF_OPENFILE_IS_UNSUPPORTED);

            link(mid, hideIfOutputNotExists, MenuItemProperty.HIDE_IF_OUTPUT_NOT_EXISTING);

            link(mid, linkContext, MenuItemProperty.LINK_CONTEXT);

            link(mid, packageContext, MenuItemProperty.PACKAGE_CONTEXT);

            // renderer.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED));
            updateHeaderLabel(mid);

        } catch (ActionClassNotAvailableException e) {
            throw new WTFException(e);
        }

    }

    public void updateHeaderLabel(MenuItemData mid) throws ActionClassNotAvailableException {

        String type = null;
        String name = mid.getName();
        Icon icon = null;
        if (mid.getIconKey() != null) {
            icon = (NewTheme.I().getIcon(mid.getIconKey(), 20));
        }

        if (mid instanceof MenuContainer) {

            type = _GUI._.InfoPanel_update_submenu();

            // label.setText(_GUI._.InfoPanel_updateInfo_header_actionlabel(, ));

        } else if (mid instanceof SeparatorData) {

            name = _GUI._.Renderer_getTreeCellRendererComponent_seperator();

        } else {
            if (mid instanceof MenuLink) {
                type = _GUI._.InfoPanel_update_link();

            } else {

                AppAction action = mid.createAction(null);
                if (StringUtils.isEmpty(name)) {
                    name = action.getName();
                }
                type = _GUI._.InfoPanel_update_action();
                if (icon == null) {
                    icon = action.getSmallIcon();
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

    private void link(MenuItemData mid, JCheckBox hideIfDisabled, MenuItemProperty hideIfDisabled3) {

        hideIfDisabled.setSelected(mid.mergeProperties().contains(hideIfDisabled3));
        hideIfDisabled.setEnabled(mid.getActionData() == null || mid.getActionData().getProperties() == null || !mid.getActionData().getProperties().contains(hideIfDisabled3));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        save();

    }

    private void save() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                HashSet<MenuItemProperty> newProperties = new HashSet<MenuItemProperty>();
                if (hideIfDisabled.isSelected()) newProperties.add(MenuItemProperty.HIDE_IF_DISABLED);
                if (hideIfOpenFileIsUnsupported.isSelected()) newProperties.add(MenuItemProperty.HIDE_IF_OPENFILE_IS_UNSUPPORTED);
                if (hideIfOutputNotExists.isSelected()) newProperties.add(MenuItemProperty.HIDE_IF_OUTPUT_NOT_EXISTING);
                if (linkContext.isSelected()) newProperties.add(MenuItemProperty.LINK_CONTEXT);
                if (packageContext.isSelected()) newProperties.add(MenuItemProperty.PACKAGE_CONTEXT);

                item.setProperties(newProperties);
            }
        }.waitForEDT();

    }
}
