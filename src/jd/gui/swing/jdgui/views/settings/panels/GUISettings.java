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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.StorageException;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.WindowManager.FrameState;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.jdtrayicon.CFG_TRAY_CONFIG;
import org.jdownloader.gui.jdtrayicon.TrayIconMenuManager;
import org.jdownloader.gui.mainmenu.MainMenuManager;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.toolbar.MainToolbarManager;
import org.jdownloader.gui.translate.GuiTranslation;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.contextmenumanager.DownloadListContextMenuManager;
import org.jdownloader.gui.views.linkgrabber.contextmenu.LinkgrabberContextMenuManager;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.NewLinksInLinkgrabberAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;
import org.jdownloader.translate.JdownloaderTranslation;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyRestartRequest;

public class GUISettings extends AbstractConfigPanel implements StateUpdateListener {

    private static final long                     serialVersionUID = 1L;

    private ComboBox<String>                      lng;
    private String[]                              languages;
    private Thread                                languageScanner;
    private SettingsButton                        resetDialogs;
    private SettingsButton                        contextMenuManagerDownloadList;
    private SettingsButton                        contextMenuManagerLinkgrabber;
    private SettingsButton                        toolbarManager;
    private SettingsButton                        mainMenuManager;
    private SettingsButton                        trayMenuManager;
    private SettingsButton                        resetDialogPosition;

    private ComboBox<FrameState>                  focus;

    private ComboBox<NewLinksInLinkgrabberAction> linkgrabberfocus;

    private boolean                               setting;

    public String getTitle() {
        return _GUI._.GUISettings_getTitle();
    }

