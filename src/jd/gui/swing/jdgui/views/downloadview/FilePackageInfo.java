//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views.downloadview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import jd.config.Configuration;
import jd.controlling.DownloadController;
import jd.controlling.PasswordListController;
import jd.gui.swing.components.ComboBrowseFile;
import jd.gui.swing.components.JDCollapser;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.swing.components.JDTextField;
import jd.gui.swing.components.MultiProgressBar;
import jd.gui.swing.jdgui.views.DownloadView;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class FilePackageInfo extends JDCollapser implements ActionListener, FocusListener {

    private static final long serialVersionUID = 5410296068527460629L;

    private ComboBrowseFile brwSaveTo;

    private JDTextField txtComment;
    private JDTextField txtCommentDl;
    private JDTextField txtName;
    private JDTextField txtNameDl;
    private JDTextField txtPasswordDl;
    private JDTextField txtPassword;
    private JDTextField txtStatusDl;
    private JDTextField txtSize;
    private JDTextField txtSizeDl;
    private JDTextField txtURL;
    private JDTextField txtPathLabel;

    private JCheckBox chbExtract;

    private JTabbedPane tabbedPane;

    private FilePackage fp = null;

    private boolean hidden = false;

    private JCheckBox chbUseSubdirectory;

    private JPanel panel;

    private MultiProgressBar progressBarFilePackage;

    private JLabel hosterlabel;

    private DownloadLink downloadLink;

    private transient Thread updater;

    private MultiProgressBar progressBarDownloadLink;

    private JLabel typeicon;

    private JLabel eta;

    private JLabel speed;

    public FilePackageInfo() {
        super();
        buildGui();
        fp = null;
        this.menutitle.setText(JDL.L("gui.linkgrabber.packagetab.title", "FilePackage"));
    }

    public void setPackage(FilePackage fp) {

        this.tabbedPane.setEnabledAt(1, false);
        if (this.fp != null && this.fp == fp) {
            update();
            return;
        }
        this.fp = fp;
        this.tabbedPane.setSelectedIndex(0);
        if (this.fp != null) {
            update();
        }
    }

    public void update() {
        if (fp == null) return;
        /*
         * wichtig: die set funktionen lösen eine action aus , welche ansonsten
         * wiederum ein updatevent aufrufen würden
         */

        txtSize.setText(Formatter.formatReadable(fp.getTotalEstimatedPackageSize()));
        txtName.setText(fp.getName());
        txtComment.setText(fp.getComment());
        txtPassword.setText(fp.getPassword());
        brwSaveTo.setText(fp.getDownloadDirectory());
        chbExtract.setSelected(fp.isExtractAfterDownload());
        /* neuzeichnen */
        revalidate();

    }

    public FilePackage getPackage() {
        return fp;
    }

    private void buildGui() {
        tabbedPane = new JTabbedPane();
        tabbedPane.add(createFilePackageInfo(), JDL.L("gui.fileinfopanel.packagetab", "Package"));
        tabbedPane.add(createLinkInfo(), JDL.L("gui.fileinfopanel.link", "Downloadlink"));
        content.setLayout(new MigLayout("", "[grow]", "[]"));
        content.add(tabbedPane, "grow");
    }

    private JPanel createFilePackageInfo() {
        txtName = new JDTextField(true);
        txtName.addActionListener(this);
        txtName.addFocusListener(this);
        brwSaveTo = new ComboBrowseFile("DownloadSaveTo");
        brwSaveTo.setEditable(true);
        brwSaveTo.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
        brwSaveTo.setText(JDUtilities.getConfiguration().getDefaultDownloadDirectory());
        brwSaveTo.addActionListener(this);
        brwSaveTo.addFocusListener(this);
        txtPassword = new JDTextField(true);
        txtPassword.addActionListener(this);
        txtPassword.addFocusListener(this);
        txtComment = new JDTextField(true);
        txtComment.addActionListener(this);
        txtComment.addFocusListener(this);
        chbExtract = new JCheckBox(JDL.L("gui.fileinfopanel.packagetab.chb.extractAfterdownload", "Extract"));
        chbExtract.setSelected(true);
        chbExtract.setHorizontalTextPosition(SwingConstants.LEFT);
        chbExtract.addActionListener(this);
        chbExtract.addFocusListener(this);
        chbUseSubdirectory = new JCheckBox(JDL.L("gui.linkgrabber.packagetab.chb.useSubdirectory", "Use Subdirectory"));
        chbUseSubdirectory.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false));
        chbUseSubdirectory.setHorizontalTextPosition(SwingConstants.LEFT);
        chbUseSubdirectory.addActionListener(this);

        progressBarFilePackage = new MultiProgressBar();
        panel = new JPanel();
        panel.setLayout(new MigLayout("ins 10, wrap 3,debug", "[]10[grow,fill][]", "[]5[]5[]5[]"));
        panel.add(progressBarFilePackage, "spanx,growx,pushx,split 2,height 18!");
        panel.add(txtSize = new JDTextField(true), "alignx right");
        txtSize.setEditable(false);
        txtSize.setBorder(null);
        txtSize.setOpaque(false);
        txtSize.putClientProperty("Synthetica.opaque", Boolean.FALSE);

        panel.add(new JLabel(JDL.L("gui.fileinfopanel.packagetab.lbl.name", "Paketname")));
        panel.add(txtName, "span 2,growx,spanx");
        panel.add(new JLabel(JDL.L("gui.fileinfopanel.packagetab.lbl.saveto", "Speichern unter")));
        panel.add(brwSaveTo.getInput(), "gapright 10,shrinkx");
        panel.add(brwSaveTo.getButton(), "pushx,growx");
        panel.add(new JLabel(JDL.L("gui.fileinfopanel.packagetab.lbl.password", "Archivpasswort")));
        panel.add(txtPassword, " gapright 10, growx");
        panel.add(chbExtract, "alignx right");
        panel.add(new JLabel(JDL.L("gui.fileinfopanel.packagetab.lbl.comment", "Kommentar")));
        panel.add(txtComment, "gapright 10, growx");
        panel.add(chbUseSubdirectory, "alignx right");
        return panel;
    }

    private JPanel createLinkInfo() {
        progressBarDownloadLink = new MultiProgressBar();
        panel = new JPanel();
        panel.setLayout(new MigLayout("ins 10, wrap 3", "[]10[grow,fill][]", "[]5[]5[]5[]"));
        panel.add(hosterlabel = new JLabel(JDTheme.II("gui.images.sort", 16, 16)), "split 2");
        panel.add(typeicon = new JLabel(JDTheme.II("gui.images.sort", 16, 16)), "");
        typeicon.setText(JDL.L("gui.fileinfopanel.linktab.chunks", "Chunks"));
        panel.add(progressBarDownloadLink, "spanx,growx,pushx,split 2,height 18!");
        panel.add(txtSizeDl = new JDTextField(true), "alignx right");
        txtSizeDl.setOpaque(false);
        txtSizeDl.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        txtSizeDl.setEditable(false);
        txtSizeDl.setBorder(null);
        panel.add(eta = new JLabel(JDL.LF("gui.fileinfopanel.linktab.eta", "ETA: %s mm:ss", "0")));
        panel.add(speed = new JLabel(JDL.LF("gui.fileinfopanel.linktab.speed", "Speed: %s/s", "0 kb")), "skip,alignx right");
        panel.add(new JLabel(JDL.L("gui.fileinfopanel.linktab.name", "Linkname")));
        panel.add(txtNameDl = new JDTextField(true), "growx, span 2");
        txtNameDl.setEditable(false);
        panel.add(new JLabel(JDL.L("gui.fileinfopanel.linktab.saveto", "Save to")));
        panel.add(txtPathLabel = new JDTextField(true), "growx, span 2");
        txtPathLabel.setEditable(false);
        panel.add(new JLabel(JDL.L("gui.fileinfopanel.linktab.url", "URL")));
        panel.add(txtURL = new JDTextField(true), "growx, span 2");
        txtURL.setEditable(false);
        panel.add(new JLabel(JDL.L("gui.fileinfopanel.linktab.comment", "Comment")));
        panel.add(txtCommentDl = new JDTextField(true), "growx, span 2");
        txtCommentDl.setEditable(false);
        panel.add(new JLabel(JDL.L("gui.fileinfopanel.linktab.password", "Password")));
        panel.add(txtPasswordDl = new JDTextField(true), "growx, span 2");
        txtPasswordDl.setEditable(false);
        panel.add(new JLabel(JDL.L("gui.fileinfopanel.linktab.status", "Status")));
        panel.add(txtStatusDl = new JDTextField(true), "growx, span 2");
        txtStatusDl.setEditable(false);

        return panel;
    }

    public void actionPerformed(ActionEvent e) {
        if (fp == null) return;
        if (e.getSource() == txtName) {
            fp.setName(txtName.getText());
        } else if (e.getSource() == brwSaveTo) {
            fp.setDownloadDirectory(brwSaveTo.getText());
        } else if (e.getSource() == txtComment) {
            fp.setComment(txtComment.getText());
        } else if (e.getSource() == txtPassword) {
            fp.setPassword(txtPassword.getText());
        } else if (e.getSource() == chbExtract) {
            fp.setExtractAfterDownload(chbExtract.isSelected());
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(fp.get(0));
    }

    // @Override
    public void onShow() {
        update();
        hidden = false;
        if (updater != null && updater.isAlive()) return;
        updater = new Thread() {
            public void run() {
                try {
                    while (true && !hidden) {
                        progressBarFilePackage.setMaximums(null);
                        progressBarDownloadLink.setMaximums(null);
                        if (fp != null) {
                            long[] max = new long[fp.getDownloadLinkList().size()];
                            long[] values = new long[fp.getDownloadLinkList().size()];
                            int i = 0;
                            for (DownloadLink dl : fp.getDownloadLinkList()) {
                                max[i] = Math.max(1024, dl.getDownloadSize());
                                values[i] = Math.max(1, dl.getDownloadCurrent());
                                i++;
                            }
                            FilePackageInfo.this.progressBarFilePackage.setMaximums(max);
                            FilePackageInfo.this.progressBarFilePackage.setValues(values);
                        }
                        if (downloadLink != null) {
                            if (downloadLink.getChunksProgress() != null) {
                                long fileSize = downloadLink.getDownloadSize();
                                int chunks = downloadLink.getChunksProgress().length;
                                long part = fileSize / chunks;

                                long[] max = new long[chunks];
                                long[] values = new long[chunks];
                                for (int i = 0; i < chunks; i++) {
                                    max[i] = part;
                                    values[i] = (downloadLink.getChunksProgress()[i]+1) - i * part;
                                }
max[chunks-1]=fileSize-part*(chunks-1);
values[chunks-1] = (downloadLink.getChunksProgress()[chunks-1]+1) - part*(chunks-1);
                                FilePackageInfo.this.progressBarDownloadLink.setMaximums(max);
                                FilePackageInfo.this.progressBarDownloadLink.setValues(values);
                            } else {
                                FilePackageInfo.this.progressBarDownloadLink.setMaximums(new long[] { 10 });
                                FilePackageInfo.this.progressBarDownloadLink.setValues(new long[] { 0 });
                            }
                            txtNameDl.setText(downloadLink.getName());
                            if (downloadLink.getSourcePluginComment() != null && downloadLink.getSourcePluginComment().trim().length() > 0) {
                                txtCommentDl.setText(downloadLink.getSourcePluginComment());
                            } else {
                                txtCommentDl.setText(downloadLink.getFilePackage().getComment());
                            }
                            if (downloadLink.getSourcePluginPasswordList() != null && downloadLink.getSourcePluginPasswordList().size() > 0) {
                                txtPasswordDl.setText(downloadLink.getSourcePluginPasswordList().toString());
                            } else {
                                txtPasswordDl.setText(downloadLink.getFilePackage().getPassword());
                            }
                            txtStatusDl.setText(downloadLink.getLinkStatus().getStatusString());

                            if (downloadLink.getDownloadSpeed() <= 0) {
                                eta.setVisible(false);
                                speed.setVisible(false);
                            } else {
                                eta.setText(JDL.LF("gui.fileinfopanel.linktab.eta2", "ETA: %s", Formatter.formatSeconds((downloadLink.getDownloadSize() - downloadLink.getDownloadCurrent()) / downloadLink.getDownloadSpeed())));
                                speed.setText(JDL.LF("gui.fileinfopanel.linktab.speed", "Speed: %s/s", Formatter.formatReadable(Math.max(0, downloadLink.getDownloadSpeed()))));
                                speed.setVisible(true);
                                eta.setVisible(true);
                            }
                        }
                        revalidate();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                } catch (Exception e) {
                }
            }
        };
        updater.start();
    }

    public void onHideSave() {

        PasswordListController.getInstance().addPassword(txtPassword.getText());
        fp.setName(txtName.getText());
        fp.setComment(txtComment.getText());
        fp.setPassword(txtPassword.getText());
        fp.setDownloadDirectory(brwSaveTo.getText());

    }

    // @Override
    public void onHide() {
        if (fp == null && downloadLink == null) return;
        onHideSave();
        hidden = true;
        if (updater != null) {
            updater.interrupt();
            updater = null;
        }
        this.progressBarFilePackage.setMaximums(null);
        this.progressBarDownloadLink.setMaximums(null);
        downloadLink = null;
        fp = null;
    }

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(DownloadLink downloadLink) {
        this.tabbedPane.setEnabledAt(1, true);
        this.tabbedPane.setSelectedIndex(1);
        if (this.downloadLink != null && this.downloadLink == downloadLink) { return; }
        this.downloadLink = downloadLink;
        if (downloadLink != null) {
            fp = downloadLink.getFilePackage();
            this.txtPathLabel.setText(downloadLink.getFileOutput());
            this.typeicon.setIcon(downloadLink.getIcon());
            if (downloadLink.getPlugin() != null && downloadLink.getPlugin().hasHosterIcon()) {
                this.hosterlabel.setIcon(downloadLink.getPlugin().getHosterIcon());
            } else {
                this.hosterlabel.setIcon(null);
            }

            hosterlabel.setToolTipText(downloadLink.getHost());
            typeicon.setToolTipText(downloadLink.getHost());
            this.txtSizeDl.setText(Formatter.formatReadable(downloadLink.getDownloadSize()));
            if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_NORMAL) {
                this.txtURL.setText(downloadLink.getBrowserUrl());
            } else {
                this.txtURL.setText("**************");
            }
        }
        if (this.fp != null) {
            update();
        }

    }

    @Override
    public void onClosed() {
        DownloadView.getInstance().setInfoPanel(null);

    }

    public void focusGained(FocusEvent e) {
        // TODO Auto-generated method stub

    }

    public void focusLost(FocusEvent e) {
        if (fp == null) return;
        if (e.getSource() == txtName) {
            fp.setName(txtName.getText());
        } else if (e.getSource() == brwSaveTo) {
            fp.setDownloadDirectory(brwSaveTo.getText());
        } else if (e.getSource() == txtComment) {
            fp.setComment(txtComment.getText());
        } else if (e.getSource() == txtPassword) {
            fp.setPassword(txtPassword.getText());
        } else if (e.getSource() == chbExtract) {
            fp.setExtractAfterDownload(chbExtract.isSelected());
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(fp.get(0));
    }

}
