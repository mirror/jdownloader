//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.views.settings.panels;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.FolderChooser;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.Spinner;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.ConstraintParser;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.swing.models.ConfigIntSpinnerModel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.settings.Pair;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.AutoDownloadStartOption;
import org.jdownloader.settings.CleanAfterDownloadAction;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.IfFileExistsAction;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
import org.jdownloader.translate._JDT;

public class GeneralSettingsConfigPanel extends AbstractConfigPanel {

    private static final long                  serialVersionUID = 3383448498625377495L;

    private FolderChooser                      downloadFolder;
    private Spinner                            maxSimPerHost;
    private ComboBox<CleanAfterDownloadAction> remove;
    private ComboBox<IfFileExistsAction>       ifFileExists;
    private Spinner                            maxSim;
    private GeneralSettings                    config;
    private Spinner                            maxchunks;
    private ComboBox<AutoDownloadStartOption>  startDownloadsAfterAppStart;
    private Spinner                            startDownloadTimeout;
    private Checkbox                           subfolder;
    private Checkbox                           autoCRC;
    private Checkbox                           simpleContainer;
    private Checkbox                           linkGrabberFilesPackages;

    public GeneralSettingsConfigPanel() {
        super();

        /*
         * Override default implementation of MigLayout layout manager and use one more suitable to this panel
         * 
         * Useful resources: http://www.migcalendar.com/miglayout/mavensite/apidocs/index.html
         * http://www.migcalendar.com/miglayout/mavensite/docs/cheatsheet.pdf
         */

        // Layout constraints
        LC layCons = new LC();
        // Apply layout rules
        layCons.setInsets(ConstraintParser.parseInsets("15", true));
        layCons.wrapAfter(3);

        // Axis constraints
        AC axiCons = new AC();
        // Column 1
        // Default
        // Column 2
        axiCons.index(1).shrink();
        // Column 3
        axiCons.index(2).fill();
        axiCons.index(2).grow();

        // Override default layout

        setLayout(new MigLayout(layCons, axiCons));

        config = org.jdownloader.settings.staticreferences.CFG_GENERAL.CFG;

        /* Download folder */

        downloadFolder = new FolderChooser();
        this.addHeader(_GUI._.gui_config_general_downloaddirectory(), NewTheme.I().getIcon("downloadpath", 32));
        this.addDescription(_JDT._.gui_settings_downloadpath_description());
        this.add(downloadFolder);

        /* Download management */

        String[] removeDownloads = new String[] { _GUI._.gui_config_general_toDoWithDownloads_immediate(), _GUI._.gui_config_general_toDoWithDownloads_atstart(), _GUI._.gui_config_general_toDoWithDownloads_packageready(), _GUI._.gui_config_general_toDoWithDownloads_never() };
        String[] fileExists = new String[] { _GUI._.system_download_triggerfileexists_overwrite(), CFG_GENERAL.CFG.getOnSkipDueToAlreadyExistsAction().getLabel(), _GUI._.system_download_triggerfileexists_rename(), _GUI._.system_download_triggerfileexists_ask(), _GUI._.system_download_triggerfileexists_ask() };

        maxSim = new Spinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS));
        maxSimPerHost = new Spinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS_PER_HOST));
        maxchunks = new Spinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_CHUNKS_PER_FILE));
        remove = new ComboBox<CleanAfterDownloadAction>(CleanAfterDownloadAction.values(), removeDownloads);
        ifFileExists = new ComboBox<IfFileExistsAction>(IfFileExistsAction.values(), fileExists);

        this.addHeader(_JDT._.gui_settings_downloadcontroll_title(), NewTheme.I().getIcon("downloadmanagment", 32));
        this.addDescription(_JDT._.gui_settings_downloadcontroll_description());
        this.addPair(_GUI._.gui_config_download_simultan_downloads(), null, maxSim);
        this.addPair(_GUI._.gui_config_download_simultan_downloads_per_host2(), org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_DOWNLOADS_PER_HOST_ENABLED, maxSimPerHost);
        this.addPair(_GUI._.gui_config_download_max_chunks(), null, maxchunks);
        this.addPair(_GUI._.gui_config_general_todowithdownloads(), null, remove);
        this.addPair(_GUI._.system_download_triggerfileexists(), null, ifFileExists);

        /* Autostart downloads */

        startDownloadsAfterAppStart = new ComboBox<AutoDownloadStartOption>(CFG_GENERAL.SH.getKeyHandler("AutoStartDownloadOption", KeyHandler.class), AutoDownloadStartOption.values(), new String[] { _GUI._.gui_config_general_AutoDownloadStartOption_always(), _GUI._.gui_config_general_AutoDownloadStartOption_only_if_closed_running(), _GUI._.gui_config_general_AutoDownloadStartOption_never() });
        startDownloadTimeout = new Spinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.CFG_GENERAL.AUTO_START_COUNTDOWN_SECONDS));
        this.addHeader(_GUI._.gui_config_download_autostart(), NewTheme.I().getIcon("resume", 32));
        this.addDescription(_GUI._.gui_config_download_autostart_desc());
        this.addPair(_GUI._.system_download_autostart(), null, startDownloadsAfterAppStart);
        this.addPair(_GUI._.system_download_autostart_countdown(), CFG_GENERAL.SHOW_COUNTDOWNON_AUTO_START_DOWNLOADS, startDownloadTimeout);

        /* Linkgrabber */

        linkGrabberFilesPackages = new Checkbox(CFG_LINKGRABBER.VARIOUS_PACKAGE_ENABLED);
        this.addHeader(_GUI._.GeneralSettingsConfigPanel_GeneralSettingsConfigPanel_linkgrabber(), NewTheme.I().getIcon("linkgrabber", 32));
        this.addPair(_GUI._.GeneralSettingsConfigPanel_GeneralSettingsConfigPanel_various_package(), null, linkGrabberFilesPackages);

        /* File Writing */

        autoCRC = new Checkbox();
        this.addHeader(_GUI._.gui_config_download_write(), NewTheme.I().getIcon("hashsum", 32));
        this.addDescription(_JDT._.gui_settings_filewriting_description());
        this.addPair(_GUI._.gui_config_download_crc(), null, autoCRC);

        /* Miscellaneous */

        simpleContainer = new Checkbox();
        this.addHeader(_GUI._.gui_config_various(), NewTheme.I().getIcon("settings", 32));
        this.addPair(_GUI._.gui_config_simple_container(), null, simpleContainer);
    }

    public String getTitle() {
        return _JDT._.gui_settings_general_title();
    }

    public String getIconKey() {
        return "settings";
    }

    @Override
    public <T extends SettingsComponent> Pair<T> addPair(String name, BooleanKeyHandler enabled, T comp) {
        String lblConstraints = "gapleft: " + getLeftGap();
        return addPair(name, lblConstraints, enabled, comp);
    }

    @Override
    public <T extends SettingsComponent> Pair<T> addPair(String name, String lblConstraints, BooleanKeyHandler enabled, T comp) {

        String con = "pushx,growy";
        if (comp.getConstraints() != null) {
            con += "," + comp.getConstraints();
        }

        // COL 1: Label

        JLabel lbl;
        ExtCheckBox cb = null;

        lbl = createLabel(name);
        add(lbl, lblConstraints);

        // COL 2/3: If T component enabled state is defined, add a checkbox to col 2 to toggle its state
        // and add the T component to col 3
        if (enabled != null) {
            cb = new ExtCheckBox(enabled, lbl, (JComponent) comp);
            cb.setToolTipText(_GUI._.AbstractConfigPanel_addPair_enabled());
            SwingUtils.setOpaque(cb, false);
            add(cb, "width " + cb.getPreferredSize().width + "!, aligny " + (comp.isMultiline() ? "top" : "center"));
            add((JComponent) comp, con);
        }
        // If the T component is only a checkbox, put it in col 2 and fill col 3 with glue
        else if (comp instanceof JCheckBox) {
            add((JComponent) comp, con);
            add(Box.createHorizontalGlue(), "");
        }
        // If the T component has no enabled state, fill col 2 with glue and put the T component in col 3
        else {
            add(Box.createHorizontalGlue(), "");
            add((JComponent) comp, con);
        }

        Pair<T> p = new Pair<T>(lbl, comp, cb);
        pairs.add(p);
        return p;
    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("home", 32);
    }

    @Override
    public void save() {
        GeneralSettings st = JsonConfig.create(GeneralSettings.class);
        st.setDefaultDownloadFolder(downloadFolder.getText());
        st.setHashCheckEnabled(autoCRC.isSelected());
        st.setAutoOpenContainerAfterDownload(simpleContainer.isSelected());
        config.setCleanupAfterDownloadAction(remove.getValue());
        config.setIfFileExistsAction(this.ifFileExists.getValue());
    }

    @Override
    public void updateContents() {
        GeneralSettings st = JsonConfig.create(GeneralSettings.class);
        downloadFolder.setText(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());
        autoCRC.setSelected(st.isHashCheckEnabled());
        simpleContainer.setSelected(st.isAutoOpenContainerAfterDownload());
        this.remove.setValue(config.getCleanupAfterDownloadAction());
        this.ifFileExists.setValue(config.getIfFileExistsAction());
    }
}