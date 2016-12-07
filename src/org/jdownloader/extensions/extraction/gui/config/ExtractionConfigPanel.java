package org.jdownloader.extensions.extraction.gui.config;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.text.BadLocationException;

import jd.controlling.TaskQueue;
import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.FolderChooser;
import jd.gui.swing.jdgui.views.settings.components.Spinner;
import jd.gui.swing.jdgui.views.settings.components.TextArea;
import jd.gui.swing.jdgui.views.settings.components.TextInput;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ExtractionConfig;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.multi.Multi;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.Pair;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.IfFileExistsAction;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
import org.jdownloader.utils.JDFileUtils;

public class ExtractionConfigPanel extends ExtensionConfigPanel<ExtractionExtension> {
    private static final long                                serialVersionUID = 1L;
    private Pair<Checkbox>                                   toggleCustomizedPath;
    private Pair<FolderChooser>                              customPath;
    private Pair<Checkbox>                                   toggleUseSubpath;
    private Pair<Spinner>                                    subPathMinFiles;

    private Pair<? extends TextInput>                        subPath;
    private Pair<ComboBox<FileCreationManager.DeleteOption>> toggleDeleteArchives;
    private Pair<ComboBox<IfFileExistsAction>>               toggleOverwriteExisting;
    private Pair<TextArea>                                   blacklist;
    private Pair<Checkbox>                                   toggleUseOriginalFileDate;

    private Pair<TextArea>                                   passwordlist;
    private Pair<Checkbox>                                   toggleDeleteArchiveDownloadLinks;
    private Pair<Checkbox>                                   toggleDefaultEnabled;
    private Pair<Spinner>                                    subPathMinFolders;
    private Pair<Spinner>                                    subPathMinFilesOrFolders;

    @Override
    protected String getHeaderName(ExtractionExtension plg) {
        return super.getHeaderName(plg) + ": (7Zip Binding Version: " + Multi.getSevenZipJBindingVersion() + ")";
    }

