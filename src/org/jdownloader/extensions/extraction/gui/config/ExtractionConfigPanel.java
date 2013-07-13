package org.jdownloader.extensions.extraction.gui.config;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.text.BadLocationException;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.FolderChooser;
import jd.gui.swing.jdgui.views.settings.components.Spinner;
import jd.gui.swing.jdgui.views.settings.components.TextArea;
import jd.gui.swing.jdgui.views.settings.components.TextInput;

import org.appwork.app.gui.copycutpaste.CopyAction;
import org.appwork.app.gui.copycutpaste.CutAction;
import org.appwork.app.gui.copycutpaste.DeleteAction;
import org.appwork.app.gui.copycutpaste.PasteAction;
import org.appwork.app.gui.copycutpaste.SelectAction;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.CPUPriority;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.settings.Pair;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class ExtractionConfigPanel extends ExtensionConfigPanel<ExtractionExtension> {
    private static final long         serialVersionUID = 1L;
    private Pair<Checkbox>            toggleCustomizedPath;
    private Pair<FolderChooser>       customPath;
    private Pair<Checkbox>            toggleUseSubpath;
    private Pair<Spinner>             subPathMinFiles;
    private Pair<Checkbox>            toggleUseSubpathOnlyIfNotFoldered;
    private Pair<? extends TextInput> subPath;
    private Pair<Checkbox>            toggleDeleteArchives;
    private Pair<Checkbox>            toggleOverwriteExisting;
    private Pair<TextArea>            blacklist;
    private Pair<Checkbox>            toggleUseOriginalFileDate;
    private Pair<ComboBox<String>>    cpupriority;
    private Pair<TextArea>            passwordlist;
    private Pair<Checkbox>            toggleDeleteArchiveDownloadLinks;
    private Pair<Checkbox>            toggleDefaultEnabled;

    public ExtractionConfigPanel(ExtractionExtension plg) {
        super(plg);
        toggleDefaultEnabled = this.addPair(T._.settings_auto_extract_default(), null, new Checkbox());
        this.addHeader(T._.settings_extractto(), NewTheme.I().getIcon("folder", 32));
        toggleCustomizedPath = this.addPair(T._.settings_extract_to_archive_folder(), null, new Checkbox());
        customPath = this.addPair(T._.settings_extract_to_path(), null, new FolderChooser());
        customPath.setConditionPair(toggleCustomizedPath);
        toggleUseSubpath = this.addPair(T._.settings_use_subpath(), null, new Checkbox());
        Spinner spinner = new Spinner(0, Integer.MAX_VALUE);
        spinner.setFormat("# " + T._.files());
        subPathMinFiles = this.addPair(T._.settings_subpath_minnum2(), null, spinner);
        subPathMinFiles.setConditionPair(toggleUseSubpath);
        toggleUseSubpathOnlyIfNotFoldered = this.addPair(T._.settings_subpath_no_folder2(), null, new Checkbox());
        toggleUseSubpathOnlyIfNotFoldered.setToolTipText(T._.settings_subpath_no_folder_tt());
        toggleUseSubpathOnlyIfNotFoldered.setConditionPair(toggleUseSubpath);
        subPath = this.addPair(T._.settings_subpath(), null, new TextInput() {

            @Override
            public JPopupMenu getPopupMenu(CutAction cutAction, CopyAction copyAction, PasteAction pasteAction, DeleteAction deleteAction, SelectAction selectAction) {
                JPopupMenu menu = new JPopupMenu();
                JMenu sub = new JMenu(T._.properties());
                sub.add(new AppAction() {
                    {
                        setName(T._.packagename());
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (StringUtils.isEmpty(getText())) {
                            setText(ArchiveFactory.PACKAGENAME);
                        } else {
                            int car = getCaretPosition();

                            try {
                                getDocument().insertString(car, ArchiveFactory.PACKAGENAME, null);
                            } catch (BadLocationException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                });
                sub.add(new AppAction() {
                    {
                        setName(T._.archivename());
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (StringUtils.isEmpty(getText())) {
                            setText(ArchiveFactory.ARCHIVENAME);
                        } else {
                            int car = getCaretPosition();

                            try {
                                getDocument().insertString(car, ArchiveFactory.ARCHIVENAME, null);
                            } catch (BadLocationException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                });
                sub.add(new AppAction() {
                    {
                        setName(T._.subfolder());
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (StringUtils.isEmpty(getText())) {
                            setText(ArchiveFactory.SUBFOLDER);
                        } else {
                            int car = getCaretPosition();

                            try {
                                getDocument().insertString(car, ArchiveFactory.SUBFOLDER, null);
                            } catch (BadLocationException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                });

                sub.add(new AppAction() {
                    {
                        setName(T._.hoster());
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (StringUtils.isEmpty(getText())) {
                            setText(ArchiveFactory.HOSTER);
                        } else {
                            int car = getCaretPosition();

                            try {
                                getDocument().insertString(car, ArchiveFactory.HOSTER, null);
                            } catch (BadLocationException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                });

                sub.add(new AppAction() {
                    {
                        setName(T._.date());
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (StringUtils.isEmpty(getText())) {
                            setText("$DATE:dd.MM.yyyy$");
                        } else {
                            int car = getCaretPosition();

                            try {
                                getDocument().insertString(car, "$DATE:dd.MM.yyyy$", null);
                            } catch (BadLocationException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                });
                menu.add(sub);
                menu.add(new JSeparator());
                menu.add(cutAction);
                menu.add(copyAction);
                menu.add(pasteAction);
                menu.add(deleteAction);
                menu.add(selectAction);
                return menu;
            }

        });
        subPath.setConditionPair(toggleUseSubpath);

        this.addHeader(T._.settings_various(), NewTheme.I().getIcon("settings", 32));
        toggleDeleteArchives = this.addPair(T._.settings_remove_after_extract(), null, new Checkbox());
        toggleDeleteArchiveDownloadLinks = this.addPair(T._.settings_remove_after_extract_downloadlink(), null, new Checkbox());
        toggleOverwriteExisting = this.addPair(T._.settings_overwrite(), null, new Checkbox());
        cpupriority = this.addPair(T._.settings_cpupriority(), null, new ComboBox<String>(T._.settings_cpupriority_high(), T._.settings_cpupriority_middle(), T._.settings_cpupriority_low()));

        this.addHeader(T._.settings_multi(), NewTheme.I().getIcon("settings", 32));
        toggleUseOriginalFileDate = this.addPair(T._.settings_multi_use_original_file_date(), null, new Checkbox());
        blacklist = this.addPair(T._.settings_blacklist(), null, new TextArea());

        this.addHeader(T._.settings_passwords(), NewTheme.I().getIcon("password", 32));
        passwordlist = addPair(T._.settings_passwordlist(), null, new TextArea());
    }

    @Override
    public void updateContents() {
        final ExtractionConfig s = extension.getSettings();
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                toggleDefaultEnabled.getComponent().setSelected(CFG_LINKGRABBER.AUTO_EXTRACTION_ENABLED.getValue());
                toggleCustomizedPath.getComponent().setSelected(s.isCustomExtractionPathEnabled());
                String path = s.getCustomExtractionPath();
                if (path == null) path = new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder(), "extracted").getAbsolutePath();
                customPath.getComponent().setText(path);
                toggleDeleteArchives.getComponent().setSelected(s.isDeleteArchiveFilesAfterExtraction());
                toggleDeleteArchiveDownloadLinks.getComponent().setSelected(s.isDeleteArchiveDownloadlinksAfterExtraction());
                toggleOverwriteExisting.getComponent().setSelected(s.isOverwriteExistingFilesEnabled());
                toggleUseSubpath.getComponent().setSelected(s.isSubpathEnabled());
                subPath.getComponent().setText(s.getSubPath());
                subPathMinFiles.getComponent().setValue(s.getSubPathFilesTreshhold());
                toggleUseSubpathOnlyIfNotFoldered.getComponent().setSelected(s.isSubpathEnabledIfAllFilesAreInAFolder());

                StringBuilder sb = new StringBuilder();
                for (String line : s.getBlacklistPatterns()) {
                    if (sb.length() > 0) sb.append(System.getProperty("line.separator"));
                    sb.append(line);
                }
                blacklist.getComponent().setText(sb.toString());
                sb = new StringBuilder();
                java.util.List<String> pwList = s.getPasswordList();
                if (pwList == null) pwList = new ArrayList<String>();
                for (String line : pwList) {
                    if (sb.length() > 0) sb.append(System.getProperty("line.separator"));
                    sb.append(line);
                }
                passwordlist.getComponent().setText(sb.toString());
                if (s.getCPUPriority() == CPUPriority.HIGH) {
                    cpupriority.getComponent().setValue(T._.settings_cpupriority_high());
                } else if (s.getCPUPriority() == CPUPriority.MIDDLE) {
                    cpupriority.getComponent().setValue(T._.settings_cpupriority_middle());
                } else {
                    cpupriority.getComponent().setValue(T._.settings_cpupriority_low());
                }

                toggleUseOriginalFileDate.getComponent().setSelected(s.isUseOriginalFileDate());
            }
        };
    }

    @Override
    public void save() {
        final ExtractionConfig s = extension.getSettings();
        CFG_LINKGRABBER.AUTO_EXTRACTION_ENABLED.setValue(toggleDefaultEnabled.getComponent().isSelected());
        s.setCustomExtractionPathEnabled(toggleCustomizedPath.getComponent().isSelected());
        s.setCustomExtractionPath(customPath.getComponent().getText());
        s.setDeleteArchiveFilesAfterExtraction(toggleDeleteArchives.getComponent().isSelected());
        s.setDeleteArchiveDownloadlinksAfterExtraction(toggleDeleteArchiveDownloadLinks.getComponent().isSelected());
        s.setOverwriteExistingFilesEnabled(toggleOverwriteExisting.getComponent().isSelected());
        s.setSubpathEnabled(toggleUseSubpath.getComponent().isSelected());
        s.setSubPath(subPath.getComponent().getText());
        try {
            s.setSubPathFilesTreshhold((Integer) subPathMinFiles.getComponent().getValue());
        } catch (final Throwable e) {
            Log.exception(e);
        }
        s.setSubpathEnabledIfAllFilesAreInAFolder(toggleUseSubpathOnlyIfNotFoldered.getComponent().isSelected());
        {
            String[] list = Regex.getLines(passwordlist.getComponent().getText());
            java.util.List<String> passwords = new ArrayList<String>(list.length);
            for (String ss : list) {
                if (passwords.contains(ss)) continue;
                passwords.add(ss);
            }
            s.setPasswordList(passwords);
        }
        {
            String[] list = Regex.getLines(blacklist.getComponent().getText());
            java.util.List<String> ignoreList = new ArrayList<String>(list.length);
            for (String ss : list) {
                if (ignoreList.contains(ss)) continue;
                ignoreList.add(ss);
            }
            s.setBlacklistPatterns(ignoreList.toArray(new String[ignoreList.size()]));
        }
        if (cpupriority.getComponent().getValue().equals(T._.settings_cpupriority_high())) {
            s.setCPUPriority(CPUPriority.HIGH);
        } else if (cpupriority.getComponent().getValue().equals(T._.settings_cpupriority_middle())) {
            s.setCPUPriority(CPUPriority.MIDDLE);
        } else {
            s.setCPUPriority(CPUPriority.LOW);
        }

        s.setUseOriginalFileDate(toggleUseOriginalFileDate.getComponent().isSelected());
    }
}
