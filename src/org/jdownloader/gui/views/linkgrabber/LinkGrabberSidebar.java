package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Box;

import jd.controlling.FavIconController;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.MigPanel;
import org.appwork.storage.config.ConfigEventListener;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.KeyHandler;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.LinkFilterSettings;
import org.jdownloader.gui.translate._GUI;

public class LinkGrabberSidebar extends MigPanel {

    private LinkGrabberTable   table;
    private MigPanel           quicksettings;
    private FilterTable        hosterFilterTable;
    private FilterTable        filetypeFilterTable;
    private LinkFilterSettings config;
    private Header             hosterFilter;
    private Header             filetypeFilter;
    private Header             quickSettingsHeader;
    private Checkbox           addTop;
    private Checkbox           autoConfirm;
    private Checkbox           autoStart;

    public LinkGrabberSidebar(LinkGrabberTable table) {
        super("ins 0,wrap 1", "[grow,fill]", "[][][][][grow,fill][]");
        config = JsonConfig.create(LinkFilterSettings.class);
        this.table = table;
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();

        if (c >= 0) {
            setBackground(new Color(c));
            setOpaque(true);
        }
        hosterFilter = new Header(_GUI._.LinkGrabberSidebar_LinkGrabberSidebar_hosterfilter());
        hosterFilter.getCheckBox().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                config.setLinkgrabberHosterQuickfilterEnabled(hosterFilter.isSelected());
            }
        });
        filetypeFilter = new Header(_GUI._.LinkGrabberSidebar_LinkGrabberSidebar_extensionfilter());
        filetypeFilter.getCheckBox().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                config.setLinkgrabberFiletypeQuickfilterEnabled(filetypeFilter.isSelected());
            }
        });

        quickSettingsHeader = new Header(_GUI._.LinkGrabberSidebar_LinkGrabberSidebar_settings());
        quickSettingsHeader.getCheckBox().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                config.setLinkgrabberQuickSettingsVisible(quickSettingsHeader.isSelected());
            }
        });

        hosterFilterTable = new FilterTable();
        Filter filter = new Filter("rapidshare.com", null, true);
        filter.setIcon(FavIconController.getFavIcon("rapidshare.com", filter, true));
        hosterFilterTable.getExtTableModel().addElement(filter);
        filter = new Filter("share-online.biz", null, true);
        filter.setIcon(FavIconController.getFavIcon("share-online.biz", filter, true));
        hosterFilterTable.getExtTableModel().addElement(filter);
        filetypeFilterTable = new FilterTable();
        filetypeFilterTable.setVisible(config.isLinkgrabberFiletypeQuickfilterEnabled());
        try {
            filetypeFilterTable.getExtTableModel().addAllElements(new Filter("rar", CrossSystem.getMime().getFileIcon("rar", 16, 16), true), new Filter("rar", CrossSystem.getMime().getFileIcon("rar", 16, 16), true), new Filter("html", CrossSystem.getMime().getFileIcon("html", 16, 16), true), new Filter("zip", CrossSystem.getMime().getFileIcon("zip", 16, 16), true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        quicksettings = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[]0[]0[]");
        add(hosterFilter, "gaptop 7");
        add(hosterFilterTable, "hidemode 2");
        add(filetypeFilter, "gaptop 7");
        add(filetypeFilterTable, "hidemode 2");
        add(Box.createGlue());
        add(quickSettingsHeader, "gaptop 7");

        add(quicksettings, "hidemode 2");

        addTop = new Checkbox(_GUI._.LinkGrabberSidebar_LinkGrabberSidebar_addtop(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_addtop_tt()) {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected void setSelected(boolean selected) {
                config.setLinkgrabberAddAtTop(selected);
            }

            @Override
            protected boolean isSelected() {
                return config.isLinkgrabberAddAtTop();
            }

        };
        autoConfirm = new Checkbox(_GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autoconfirm(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autoconfirm_tt()) {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected void setSelected(boolean selected) {
                config.setLinkgrabberAutoConfirmEnabled(selected);
            }

            @Override
            protected boolean isSelected() {
                return config.isLinkgrabberAutoConfirmEnabled();
            }

        };
        autoStart = new Checkbox(_GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autostart(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autostart_tt()) {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected void setSelected(boolean selected) {
                config.setLinkgrabberAutoStartEnabled(selected);
            }

            @Override
            protected boolean isSelected() {
                return config.isLinkgrabberAutoStartEnabled();
            }

        };

        quicksettings.add(addTop);
        quicksettings.add(autoConfirm);
        quicksettings.add(autoStart);
        // TableHeaderUI ds = table.getTableHeader().getUI();
        // JTableHeader th = table.getTableHeader();

        // setBorder(new JTextField().getBorder());
        // header = new LinkGrabberSideBarHeader();
        // scrollPane = new JScrollPane();
        // scrollPane.setBorder(null);
        // add(header, " height 20!");

        // add(scrollPane);

        config.getStorageHandler().getEventSender().addListener(new ConfigEventListener() {

            public void onConfigValueModified(ConfigInterface cfg, String key, Object newValue) {

                updateVisibility();
            }

            public void onConfigValidatorError(ConfigInterface config, Throwable validateException, KeyHandler methodHandler) {
            }
        });
        updateVisibility();

    }

    protected void updateVisibility() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                quicksettings.setVisible(config.isLinkgrabberQuickSettingsVisible());
                filetypeFilterTable.setVisible(config.isLinkgrabberFiletypeQuickfilterEnabled());
                hosterFilterTable.setVisible(config.isLinkgrabberHosterQuickfilterEnabled());
                hosterFilter.setSelected(config.isLinkgrabberHosterQuickfilterEnabled());
                filetypeFilter.setSelected(config.isLinkgrabberFiletypeQuickfilterEnabled());
                quickSettingsHeader.setSelected(config.isLinkgrabberQuickSettingsVisible());
            }
        };
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    public void paint(Graphics g) {
        super.paint(g);
    }

    protected void paintBorder(Graphics g) {
        super.paintBorder(g);
    }
}