    public ExtractionConfigPanel(ExtractionExtension plg) {
        super(plg);
        toggleDefaultEnabled = this.addPair(T.T.settings_auto_extract_default(), null, new Checkbox());
        this.addHeader(T.T.settings_extractto(), new AbstractIcon(IconKey.ICON_FOLDER, 32));
        toggleCustomizedPath = this.addPair(T.T.settings_extract_to_archive_folder(), null, new Checkbox());
        customPath = this.addPair(T.T.settings_extract_to_path(), null, new FolderChooser());
        customPath.setConditionPair(toggleCustomizedPath);
        toggleUseSubpath = this.addPair(T.T.settings_use_subpath(), null, new Checkbox());
        Spinner spinner = new Spinner(0, Integer.MAX_VALUE);
        spinner.setFormat("# " + T.T.files2());
        // ((DefaultEditor) spinner.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
        String lblConstraints = "gapleft 37,aligny center,alignx right";

        subPathMinFiles = this.addPair(T.T.settings_subpath_minnum3(), lblConstraints, null, spinner);
        subPathMinFiles.setConditionPair(toggleUseSubpath);
        Spinner spinner2 = new Spinner(0, Integer.MAX_VALUE);
        spinner2.setFormat("# " + T.T.folders());

        subPathMinFolders = this.addPair(T.T.and(), lblConstraints, null, spinner2);
        subPathMinFolders.setConditionPair(toggleUseSubpath);

        Spinner spinner3 = new Spinner(0, Integer.MAX_VALUE);
        spinner3.setFormat("# " + T.T.files_and_folders());

        subPathMinFilesOrFolders = this.addPair(T.T.and(), lblConstraints, null, spinner3);
        subPathMinFilesOrFolders.setConditionPair(toggleUseSubpath);

        subPath = this.addPair(T.T.settings_subpath(), null, new TextInput() {

            @Override
            public JPopupMenu getPopupMenu(AbstractAction cutAction, AbstractAction copyAction, AbstractAction pasteAction, AbstractAction deleteAction, AbstractAction selectAction) {
                JPopupMenu menu = new JPopupMenu();
                JMenu sub = new JMenu(T.T.properties());
                sub.add(new AppAction() {
                    {
                        setName(T.T.packagename());
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
                        setName(T.T.archivename());
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
                        setName(T.T.subfolder());
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
                        setName(T.T.hoster());
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
                        setName(T.T.date());
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

        this.addHeader(T.T.settings_various(), new AbstractIcon(IconKey.ICON_SETTINGS, 32));
        if (JDFileUtils.isTrashSupported()) {
            toggleDeleteArchives = this.addPair(T.T.settings_remove_after_extract(), null, (ComboBox<FileCreationManager.DeleteOption>) new ComboBox<FileCreationManager.DeleteOption>(FileCreationManager.DeleteOption.NO_DELETE, FileCreationManager.DeleteOption.RECYCLE, FileCreationManager.DeleteOption.NULL) {
                protected String valueToString(FileCreationManager.DeleteOption value) {
                    switch (value) {
                    case RECYCLE:
                        return T.T.delete_to_trash();
                    case NO_DELETE:
                        return T.T.dont_delete();
                    case NULL:
                        return T.T.final_delete();
                    }
                    return null;
                }
            });
        } else {
            toggleDeleteArchives = this.addPair(T.T.settings_remove_after_extract(), null, (ComboBox<FileCreationManager.DeleteOption>) new ComboBox<FileCreationManager.DeleteOption>(FileCreationManager.DeleteOption.NO_DELETE, FileCreationManager.DeleteOption.NULL) {
                protected String valueToString(FileCreationManager.DeleteOption value) {
                    switch (value) {
                    case RECYCLE:
                        return T.T.delete_to_trash();
                    case NO_DELETE:
                        return T.T.dont_delete();
                    case NULL:
                        return T.T.final_delete();
                    }
                    return null;
                }
            });
        }

        toggleDeleteArchiveDownloadLinks = this.addPair(T.T.settings_remove_after_extract_downloadlink(), null, new Checkbox());
        toggleOverwriteExisting = this.addPair(T.T.settings_if_file_exists(), null, new ComboBox<IfFileExistsAction>(IfFileExistsAction.values()));

        this.addHeader(T.T.settings_multi(), new AbstractIcon(IconKey.ICON_SETTINGS, 32));
        toggleUseOriginalFileDate = this.addPair(T.T.settings_multi_use_original_file_date(), null, new Checkbox());
        blacklist = this.addPair(T.T.settings_blacklist_regex(), null, new TextArea());

        this.addHeader(T.T.settings_passwords(), new AbstractIcon(IconKey.ICON_PASSWORD, 32));
        passwordlist = addPair(T.T.settings_passwordlist(), null, new TextArea());
    }

    @Override
    public void updateContents() {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {

            @Override
            protected Void run() throws RuntimeException {
                final ExtractionConfig s = extension.getSettings();
                String path = s.getCustomExtractionPath();
                if (path == null) {
                    path = new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder(), "extracted").getAbsolutePath();
                }
                final String finalPath = path;
                final List<String> pwList = s.getPasswordList();
                final String pwListString;
                if (pwList == null || pwList.size() == 0) {
                    pwListString = "";
                } else {
                    if (pwList.size() == 1) {
                        pwListString = pwList.get(0);
                    } else {
                        final StringBuilder sb = new StringBuilder();
                        final String separator = System.getProperty("line.separator");
                        for (final String pw : pwList) {
                            sb.append(pw);
                            sb.append(separator);
                        }
                        pwListString = sb.toString();
                    }
                }
                final String[] blackListPatterns = s.getBlacklistPatterns();
                final String blackListPatternsString;
                if (blackListPatterns == null || blackListPatterns.length == 0) {
                    blackListPatternsString = "";
                } else {
                    if (blackListPatterns.length == 1) {
                        blackListPatternsString = blackListPatterns[0];
                    } else {
                        final StringBuilder sb = new StringBuilder();
                        final String separator = System.getProperty("line.separator");
                        for (final String pw : blackListPatterns) {
                            sb.append(pw);
                            sb.append(separator);
                        }
                        blackListPatternsString = sb.toString();
                    }
                }
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        toggleDefaultEnabled.getComponent().setSelected(CFG_LINKGRABBER.AUTO_EXTRACTION_ENABLED.isEnabled());
                        toggleCustomizedPath.getComponent().setSelected(s.isCustomExtractionPathEnabled());
                        customPath.getComponent().setText(finalPath);
                        toggleDeleteArchives.getComponent().setSelectedItem(s.getDeleteArchiveFilesAfterExtractionAction());
                        toggleDeleteArchiveDownloadLinks.getComponent().setSelected(s.isDeleteArchiveDownloadlinksAfterExtraction());
                        toggleOverwriteExisting.getComponent().setSelectedItem(s.getIfFileExistsAction());
                        toggleUseSubpath.getComponent().setSelected(s.isSubpathEnabled());
                        subPath.getComponent().setText(s.getSubPath());
                        subPathMinFiles.getComponent().setValue(s.getSubPathMinFilesTreshhold());
                        subPathMinFolders.getComponent().setValue(s.getSubPathMinFoldersTreshhold());
                        subPathMinFilesOrFolders.getComponent().setValue(s.getSubPathMinFilesOrFoldersTreshhold());
                        blacklist.getComponent().setText(blackListPatternsString);
                        passwordlist.getComponent().setText(pwListString);
                        toggleUseOriginalFileDate.getComponent().setSelected(s.isUseOriginalFileDate());
                    }
                }.waitForEDT();
                return null;
            }
        });
    }

    @Override
    public void save() {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final ExtractionConfig s = extension.getSettings();
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        CFG_LINKGRABBER.AUTO_EXTRACTION_ENABLED.setValue(toggleDefaultEnabled.getComponent().isSelected());
                        s.setCustomExtractionPathEnabled(toggleCustomizedPath.getComponent().isSelected());
                        s.setCustomExtractionPath(customPath.getComponent().getText());
                        s.setDeleteArchiveFilesAfterExtractionAction(toggleDeleteArchives.getComponent().getSelectedItem());
                        s.setDeleteArchiveDownloadlinksAfterExtraction(toggleDeleteArchiveDownloadLinks.getComponent().isSelected());
                        s.setIfFileExistsAction(toggleOverwriteExisting.getComponent().getSelectedItem());
                        s.setSubpathEnabled(toggleUseSubpath.getComponent().isSelected());
                        s.setSubPath(subPath.getComponent().getText());
                        try {
                            s.setSubPathMinFilesTreshhold((Integer) subPathMinFiles.getComponent().getValue());
                        } catch (final Throwable e) {
                            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                        }
                        try {
                            s.setSubPathMinFoldersTreshhold((Integer) subPathMinFolders.getComponent().getValue());
                        } catch (final Throwable e) {
                            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                        }
                        try {
                            s.setSubPathMinFilesOrFoldersTreshhold((Integer) subPathMinFilesOrFolders.getComponent().getValue());
                        } catch (final Throwable e) {
                            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                        }
                        s.setUseOriginalFileDate(toggleUseOriginalFileDate.getComponent().isSelected());
                    }
                };
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        final String txt = new EDTHelper<String>() {

                            @Override
                            public String edtRun() {
                                return passwordlist.getComponent().getText();
                            }
                        }.getReturnValue();
                        final String[] list = txt.split("[\r\n]{1,2}");
                        final LinkedHashSet<String> passwords = new LinkedHashSet<String>();
                        for (final String pw : list) {
                            if (StringUtils.isNotEmpty(pw)) {
                                passwords.add(pw);
                            }
                        }
                        s.setPasswordList(new ArrayList<String>(passwords));
                        return null;
                    }
                });
                TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        final String txt = new EDTHelper<String>() {

                            @Override
                            public String edtRun() {
                                return blacklist.getComponent().getText();
                            }
                        }.getReturnValue();
                        final String[] list = Regex.getLines(txt);
                        final LinkedHashSet<String> ignoreList = new LinkedHashSet<String>();
                        for (String ss : list) {
                            ignoreList.add(ss);
                        }
                        s.setBlacklistPatterns(ignoreList.toArray(new String[ignoreList.size()]));
                        return null;
                    }
                });
                return null;
            }

        });

    }

}
