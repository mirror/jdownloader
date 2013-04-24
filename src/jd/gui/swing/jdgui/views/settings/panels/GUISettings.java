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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;

import jd.gui.UserIF;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.gui.swing.jdgui.views.settings.components.Spinner;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.StorageException;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.JDRestartController;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate.GuiTranslation;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.contextmenumanager.DownloadListContextMenuManager;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate.JdownloaderTranslation;
import org.jdownloader.translate._JDT;

public class GUISettings extends AbstractConfigPanel {

    private static final long serialVersionUID = 1L;
    private Spinner           captchaSize;
    private ComboBox<String>  lng;
    private String[]          languages;
    private Thread            languageScanner;
    private SettingsButton    resetDialogs;
    private SettingsButton    contextMenuManagerDownloadList;

    public String getTitle() {
        return _JDT._.gui_settings__gui_title();
    }

    public GUISettings() {
        super();
        captchaSize = new Spinner(org.jdownloader.settings.staticreferences.CFG_GUI.CAPTCHA_SCALE_FACTOR);
        captchaSize.setFormat("#'%'");

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

                        JDRestartController.getInstance().directAsynchRestart();
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
                    Dialog.getInstance().showMessageDialog(_GUI._.GUISettings_actionPerformed_reset_done());

                } catch (StorageException e1) {
                    e1.printStackTrace();
                }
            }
        });
        contextMenuManagerDownloadList = new SettingsButton(new AppAction() {
            {
                setName(_GUI._.lit_open());

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        JDGui.getInstance().requestPanel(UserIF.Panels.DOWNLOADLIST, null);

                        DownloadListContextMenuManager.getInstance();
                    }
                };

            }
        });

        this.addHeader(getTitle(), NewTheme.I().getIcon("gui", 32));

        // this.addHeader(getTitle(),
        // NewTheme.I().getIcon("barrierfreesettings", 32));
        this.addDescription(_JDT._.gui_settings_barrierfree_description());
        this.addPair(_GUI._.gui_config_barrierfree_captchasize(), null, captchaSize);
        this.addPair(_GUI._.gui_config_language(), null, lng);
        this.addPair(_GUI._.gui_config_dialogs(), null, resetDialogs);
        this.addHeader(_GUI._.gui_config_menumanager(), NewTheme.I().getIcon("menu", 32));

        this.addDescription(_GUI._.gui_config_menumanager_desc());
        this.addPair(_GUI._.gui_config_menumanager(), null, contextMenuManagerDownloadList);

    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("gui", 32);
    }

    @Override
    public void save() {

    }

    @Override
    public void updateContents() {
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

    }

}