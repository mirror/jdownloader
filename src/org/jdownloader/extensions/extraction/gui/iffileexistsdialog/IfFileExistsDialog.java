package org.jdownloader.extensions.extraction.gui.iffileexistsdialog;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import jd.controlling.downloadcontroller.IfFileExistsDialogInterface;
import jd.plugins.DownloadLink;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.CFG_EXTRACTION;
import org.jdownloader.extensions.extraction.Item;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.IfFileExistsAction;
import org.jdownloader.translate._JDT;

public class IfFileExistsDialog extends AbstractDialog<IfFileExistsAction> implements IfFileExistsDialogInterface, FocusListener {

    private String             path;
    private IfFileExistsAction result;
    private String             packagename;

    public String getPackagename() {
        return packagename;
    }

    public String getPackageID() {
        return packageID;
    }

    private JRadioButton skip;
    private JRadioButton overwrite;
    private JRadioButton rename;
    private String       packageID;
    private DownloadLink downloadLink;
    private Archive      archive;
    private Item         item;
    private LogSource    logger;
    private JTextField   newName;
    private String       newNameString;

    public IfFileExistsDialog(File extractTo, Item item, Archive archive) {
        super(0, _JDT._.jd_controlling_SingleDownloadController_askexists_title(), null, null, null);
        //
        logger = LogController.getInstance().getLogger(IfFileExistsDialog.class.getName());
        this.archive = archive;
        for (ArchiveFile af : archive.getArchiveFiles()) {
            if (af instanceof DownloadLinkArchiveFile) {
                downloadLink = ((DownloadLinkArchiveFile) af).getDownloadLinks().get(0);
                this.packagename = downloadLink.getFilePackage().getName();
                this.packageID = downloadLink.getFilePackage().getName() + "_" + downloadLink.getFilePackage().getCreated();
                break;
            }
        }
        this.item = item;
        this.path = extractTo.getAbsolutePath();

        String extension = Files.getExtension(extractTo.getName());
        String name = StringUtils.isEmpty(extension) ? extractTo.getName() : extractTo.getName().substring(0, extractTo.getName().length() - extension.length() - 1);
        int i = 1;
        while (extractTo.exists()) {
            if (StringUtils.isEmpty(extension)) {
                extractTo = new File(extractTo.getParentFile(), name + "_" + i);

            } else {
                extractTo = new File(extractTo.getParentFile(), name + "_" + i + "." + extension);

            }
            i++;
        }

        newNameString = extractTo.getName();

        // setTimeout(60000);
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    @Override
    public String getDontShowAgainKey() {
        // returning null causes the dialog to show a checkbox, but the dialog itself does not handle the results
        return null;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (okButton != null) okButton.removeFocusListener(this);
        if (result != null) CFG_EXTRACTION.CFG.setLatestIfFileExistsAction(result);

    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 2", "[][grow,fill]", "[]");
        ExtTextArea txt = new ExtTextArea();
        txt.setLabelMode(true);
        txt.setToolTipText(path);
        File localFile = new File(path);

        txt.setText(T._.file_exists_message());
        p.add(txt, "spanx");
        p.add(SwingUtils.toBold(new JLabel(_GUI._.IfFileExistsDialog_layoutDialogContent_filename())), "sg 1");
        p.add(new JLabel(new File(path).getName()));
        if (item != null) {
            p.add(SwingUtils.toBold(new JLabel(_GUI._.IfFileExistsDialog_layoutDialogContent_filesize2())), "sg 1");
            p.add(new JLabel(SizeFormatter.formatBytes(item.getSize())));
        }
        p.add(SwingUtils.toBold(new JLabel(_GUI._.IfFileExistsDialog_layoutDialogContent_filesize_existing())), "sg 1");
        p.add(new JLabel(SizeFormatter.formatBytes(localFile.length())));

        if (packagename != null) {
            p.add(SwingUtils.toBold(new JLabel(_GUI._.IfFileExistsDialog_layoutDialogContent_package())), "sg 1");
            p.add(new JLabel(packagename));
        }

        p.add(SwingUtils.toBold(new JLabel(T._.IfFileExistsDialog_layoutDialogContent_archive())), "sg 1");
        p.add(new JLabel(archive.getName()));

        skip = new JRadioButton(_GUI._.IfFileExistsDialog_layoutDialogContent_skip_());
        skip.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                result = IfFileExistsAction.SKIP_FILE;
                newName.setEnabled(false);
            }
        });
        overwrite = new JRadioButton(_GUI._.IfFileExistsDialog_layoutDialogContent_overwrite_());
        overwrite.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                result = IfFileExistsAction.OVERWRITE_FILE;
                newName.setEnabled(false);
            }
        });
        rename = new JRadioButton(_GUI._.IfFileExistsDialog_layoutDialogContent_rename_());
        rename.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                result = IfFileExistsAction.AUTO_RENAME;
                newName.setEnabled(true);
            }
        });

        // Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(skip);
        group.add(overwrite);
        group.add(rename);

        p.add(new JSeparator(), "spanx,pushx,growx");
        p.add(skip, "skip 1");
        p.add(overwrite, "skip 1");
        p.add(rename, "skip 1");
        newName = new JTextField(newNameString);
        p.add(SwingUtils.toBold(new JLabel(T._.IfFileExistsDialog_layoutDialogContent_newName())), "sg 1");
        p.add(newName);

        IfFileExistsAction def = CFG_EXTRACTION.CFG.getLatestIfFileExistsAction();
        if (def == null) def = IfFileExistsAction.SKIP_FILE;
        switch (def) {
        case AUTO_RENAME:
            rename.setSelected(true);
            break;

        case OVERWRITE_FILE:
            overwrite.setSelected(true);
            break;
        default:

            skip.setSelected(true);
        }
        newName.setEnabled(rename.isSelected());
        result = def;
        if (okButton != null) {
            okButton.addFocusListener(this);
        }
        return p;
    }

    public String getNewName() {
        return newName.getText();
    }

    public IfFileExistsAction getAction() {
        return result;
    }

    public String getFilePath() {
        return path;
    }

    public IfFileExistsDialogInterface show() {

        return UIOManager.I().show(IfFileExistsDialogInterface.class, this);
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (downloadLink != null) {
            DownloadsTableModel.getInstance().setSelectedObject(downloadLink);
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
    }

    @Override
    protected IfFileExistsAction createReturnValue() {
        return null;
    }

    @Override
    public String getHost() {
        return downloadLink.getHost();
    }
}
