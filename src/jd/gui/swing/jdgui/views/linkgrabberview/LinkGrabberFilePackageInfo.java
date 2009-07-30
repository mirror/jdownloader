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

package jd.gui.swing.jdgui.views.linkgrabberview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import jd.config.Configuration;
import jd.controlling.DownloadController;
import jd.controlling.PasswordListController;
import jd.gui.swing.components.ComboBrowseFile;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.swing.components.JDTextField;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class LinkGrabberFilePackageInfo extends SwitchPanel implements ActionListener {

    private static final long serialVersionUID = 5410296068527460629L;

    private ComboBrowseFile brwSaveTo;

    private JDTextField txtComment;

    private JDTextField txtName;

    private JDTextField txtPassword;

    private JCheckBox chbExtract;

    private JCheckBox chbUseSubdirectory;

    private LinkGrabberFilePackage fp = null;

    private boolean notifyUpdate = true;

    public LinkGrabberFilePackageInfo() {
        buildGui();
        fp = null;
    }

    public void setPackage(LinkGrabberFilePackage fp) {
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
        notifyUpdate = false;
        if (!txtName.isFocusOwner()) txtName.setText(fp.getName());
        if (!txtComment.isFocusOwner()) txtComment.setText(fp.getComment());
        if (!txtPassword.isFocusOwner()) txtPassword.setText(fp.getPassword());
        if (!brwSaveTo.isFocusOwner()) brwSaveTo.setText(fp.getDownloadDirectory());
        if (!chbExtract.isFocusOwner()) chbExtract.setSelected(fp.isExtractAfterDownload());
        if (!chbUseSubdirectory.isFocusOwner()) chbUseSubdirectory.setSelected(fp.useSubDir());
        /* neuzeichnen */
        revalidate();
        notifyUpdate = true;
    }

    public LinkGrabberFilePackage getPackage() {
        return fp;
    }

    private void buildGui() {
        txtName = new JDTextField(true);
        txtName.addActionListener(this);

        brwSaveTo = new ComboBrowseFile("DownloadSaveTo");
        brwSaveTo.setEditable(true);
        brwSaveTo.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
        brwSaveTo.setText(JDUtilities.getConfiguration().getDefaultDownloadDirectory());
        brwSaveTo.addActionListener(this);

        txtPassword = new JDTextField(true);
        txtPassword.addActionListener(this);

        txtComment = new JDTextField(true);
        txtComment.addActionListener(this);

        chbExtract = new JCheckBox(JDL.L("gui.linkgrabber.packagetab.chb.extractAfterdownload", "Extract"));
        chbExtract.setSelected(true);
        chbExtract.setHorizontalTextPosition(SwingConstants.LEFT);
        chbExtract.addActionListener(this);

        chbUseSubdirectory = new JCheckBox(JDL.L("gui.linkgrabber.packagetab.chb.useSubdirectory", "Use Subdirectory"));
        chbUseSubdirectory.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false));
        chbUseSubdirectory.setHorizontalTextPosition(SwingConstants.LEFT);
        chbUseSubdirectory.addActionListener(this);

        this.setLayout(new MigLayout("ins 10, wrap 3", "[]10[grow,fill][]", "[]5[]5[]5[]"));

        this.add(new JLabel(JDL.L("gui.linkgrabber.packagetab.lbl.name", "Paketname")));
        this.add(txtName, "span 2");
        this.add(new JLabel(JDL.L("gui.linkgrabber.packagetab.lbl.saveto", "Speichern unter")));
        this.add(brwSaveTo.getInput(), "gapright 10, growx");
        this.add(brwSaveTo.getButton(), "pushx,growx");

        this.add(new JLabel(JDL.L("gui.linkgrabber.packagetab.lbl.password", "Archivpasswort")), "newline");

        this.add(txtPassword, " gapright 10, growx");
        this.add(chbExtract, "alignx right");
        this.add(new JLabel(JDL.L("gui.linkgrabber.packagetab.lbl.comment", "Kommentar")));
        this.add(txtComment, "gapright 10, growx");
        this.add(chbUseSubdirectory, "alignx right");
    }

    public void actionPerformed(ActionEvent e) {
        if (fp == null || !notifyUpdate) return;
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
        } else if (e.getSource() == chbUseSubdirectory) {
            fp.setUseSubDir(chbUseSubdirectory.isSelected());
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(fp.get(0));
    }

    // @Override
    public void onShow() {
        update();
    }

    private void onHideSave() {
        notifyUpdate = false;
        fp.setName(txtName.getText());
        fp.setComment(txtComment.getText());
        fp.setPassword(txtPassword.getText());
        notifyUpdate = true;
    }

    // @Override
    public void onHide() {
        if (this.fp == null) return;
        onHideSave();
        PasswordListController.getInstance().addPassword(txtPassword.getText());
        actionPerformed(new ActionEvent(this.brwSaveTo, 0, null));
        fp = null;
    }

}