    public GUISettings() {
        super();

        lng = new ComboBox<String>(TranslationFactory.getDesiredLanguage()) {

            @Override
            protected void renderComponent(Component lbl, JList list, String value, int index, boolean isSelected, boolean cellHasFocus) {
                Locale loc = TranslationFactory.stringToLocale(value);
                ((JLabel) lbl).setText(loc.getDisplayName(Locale.ENGLISH));

            }

        };

        lng.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String newLng = (String) lng.getSelectedItem();

                if (!newLng.equals(TranslationFactory.getDesiredLanguage())) {
                    JSonStorage.saveTo(Application.getResource("cfg/language.json"), newLng);

                    try {
                        Dialog.getInstance().showConfirmDialog(0, _GUI._.GUISettings_save_language_changed_restart_required_title(), _GUI._.GUISettings_save_language_changed_restart_required_msg(), NewTheme.getInstance().getIcon("language", 32), null, null);
                        RestartController.getInstance().asyncRestart(new SmartRlyRestartRequest(true));
                    } catch (DialogClosedException e2) {

                    } catch (DialogCanceledException e2) {

                    }
                }
            }
        });

        resetDialogs = new SettingsButton(new AppAction() {
            {
                setName(_GUI._.GUISettings_GUISettings_resetdialogs_());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    AbstractDialog.resetDialogInformations();
                    CFG_TRAY_CONFIG.ON_CLOSE_ACTION.setValue(CFG_TRAY_CONFIG.ON_CLOSE_ACTION.getDefaultValue());
                    Dialog.getInstance().showMessageDialog(_GUI._.GUISettings_actionPerformed_reset_done());

                } catch (StorageException e1) {
                    e1.printStackTrace();
                }
            }
        });
        resetDialogPosition = new SettingsButton(new AppAction() {
            {
                setName(_GUI._.GUISettings_GUISettings_resetdialog_positions_());
                // setEnabled(false);
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                if (Application.getResource("cfg/").listFiles(new FileFilter() {

                    @Override
                    public boolean accept(File pathname) {
                        if (pathname.getName().startsWith("org.appwork.utils.swing.locator.")) {
                            pathname.deleteOnExit();
                            return true;

                        }
                        if (pathname.getName().startsWith("org.appwork.utils.swing.dimensor.")) {
                            pathname.deleteOnExit();
                            return true;

                        }
                        if (pathname.getName().startsWith("CaptchaDialogDimensions")) {
                            pathname.deleteOnExit();
                            return true;

                        }

                        return false;
                    }
                }).length > 0) {
                    try {
                        Dialog.getInstance().showConfirmDialog(0, _GUI._.jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion_title(), _GUI._.jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion(), NewTheme.getInstance().getIcon("desktop", 32), null, null);
                        RestartController.getInstance().asyncRestart(new SmartRlyRestartRequest(true));
                    } catch (DialogClosedException e2) {

                    } catch (DialogCanceledException e2) {

                    }
                }

            }
        });
        contextMenuManagerDownloadList = new SettingsButton(new AppAction() {
            {
                setName(_GUI._.gui_config_menumanager_downloadlist());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        JDGui.getInstance().requestPanel(JDGui.Panels.DOWNLOADLIST);

                        DownloadListContextMenuManager.getInstance().openGui();
                    }
                };

            }
        });

        contextMenuManagerLinkgrabber = new SettingsButton(new AppAction() {
            {
                setName(_GUI._.gui_config_menumanager_linkgrabber());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        JDGui.getInstance().requestPanel(JDGui.Panels.LINKGRABBER);

                        LinkgrabberContextMenuManager.getInstance().openGui();
                    }
                };

            }
        });
        toolbarManager = new SettingsButton(new AppAction() {
            {
                setName(_GUI._.gui_config_menumanager_toolbar());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {

                        MainToolbarManager.getInstance().openGui();
                    }
                };

            }
        });

        mainMenuManager = new SettingsButton(new AppAction() {
            {
                setName(_GUI._.gui_config_menumanager_mainmenu());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        MainMenuManager.getInstance().openGui();
                    }
                };

            }
        });

        trayMenuManager = new SettingsButton(new AppAction() {
            {
                setName(_GUI._.gui_config_menumanager_traymenu());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        TrayIconMenuManager.getInstance().openGui();
                    }
                };

            }
        });
        this.addHeader(getTitle(), NewTheme.I().getIcon("gui", 32));
        this.addDescription(_GUI._.GUISettings_GUISettings_description());
        // this.addHeader(getTitle(),
        // NewTheme.I().getIcon("barrierfreesettings", 32));
        this.addPair(_GUI._.gui_config_language(), null, lng);
        this.addPair(_GUI._.gui_config_dialogs(), null, resetDialogs);
        this.addPair("", null, resetDialogPosition);
        this.addHeader(_GUI._.gui_config_menumanager_header(), NewTheme.I().getIcon("menu", 32));

        this.addDescription(_GUI._.gui_config_menumanager_desc());
        this.addPair("", null, contextMenuManagerDownloadList);
        this.addPair("", null, contextMenuManagerLinkgrabber);
        this.addPair("", null, toolbarManager);
        this.addPair("", null, mainMenuManager);
        this.addPair("", null, trayMenuManager);
        this.addHeader(_GUI._.GUISettings_GUISettings_object_frames(), NewTheme.I().getIcon(IconKey.ICON_DESKTOP, 32));
        this.addDescription(_GUI._.GUISettings_GUISettings_object_frames_description());
        // this.addHeader(_GUI._.GUISettings_GUISettings_object_accessability(), NewTheme.I().getIcon("barrierfreesettings", 32));
        // this.addDescription(_JDT._.gui_settings_barrierfree_description());
        this.addDescriptionPlain(_GUI._.GUISettings_GUISettings_sielntMode_description());
        addPair(_GUI._.GUISettings_GUISettings_sielntMode(), null, new Checkbox(CFG_SILENTMODE.MANUAL_ENABLED));
        // OS_DEFAULT,
        // TO_FRONT,
        // TO_BACK,
        // TO_FRONT_FOCUSED;
        focus = new ComboBox<FrameState>(new FrameState[] { FrameState.OS_DEFAULT, FrameState.TO_BACK, FrameState.TO_FRONT, FrameState.TO_FRONT_FOCUSED }, new String[] { _GUI._.GUISettings_GUISettings_framestate_os_default(System.getProperty("os.name")), _GUI._.GUISettings_GUISettings_framestate_back(), _GUI._.GUISettings_GUISettings_framestate_front(), _GUI._.GUISettings_GUISettings_framestate_focus() }) {

        };
        focus.addStateUpdateListener(this);
        addPair(_GUI._.GUISettings_GUISettings_dialog_focus(), null, focus);

        linkgrabberfocus = new ComboBox<NewLinksInLinkgrabberAction>(new NewLinksInLinkgrabberAction[] { NewLinksInLinkgrabberAction.NOTHING, NewLinksInLinkgrabberAction.TO_FRONT, NewLinksInLinkgrabberAction.FOCUS }, new String[] { _GUI._.GUISettings_GUISettings_newlinks_nothing(), _GUI._.GUISettings_GUISettings_newlinks_front(), _GUI._.GUISettings_GUISettings_newlinks_focus() });
        linkgrabberfocus.addStateUpdateListener(this);
        addPair(_GUI._.GUISettings_GUISettings_dialog_linkgrabber_on_new_links(), null, linkgrabberfocus);
    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("gui", 32);
    }

    @Override
    public void save() {
        CFG_GUI.CFG.setNewDialogFrameState(focus.getSelectedItem());
        CFG_GUI.CFG.setNewLinksAction(linkgrabberfocus.getSelectedItem());
        // focus.setSelectedItem(CFG_GUI.CFG.getNewDialogFrameState());
    }

    @Override
    public void updateContents() {
        setting = true;
        try {
            focus.setSelectedItem(CFG_GUI.CFG.getNewDialogFrameState());
            NewLinksInLinkgrabberAction newvalue = CFG_GUI.CFG.getNewLinksAction();
            linkgrabberfocus.setSelectedItem(newvalue);
            // asynch loading, because listAvailableTranslations can take its time.
            if (languages == null && languageScanner == null) {
                languageScanner = new Thread("LanguageScanner") {
                    public void run() {
                        List<String> list = TranslationFactory.listAvailableTranslations(JdownloaderTranslation.class, GuiTranslation.class);
                        Collections.sort(list, new Comparator<String>() {

                            @Override
                            public int compare(String o1, String o2) {
                                Locale lc1 = TranslationFactory.stringToLocale(o1);
                                Locale lc2 = TranslationFactory.stringToLocale(o2);

                                return lc1.getDisplayName(Locale.ENGLISH).compareToIgnoreCase(lc2.getDisplayName(Locale.ENGLISH));
                            }
                        });
                        languages = list.toArray(new String[] {});
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                lng.setModel(new DefaultComboBoxModel(languages));
                                lng.setSelectedItem(TranslationFactory.getDesiredLanguage());
                            }
                        };
                        languageScanner = null;
                    }
                };
                languageScanner.start();
            }
        } finally {
            setting = false;
        }

    }

    @Override
    public void onStateUpdated() {
        if (!setting) save();
    }
}