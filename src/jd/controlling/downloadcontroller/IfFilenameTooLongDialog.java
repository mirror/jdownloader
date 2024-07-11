package jd.controlling.downloadcontroller;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.SIZEUNIT;
import org.jdownloader.settings.IfFilenameTooLongAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.plugins.DownloadLink;

public class IfFilenameTooLongDialog extends AbstractDialog<IfFilenameTooLongAction> implements IfFilenameTooLongDialogInterface, FocusListener {
    private final String            path;
    private IfFilenameTooLongAction result;
    private final String            packagename;

    @Override
    public boolean isRemoteAPIEnabled() {
        return true;
    }

    public String getPackagename() {
        return packagename;
    }

    public String getPackageID() {
        return packageID;
    }

    private JRadioButton       skip;
    private JRadioButton       overwrite;
    private JRadioButton       autoShorten;
    private final String       packageID;
    private final DownloadLink downloadLink;
    private final String       autoShortenedFilenameSuggestion;

    public IfFilenameTooLongDialog(final DownloadLink link, final String autoShortenedFilenameSuggestion) {
        super(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_COUNTDOWN, "Filename is too long", null, null, null);
        this.packagename = link.getFilePackage().getName();
        this.packageID = link.getFilePackage().getName() + "_" + link.getFilePackage().getCreated();
        this.path = link.getFileOutput();
        this.downloadLink = link;
        this.autoShortenedFilenameSuggestion = autoShortenedFilenameSuggestion;
        setTimeout(60000);
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
    protected IfFilenameTooLongAction createReturnValue() {
        if (okButton != null) {
            okButton.removeFocusListener(this);
        }
        // TODO: Set last selected value so that "do not ask again" dialog will work fine
        // if (result != null) {
        // org.jdownloader.settings.staticreferences.CFG_GUI.CFG.setLastIfFileExists(result);
        // }
        return result;
    }

    protected String getDontShowAgainLabelText() {
        return _GUI.T.IfFileExistsDialog_getDontShowAgainLabelText_();
    }

    @Override
    public JComponent layoutDialogContent() {
        final MigPanel p = new MigPanel("ins 0,wrap 1", "", "");
        final ExtTextArea txt = new ExtTextArea();
        txt.setLabelMode(true);
        txt.setToolTipText(path);
        txt.setText("The name of this file is too long to write it to your filesystem.\r\nJDownloader can automatically shorten it, you can shorten it yourself, skip it or update your OS' settings to allow longer filenames.");
        p.add(txt);
        final JTextField textfieldFilenameOld = new JTextField(this.downloadLink.getName());
        final JTextField textfieldFilenameNew = new JTextField(this.autoShortenedFilenameSuggestion);
        final JTextField textfieldPackagename = new JTextField(packagename);
        p.add(SwingUtils.toBold(new JLabel("Current filename:")), "split 2,sg 1");
        // p.add(new JLabel(this.downloadLink.getName()));
        p.add(textfieldFilenameOld);
        p.add(SwingUtils.toBold(new JLabel("Auto shortened filename:")), "split 2,sg 1");
        p.add(new JLabel(this.autoShortenedFilenameSuggestion));
        p.add(textfieldFilenameNew);
        p.add(SwingUtils.toBold(new JLabel("Filesize:")), "split 2,sg 1");
        final SIZEUNIT maxSizeUnit = (SIZEUNIT) CFG_GUI.MAX_SIZE_UNIT.getValue();
        p.add(new JLabel(SIZEUNIT.formatValue(maxSizeUnit, this.downloadLink.getDownloadSize())));
        p.add(SwingUtils.toBold(new JLabel(_GUI.T.IfFileExistsDialog_layoutDialogContent_package())), "split 2,sg 1");
        p.add(new JLabel(packagename));
        p.add(textfieldPackagename);
        p.add(SwingUtils.toBold(new JLabel(_GUI.T.IfFileExistsDialog_layoutDialogContent_hoster())), "split 2,sg 1");
        p.add(new JLabel(downloadLink.getDomainInfo().getTld()));
        skip = new JRadioButton(_GUI.T.IfFileExistsDialog_layoutDialogContent_skip_());
        skip.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                result = IfFilenameTooLongAction.SKIP_FILE;
            }
        });
        overwrite = new JRadioButton(_GUI.T.IfFileExistsDialog_layoutDialogContent_overwrite_());
        overwrite.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                result = IfFilenameTooLongAction.OVERWRITE_FILE;
            }
        });
        autoShorten = new JRadioButton("Auto shorten filename");
        autoShorten.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                result = IfFilenameTooLongAction.AUTO_SHORTEN;
            }
        });
        // Group the radio buttons.
        final ButtonGroup group = new ButtonGroup();
        group.add(skip);
        group.add(overwrite);
        group.add(autoShorten);
        p.add(new JSeparator(), "pushx,growx");
        p.add(skip, "gapleft 10");
        p.add(overwrite, "gapleft 10");
        p.add(autoShorten, "gapleft 10");
        // TODO: Update this so last selection is correctly used as current default
        // IfFileExistsAction def = org.jdownloader.settings.staticreferences.CFG_GUI.CFG.getLastIfFileExists();
        IfFilenameTooLongAction def = null;
        if (def == null) {
            def = IfFilenameTooLongAction.SKIP_FILE;
        }
        switch (def) {
        case AUTO_SHORTEN:
            autoShorten.setSelected(true);
            break;
        case OVERWRITE_FILE:
            overwrite.setSelected(true);
            break;
        default:
            skip.setSelected(true);
        }
        result = def;
        if (okButton != null) {
            okButton.addFocusListener(this);
        }
        return p;
    }

    public IfFilenameTooLongAction getAction() {
        return result;
    }

    public String getFilePath() {
        return path;
    }

    public IfFilenameTooLongDialogInterface show() {
        return UIOManager.I().show(IfFilenameTooLongDialogInterface.class, this);
    }

    @Override
    public void focusGained(FocusEvent e) {
        DownloadsTableModel.getInstance().setSelectedObject(downloadLink);
    }

    @Override
    public void focusLost(FocusEvent e) {
    }

    @Override
    public String getHost() {
        return downloadLink.getHost();
    }
}
