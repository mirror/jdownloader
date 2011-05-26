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

package jd.gui.swing.jdgui.views.downloads;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import jd.controlling.DownloadController;
import jd.gui.swing.components.ComboBrowseFile;
import jd.gui.swing.components.JDCollapser;
import jd.gui.swing.components.JDTextField;
import jd.gui.swing.components.multiprogress.MultiProgressBar;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import net.miginfocom.swing.MigLayout;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class FilePackageInfo extends JDCollapser implements ActionListener, FocusListener {

    private static final long serialVersionUID = 5410296068527460629L;

    private JPanel            panel;

    private JTabbedPane       tabbedPane;

    private JDTextField       txtComment;
    private JDTextField       txtCommentDl;
    private JDTextField       txtName;
    private JDTextField       txtNameDl;
    private JDTextField       txtPasswordDl;
    private JDTextField       txtPassword;
    private JDTextField       txtPassword2;
    private JDTextField       txtStatusDl;
    private JDTextField       txtURL;
    private JDTextField       txtPathLabel;
    private JLabel            lblSize;
    private JLabel            lblSizeDl;
    private JLabel            lblHoster;
    private JLabel            lblType;
    private JLabel            lblChunks;
    private JLabel            lblETA;
    private JLabel            lblSpeed;
    private JLabel            lblFiles;
    private JCheckBox         chbPostProcessing;
    private ComboBrowseFile   brwSaveTo;
    private MultiProgressBar  progressBarFilePackage;
    private MultiProgressBar  progressBarDownloadLink;

    private FilePackage       fp               = null;
    private DownloadLink      downloadLink;

    private boolean           hidden           = false;

    private transient Thread  updater;

    public FilePackageInfo() {
        buildGui();
        fp = null;
        menutitle.setText(_GUI._.gui_table_contextmenu_prop());
        menutitle.setIcon(NewTheme.I().getIcon("info", 16));
    }

    public void setPackage(FilePackage fp) {
        tabbedPane.setSelectedIndex(0);
        tabbedPane.setEnabledAt(1, false);
        if (this.fp != null && this.fp == fp) {
            update();
            return;
        }
        this.fp = fp;
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
        lblSize.setText(Formatter.formatReadable(fp.getTotalEstimatedPackageSize()));
        txtName.setText(fp.getName());
        txtComment.setText(fp.getComment());
        txtPassword.setText(fp.getPassword());
        txtPassword2.setText(fp.getPasswordAuto().toString());
        brwSaveTo.setText(fp.getDownloadDirectory());
        chbPostProcessing.setSelected(fp.isPostProcessing());
        /* neuzeichnen */
        revalidate();
    }

    public FilePackage getPackage() {
        return fp;
    }

    private void buildGui() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(_GUI._.gui_fileinfopanel_packagetab(), NewTheme.I().getIcon("package_open", 16), createFilePackageInfo());
        tabbedPane.addTab(_GUI._.gui_fileinfopanel_link(), NewTheme.I().getIcon("link", 16), createLinkInfo());
        content.setLayout(new MigLayout("ins 0", "[grow]", "[]"));
        content.add(tabbedPane, "grow");
    }

    private JPanel createFilePackageInfo() {
        txtName = new JDTextField(true);
        txtName.addActionListener(this);
        txtName.addFocusListener(this);
        brwSaveTo = new ComboBrowseFile("DownloadSaveTo");
        brwSaveTo.setEditable(true);
        brwSaveTo.setFileSelectionMode(ComboBrowseFile.DIRECTORIES_ONLY);
        brwSaveTo.setText(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());
        brwSaveTo.addActionListener(this);
        brwSaveTo.addFocusListener(this);
        txtPassword = new JDTextField(true);
        txtPassword.addActionListener(this);
        txtPassword.addFocusListener(this);
        txtPassword2 = new JDTextField(true);
        txtPassword2.setEditable(false);
        txtComment = new JDTextField(true);
        txtComment.addActionListener(this);
        txtComment.addFocusListener(this);
        chbPostProcessing = new JCheckBox(_GUI._.gui_fileinfopanel_packagetab_chb_postProcessing());
        chbPostProcessing.setToolTipText(_GUI._.gui_fileinfopanel_packagetab_chb_postProcessing_toolTip());
        chbPostProcessing.setSelected(true);
        chbPostProcessing.setHorizontalTextPosition(JCheckBox.LEFT);
        chbPostProcessing.addActionListener(this);
        chbPostProcessing.addFocusListener(this);

        panel = new JPanel(new MigLayout("ins 5, wrap 3", "[]10[grow,fill]10[]", "[]5[]5[]5[]"));
        panel.add(lblFiles = new JLabel(_GUI._.gui_fileinfopanel_packagetab_lbl_files(0)), "spanx, split 3");
        panel.add(progressBarFilePackage = new MultiProgressBar(), "growx, pushx, h 18!");
        panel.add(lblSize = new JLabel("0B/0B"), "alignx right");
        panel.add(new JLabel(_GUI._.gui_fileinfopanel_packagetab_lbl_name()));
        panel.add(txtName, "growx, span 2");
        panel.add(new JLabel(_GUI._.gui_fileinfopanel_packagetab_lbl_saveto()));
        panel.add(brwSaveTo.getInput(), "shrinkx");
        panel.add(brwSaveTo.getButton(), "pushx, growx");
        panel.add(new JLabel(_GUI._.gui_fileinfopanel_packagetab_lbl_password()));
        panel.add(txtPassword, "growx");
        panel.add(chbPostProcessing, "alignx right");
        panel.add(new JLabel(_GUI._.gui_fileinfopanel_packagetab_lbl_password2()));
        panel.add(txtPassword2, "growx, span 2");
        panel.add(new JLabel(_GUI._.gui_fileinfopanel_packagetab_lbl_comment()));
        panel.add(txtComment, "growx, span 2");
        return panel;
    }

    private JPanel createLinkInfo() {
        progressBarDownloadLink = new MultiProgressBar();
        panel = new JPanel(new MigLayout("ins 5, wrap 3", "[]10[grow,fill]10[]", "[]5[]5[]5[]"));
        panel.add(lblHoster = new JLabel(NewTheme.I().getIcon("sort", 16)), "split 3");
        panel.add(lblType = new JLabel(NewTheme.I().getIcon("sort", 16)));
        panel.add(lblChunks = new JLabel(_GUI._.gui_fileinfopanel_linktab_chunks()));
        panel.add(progressBarDownloadLink, "spanx, growx, pushx, split 2, h 18!");
        panel.add(lblSizeDl = new JLabel("0B/0B"), "alignx right");
        panel.add(lblETA = new JLabel(_GUI._.gui_fileinfopanel_linktab_eta2("0")));
        panel.add(lblSpeed = new JLabel(_GUI._.gui_fileinfopanel_linktab_speed("0 kb")), "skip");
        panel.add(new JLabel(_GUI._.gui_fileinfopanel_linktab_name()));
        panel.add(txtNameDl = new JDTextField(true), "growx, spanx");
        txtNameDl.setEditable(false);
        panel.add(new JLabel(_GUI._.gui_fileinfopanel_linktab_saveto()));
        panel.add(txtPathLabel = new JDTextField(true), "growx, spanx");
        txtPathLabel.setEditable(false);
        panel.add(new JLabel(_GUI._.gui_fileinfopanel_linktab_url()));
        panel.add(txtURL = new JDTextField(true), "growx, spanx");
        txtURL.setEditable(false);
        panel.add(new JLabel(_GUI._.gui_fileinfopanel_linktab_comment()));
        panel.add(txtCommentDl = new JDTextField(true), "growx, spanx");
        txtCommentDl.setEditable(false);
        panel.add(new JLabel(_GUI._.gui_fileinfopanel_linktab_password()));
        panel.add(txtPasswordDl = new JDTextField(true), "growx, spanx");
        txtPasswordDl.setEditable(false);
        panel.add(new JLabel(_GUI._.gui_fileinfopanel_linktab_status()));
        panel.add(txtStatusDl = new JDTextField(true), "growx, spanx");
        txtStatusDl.setEditable(false);

        return panel;
    }

    public void actionPerformed(ActionEvent e) {
        updateFilePackage(e.getSource());
    }

    public void focusGained(FocusEvent e) {
    }

    public void focusLost(FocusEvent e) {
        updateFilePackage(e.getSource());
    }

    private void updateFilePackage(Object source) {
        if (fp == null) return;
        if (source == txtName) {
            fp.setName(txtName.getText());
        } else if (source == brwSaveTo) {
            fp.setDownloadDirectory(brwSaveTo.getText());
        } else if (source == txtComment) {
            fp.setComment(txtComment.getText());
        } else if (source == txtPassword) {
            fp.setPassword(txtPassword.getText());
        } else if (source == chbPostProcessing) {
            fp.setPostProcessing(chbPostProcessing.isSelected());
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(fp.get(0));
    }

    @Override
    public void onShow() {
        update();
        hidden = false;
        if (updater != null && updater.isAlive()) return;
        updater = new Thread() {
            @Override
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
                                max[i] = Math.max(1, dl.getDownloadSize());
                                if (dl.getLinkStatus().hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) {
                                    values[i] = -1;
                                } else {
                                    values[i] = Math.max(0, dl.getDownloadCurrent());
                                }
                                i++;
                            }
                            progressBarFilePackage.setMaximums(max);
                            progressBarFilePackage.setValues(values);
                            lblSize.setText(Formatter.formatReadable(fp.getTotalKBLoaded()) + "/" + Formatter.formatReadable(fp.getTotalEstimatedPackageSize()));
                            lblFiles.setText(_GUI._.gui_fileinfopanel_packagetab_lbl_files(fp.getDownloadLinkList().size()));
                            lblSize.setToolTipText(fp.getTotalKBLoaded() + " / " + fp.getTotalEstimatedPackageSize());
                        }
                        if (downloadLink != null) {
                            if (downloadLink.getChunksProgress() != null && !downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) {
                                long fileSize = downloadLink.getDownloadSize();
                                int chunks = downloadLink.getChunksProgress().length;
                                lblChunks.setText(chunks + " " + _GUI._.gui_fileinfopanel_linktab_chunks());
                                long part = fileSize / chunks;

                                long[] max = new long[chunks];
                                long[] values = new long[chunks];
                                for (int i = 0; i < chunks; i++) {
                                    max[i] = part;
                                    values[i] = (downloadLink.getChunksProgress()[i] + 1) - i * part;
                                }
                                max[chunks - 1] = fileSize - part * (chunks - 1);
                                values[chunks - 1] = (downloadLink.getChunksProgress()[chunks - 1] + 1) - part * (chunks - 1);
                                progressBarDownloadLink.setMaximums(max);
                                progressBarDownloadLink.setValues(values);
                            } else {
                                progressBarDownloadLink.setMaximums(new long[] { 10 });
                                if (downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) {
                                    progressBarDownloadLink.setValues(new long[] { -1 });
                                } else {
                                    progressBarDownloadLink.setValues(new long[] { 0 });
                                }
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
                            lblSizeDl.setText(Formatter.formatReadable(downloadLink.getDownloadCurrent()) + "/" + Formatter.formatReadable(downloadLink.getDownloadSize()));
                            lblSizeDl.setToolTipText(downloadLink.getDownloadCurrent() + " / " + downloadLink.getDownloadSize());
                            if (downloadLink.getDownloadSpeed() <= 0) {
                                lblETA.setVisible(false);
                                lblSpeed.setVisible(false);
                            } else {
                                lblETA.setText(_GUI._.gui_fileinfopanel_linktab_eta2(Formatter.formatSeconds((downloadLink.getDownloadSize() - downloadLink.getDownloadCurrent()) / downloadLink.getDownloadSpeed())));
                                lblSpeed.setText(_GUI._.gui_fileinfopanel_linktab_speed(Formatter.formatReadable(Math.max(0, downloadLink.getDownloadSpeed()))));
                                lblSpeed.setVisible(true);
                                lblETA.setVisible(true);
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
        if (fp == null) return;
        fp.setName(txtName.getText());
        fp.setComment(txtComment.getText());
        fp.setPassword(txtPassword.getText());
        fp.setDownloadDirectory(brwSaveTo.getText());
        fp.setPostProcessing(chbPostProcessing.isSelected());
    }

    @Override
    public void onHide() {
        if (fp == null && downloadLink == null) return;
        onHideSave();
        hidden = true;
        if (updater != null) {
            updater.interrupt();
            updater = null;
        }
        progressBarFilePackage.setMaximums(null);
        progressBarDownloadLink.setMaximums(null);
    }

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(DownloadLink downloadLink) {
        tabbedPane.setEnabledAt(1, true);
        tabbedPane.setSelectedIndex(1);
        if (this.downloadLink != null && this.downloadLink == downloadLink) return;
        this.downloadLink = downloadLink;
        if (downloadLink != null) {
            fp = downloadLink.getFilePackage();
            txtPathLabel.setText(downloadLink.getFileOutput());
            lblType.setIcon(downloadLink.getIcon());
            if (downloadLink.getDefaultPlugin() != null) {
                lblHoster.setIcon(downloadLink.getDefaultPlugin().getHosterIconScaled());
            } else {
                lblHoster.setIcon(null);
            }

            lblHoster.setToolTipText(downloadLink.getHost());
            lblType.setToolTipText(downloadLink.getType());
            lblSizeDl.setText(Formatter.formatReadable(downloadLink.getDownloadCurrent()) + "/" + Formatter.formatReadable(downloadLink.getDownloadSize()));
            if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_NORMAL) {
                txtURL.setText(downloadLink.getBrowserUrl());
            } else {
                txtURL.setText("**************");
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

}