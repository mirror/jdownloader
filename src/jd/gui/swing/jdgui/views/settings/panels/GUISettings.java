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
import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import jd.gui.swing.jdgui.views.settings.panels.urlordertable.UrlOrderTable;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.StorageException;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.ComboBoxDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.jdtrayicon.CFG_TRAY_CONFIG;
import org.jdownloader.gui.jdtrayicon.MenuManagerTrayIcon;
import org.jdownloader.gui.mainmenu.MenuManagerMainmenu;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;
import org.jdownloader.gui.translate.GuiTranslation;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.MenuManagerDownloadTabBottomBar;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuManagerDownloadTableContext;
import org.jdownloader.gui.views.linkgrabber.bottombar.MenuManagerLinkgrabberTabBottombar;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.NewLinksInLinkgrabberAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;
import org.jdownloader.translate.JdownloaderTranslation;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyRestartRequest;

public class GUISettings extends AbstractConfigPanel implements StateUpdateListener {

    private static final long                     serialVersionUID = 1L;

    private SettingsButton                        lng;

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

    private SettingsButton                        downloadBottomManager;

    private SettingsButton                        linkgrabberBottomManager;

    public String getTitle() {
        return _GUI.T.GUISettings_getTitle();
    }

    public GUISettings() {
        super();

        lng = new SettingsButton(new AppAction() {
            {
                String value = TranslationFactory.getDesiredLanguage();
                Locale loc = TranslationFactory.stringToLocale(value);
                String set = loc.getDisplayName(Locale.ENGLISH);
                if (StringUtils.isEmpty(set)) {
                    int tmpIndex = value.indexOf("_");
                    if (tmpIndex >= 0) {
                        String tmp = value.substring(0, tmpIndex);
                        Locale tmpLoc = TranslationFactory.stringToLocale(tmp);
                        if (tmpLoc != null) {
                            set = tmpLoc.getDisplayName(Locale.ENGLISH);
                        }
                    }
                    if (StringUtils.isEmpty(set)) {
                        set = value;
                    } else {
                        set = set + "(" + value + ")";
                    }
                }
                setName(_GUI.T.change_language(set));

            }

            @Override
            public void actionPerformed(ActionEvent e) {

                final AtomicReference<List<String>> languages = new AtomicReference<List<String>>();
                ProgressDialog p = new ProgressDialog(new ProgressGetter() {

                    @Override
                    public void run() throws Exception {
                        List<String> list = TranslationFactory.listAvailableTranslations(JdownloaderTranslation.class, GuiTranslation.class);
                        Collections.sort(list, new Comparator<String>() {

                            @Override
                            public int compare(String o1, String o2) {
                                Locale lc1 = TranslationFactory.stringToLocale(o1);
                                Locale lc2 = TranslationFactory.stringToLocale(o2);
                                String v1 = lc1.getDisplayName(Locale.ENGLISH);
                                String v2 = lc2.getDisplayName(Locale.ENGLISH);
                                if (StringUtils.isEmpty(v1) && StringUtils.isEmpty(v2)) {
                                    return 0;
                                }
                                if (StringUtils.isEmpty(v1) && !StringUtils.isEmpty(v2)) {
                                    return 1;
                                }
                                if (StringUtils.isEmpty(v2) && !StringUtils.isEmpty(v1)) {
                                    return -11;
                                }
                                return v1.compareToIgnoreCase(v2);
                            }
                        });
                        languages.set(list);

                    }

                    @Override
                    public String getString() {
                        return null;
                    }

                    @Override
                    public int getProgress() {
                        return -1;
                    }

                    @Override
                    public String getLabelString() {
                        return null;
                    }
                }, org.appwork.uio.UIOManager.BUTTONS_HIDE_CANCEL, _GUI.T.lit_please_wait(), "", null);

                UIOManager.I().show(null, p);

                ComboBoxDialog comboDialog = new ComboBoxDialog(0, _GUI.T.languages_dialog_title(), _GUI.T.languages_dialog_title(), languages.get().toArray(new String[] {}), languages.get().indexOf(TranslationFactory.getDesiredLanguage()), new AbstractIcon(IconKey.ICON_LANGUAGE, 32), _GUI.T.languages_dialog_change_and_restart(), null, null) {
                    @Override
                    protected ListCellRenderer getRenderer(final ListCellRenderer orgRenderer) {
                        return new ListCellRenderer() {

                            @Override
                            public Component getListCellRendererComponent(JList list, Object v, int index, boolean isSelected, boolean cellHasFocus) {
                                if (v instanceof String) {
                                    String value = (String) v;
                                    Locale loc = TranslationFactory.stringToLocale(value);
                                    String set = loc.getDisplayName(Locale.ENGLISH);
                                    if (StringUtils.isEmpty(set)) {
                                        int tmpIndex = value.indexOf("_");
                                        if (tmpIndex >= 0) {
                                            String tmp = value.substring(0, tmpIndex);
                                            Locale tmpLoc = TranslationFactory.stringToLocale(tmp);
                                            if (tmpLoc != null) {
                                                set = tmpLoc.getDisplayName(Locale.ENGLISH);
                                            }
                                        }
                                        if (StringUtils.isEmpty(set)) {
                                            set = value;
                                        } else {
                                            set = set + "(" + value + ")";
                                        }
                                    }
                                    return orgRenderer.getListCellRendererComponent(list, set, index, isSelected, cellHasFocus);

                                }
                                return orgRenderer.getListCellRendererComponent(list, v, index, isSelected, cellHasFocus);

                            }

                        };
                    }

                    @Override
                    public boolean isRemoteAPIEnabled() {
                        return false;
                    }
                };

                int index = UIOManager.I().show(ComboBoxDialogInterface.class, comboDialog).getSelectedIndex();
                if (index >= 0) {
                    String newLng = languages.get().get(index);

                    if (!newLng.equals(TranslationFactory.getDesiredLanguage())) {

                        try {
                            Dialog.getInstance().showConfirmDialog(0, _GUI.T.GUISettings_save_language_changed_restart_required_title(), _GUI.T.GUISettings_save_language_changed_restart_required_msg(), NewTheme.getInstance().getIcon("language", 32), null, null);
                            JSonStorage.saveTo(Application.getResource("cfg/language.json"), newLng);
                            RestartController.getInstance().asyncRestart(new SmartRlyRestartRequest(true));
                        } catch (DialogClosedException e2) {

                        } catch (DialogCanceledException e2) {

                        }
                    }

                }
            }
        });

        // lng = new ComboBox<String>(TranslationFactory.getDesiredLanguage()) {
        //
        // @Override
        // protected void renderComponent(Component lbl, JList list, String value, int index, boolean isSelected, boolean cellHasFocus) {
        // Locale loc = TranslationFactory.stringToLocale(value);
        // String set = loc.getDisplayName(Locale.ENGLISH);
        // if (StringUtils.isEmpty(set)) {
        // int tmpIndex = value.indexOf("_");
        // if (tmpIndex >= 0) {
        // String tmp = value.substring(0, tmpIndex);
        // Locale tmpLoc = TranslationFactory.stringToLocale(tmp);
        // if (tmpLoc != null) {
        // set = tmpLoc.getDisplayName(Locale.ENGLISH);
        // }
        // }
        // if (StringUtils.isEmpty(set)) {
        // set = value;
        // } else {
        // set = set + "(" + value + ")";
        // }
        // }
        // ((JLabel) lbl).setText(set);
        // }
        //
        // };
        // lng.addPopupMenuListener(new PopupMenuListener() {
        //
        // @Override
        // public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        //
        // if (lng.getModel().getSize() == 1) {
        // loadLanguages();
        // return;
        // }
        // lng.setMaximumRowCount(8);
        //
        // }
        //
        // @Override
        // public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        // String newLng = lng.getSelectedItem();
        // if (!newLng.equals(TranslationFactory.getDesiredLanguage())) {
        // JSonStorage.saveTo(Application.getResource("cfg/language.json"), newLng);
        //
        // try {
        // Dialog.getInstance().showConfirmDialog(0, _GUI.T.GUISettings_save_language_changed_restart_required_title(),
        // _GUI.T.GUISettings_save_language_changed_restart_required_msg(), NewTheme.getInstance().getIcon("language", 32), null, null);
        // RestartController.getInstance().asyncRestart(new SmartRlyRestartRequest(true));
        // } catch (DialogClosedException e2) {
        //
        // } catch (DialogCanceledException e2) {
        //
        // }
        // }
        // }
        //
        // @Override
        // public void popupMenuCanceled(PopupMenuEvent e) {
        //
        // }
        // });

        resetDialogs = new SettingsButton(new AppAction() {
            {
                setName(_GUI.T.GUISettings_GUISettings_resetdialogs_());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    AbstractDialog.resetDialogInformations();
                    CFG_TRAY_CONFIG.ON_CLOSE_ACTION.setValue(CFG_TRAY_CONFIG.ON_CLOSE_ACTION.getDefaultValue());
                    Dialog.getInstance().showMessageDialog(_GUI.T.GUISettings_actionPerformed_reset_done());

                } catch (StorageException e1) {
                    e1.printStackTrace();
                }
            }
        });
        resetDialogPosition = new SettingsButton(new AppAction() {
            {
                setName(_GUI.T.GUISettings_GUISettings_resetdialog_positions_());
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
                        if (pathname.getName().startsWith("RememberRelativeLocator")) {
                            pathname.deleteOnExit();
                            return true;

                        }
                        if (pathname.getName().startsWith("RememberAbsoluteLocator-")) {
                            pathname.deleteOnExit();
                            return true;

                        }
                        if (pathname.getName().startsWith("RememberAbsoluteLocator-")) {
                            pathname.deleteOnExit();
                            return true;

                        }
                        if (pathname.getName().startsWith("gui.windows.dimensionsandlocations")) {
                            pathname.deleteOnExit();
                            return true;

                        }
                        if (pathname.getName().startsWith("RememberLastDimensor-")) {
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
                        Dialog.getInstance().showConfirmDialog(0, _GUI.T.jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion_title(), _GUI.T.jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion(), NewTheme.getInstance().getIcon("desktop", 32), null, null);
                        RestartController.getInstance().asyncRestart(new SmartRlyRestartRequest(true));
                    } catch (DialogClosedException e2) {

                    } catch (DialogCanceledException e2) {

                    }
                }

            }
        });
        contextMenuManagerDownloadList = new SettingsButton(new AppAction() {
            {
                setName(_GUI.T.gui_config_menumanager_downloadlist());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        JDGui.getInstance().requestPanel(JDGui.Panels.DOWNLOADLIST);

                        MenuManagerDownloadTableContext.getInstance().openGui();
                    }
                };

            }
        });

        contextMenuManagerLinkgrabber = new SettingsButton(new AppAction() {
            {
                setName(_GUI.T.gui_config_menumanager_linkgrabber());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        JDGui.getInstance().requestPanel(JDGui.Panels.LINKGRABBER);

                        MenuManagerLinkgrabberTableContext.getInstance().openGui();
                    }
                };

            }
        });
        toolbarManager = new SettingsButton(new AppAction() {
            {
                setName(_GUI.T.gui_config_menumanager_toolbar());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {

                        MenuManagerMainToolbar.getInstance().openGui();
                    }
                };

            }
        });

        mainMenuManager = new SettingsButton(new AppAction() {
            {
                setName(_GUI.T.gui_config_menumanager_mainmenu());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        MenuManagerMainmenu.getInstance().openGui();
                    }
                };

            }
        });

        trayMenuManager = new SettingsButton(new AppAction() {
            {
                setName(_GUI.T.gui_config_menumanager_traymenu());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        MenuManagerTrayIcon.getInstance().openGui();
                    }
                };

            }
        });

        downloadBottomManager = new SettingsButton(new AppAction() {
            {
                setName(_GUI.T.gui_config_menumanager_downloadBottom());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {

                        MenuManagerDownloadTabBottomBar.getInstance().openGui();
                    }
                };

            }
        });

        linkgrabberBottomManager = new SettingsButton(new AppAction() {
            {
                setName(_GUI.T.gui_config_menumanager_linkgrabberBottom());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {

                        MenuManagerLinkgrabberTabBottombar.getInstance().openGui();
                    }
                };

            }
        });

        this.addHeader(getTitle(), new AbstractIcon(IconKey.ICON_GUI, 32));
        this.addDescription(_GUI.T.GUISettings_GUISettings_description());
        // this.addHeader(getTitle(),
        // new AbstractIcon(IconKey.ICON_barrierfreesettings", 32));
        this.addPair(_GUI.T.gui_config_language(), null, lng);
        this.addPair(_GUI.T.gui_config_dialogs(), null, resetDialogs);
        this.addPair("", null, resetDialogPosition);
        this.addHeader(_GUI.T.GUISettings_GUISettings_object_urls(), NewTheme.I().getIcon(IconKey.ICON_URL, 32));
        this.addDescription(_GUI.T.GUISettings_GUISettings_object_urls_description());

        UrlOrderContainer container = new UrlOrderContainer(new UrlOrderTable());
        addPair("", null, container);

        this.addHeader(_GUI.T.gui_config_menumanager_header(), new AbstractIcon(IconKey.ICON_MENU, 32));

        this.addDescription(_GUI.T.gui_config_menumanager_desc());
        this.addPair("", null, contextMenuManagerDownloadList);
        this.addPair("", null, contextMenuManagerLinkgrabber);
        this.addPair("", null, toolbarManager);
        this.addPair("", null, mainMenuManager);
        this.addPair("", null, trayMenuManager);
        this.addPair("", null, downloadBottomManager);
        this.addPair("", null, linkgrabberBottomManager);

        this.addHeader(_GUI.T.GUISettings_GUISettings_object_frames(), new AbstractIcon(IconKey.ICON_DESKTOP, 32));
        this.addDescription(_GUI.T.GUISettings_GUISettings_object_frames_description());
        // this.addHeader(_GUI.T.GUISettings_GUISettings_object_accessability(), new AbstractIcon(IconKey.ICON_barrierfreesettings", 32));
        // this.addDescription(_JDT.T.gui_settings_barrierfree_description());
        this.addDescriptionPlain(_GUI.T.GUISettings_GUISettings_sielntMode_description());
        addPair(_GUI.T.GUISettings_GUISettings_sielntMode(), null, new Checkbox(CFG_SILENTMODE.MANUAL_ENABLED));
        // OS_DEFAULT,
        // TO_FRONT,
        // TO_BACK,
        // TO_FRONT_FOCUSED;
        focus = new ComboBox<FrameState>(new FrameState[] { FrameState.OS_DEFAULT, FrameState.TO_BACK, FrameState.TO_FRONT, FrameState.TO_FRONT_FOCUSED }, new String[] { _GUI.T.GUISettings_GUISettings_framestate_os_default(System.getProperty("os.name")), _GUI.T.GUISettings_GUISettings_framestate_back(), _GUI.T.GUISettings_GUISettings_framestate_front(), _GUI.T.GUISettings_GUISettings_framestate_focus() }) {

        };
        focus.addStateUpdateListener(this);
        addPair(_GUI.T.GUISettings_GUISettings_dialog_focus(), null, focus);

        linkgrabberfocus = new ComboBox<NewLinksInLinkgrabberAction>(new NewLinksInLinkgrabberAction[] { NewLinksInLinkgrabberAction.NOTHING, NewLinksInLinkgrabberAction.SWITCH, NewLinksInLinkgrabberAction.TO_FRONT, NewLinksInLinkgrabberAction.FOCUS }, new String[] { _GUI.T.GUISettings_GUISettings_newlinks_nothing(), _GUI.T.GUISettings_GUISettings_newlinks_switch(), _GUI.T.GUISettings_GUISettings_newlinks_front(), _GUI.T.GUISettings_GUISettings_newlinks_focus() });
        linkgrabberfocus.addStateUpdateListener(this);
        addPair(_GUI.T.GUISettings_GUISettings_dialog_linkgrabber_on_new_links(), null, linkgrabberfocus);

    }

    protected void loadLanguages() {

        List<String> list = TranslationFactory.listAvailableTranslations(JdownloaderTranslation.class, GuiTranslation.class);
        Collections.sort(list, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                Locale lc1 = TranslationFactory.stringToLocale(o1);
                Locale lc2 = TranslationFactory.stringToLocale(o2);
                String v1 = lc1.getDisplayName(Locale.ENGLISH);
                String v2 = lc2.getDisplayName(Locale.ENGLISH);
                if (StringUtils.isEmpty(v1) && StringUtils.isEmpty(v2)) {
                    return 0;
                }
                if (StringUtils.isEmpty(v1) && !StringUtils.isEmpty(v2)) {
                    return 1;
                }
                if (StringUtils.isEmpty(v2) && !StringUtils.isEmpty(v1)) {
                    return -11;
                }
                return v1.compareToIgnoreCase(v2);
            }
        });
        final String[] languages = list.toArray(new String[] {});
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                // lng.setModel(new DefaultComboBoxModel(languages));
                // lng.setSelectedItem(TranslationFactory.getDesiredLanguage());
            }
        };

    }

    @Override
    public Icon getIcon() {
        return new AbstractIcon(IconKey.ICON_GUI, 32);
    }

    @Override
    public void save() {
        CFG_GUI.CFG.setNewDialogFrameState(focus.getSelectedItem());
        CFG_GUI.CFG.setNewLinksActionV2(linkgrabberfocus.getSelectedItem());
        // focus.setSelectedItem(CFG_GUI.CFG.getNewDialogFrameState());
    }

    @Override
    public void updateContents() {
        setting = true;
        try {
            focus.setSelectedItem(CFG_GUI.CFG.getNewDialogFrameState());
            NewLinksInLinkgrabberAction newvalue = CFG_GUI.CFG.getNewLinksActionV2();
            linkgrabberfocus.setSelectedItem(newvalue);

        } finally {
            setting = false;
        }

    }

    @Override
    public void onStateUpdated() {
        if (!setting) {
            save();
        }
    }
}