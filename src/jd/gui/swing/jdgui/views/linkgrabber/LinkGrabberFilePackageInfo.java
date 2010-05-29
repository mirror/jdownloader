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

package jd.gui.swing.jdgui.views.linkgrabber;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import jd.config.Configuration;
import jd.controlling.LinkGrabberController;
import jd.gui.swing.components.ComboBrowseFile;
import jd.gui.swing.components.JDCollapser;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.swing.components.JDTextField;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class LinkGrabberFilePackageInfo extends JDCollapser implements ActionListener, FocusListener {

    private static final long serialVersionUID = 5410296068527460629L;

    private ComboBrowseFile brwSaveTo;

    private JDTextField txtComment;

    private JDTextField txtName;

    private JDTextField txtPassword;

    private JCheckBox chbPostProcessing;

    private JCheckBox chbUseSubdirectory;

    private LinkGrabberFilePackage fp = null;

    private JDTextField txtPassword2;

    public LinkGrabberFilePackageInfo() {
        buildGui();
        fp = null;
        this.menutitle.setText(JDL.L("gui.linkgrabber.packagetab.title", "Properties / Package / Filename"));
    }

    public void setPackage(LinkGrabberFilePackage fp) {
        if (this.fp != null && this.fp != fp) {
            onHideSave();
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
        txtName.setText(fp.getName());
        txtComment.setText(fp.getComment());
        txtPassword.setText(fp.getPassword());
        txtPassword2.setText(fp.getPasswordAuto().toString());
        brwSaveTo.setText(fp.getDownloadDirectory());
        chbPostProcessing.setSelected(fp.isPostProcessing());
        chbUseSubdirectory.setSelected(fp.useSubDir());
        /* neuzeichnen */
        revalidate();

    }

    public LinkGrabberFilePackage getPackage() {
        return fp;
    }

    private void buildGui() {
        txtName = new JDTextField(true);
        txtName.addActionListener(this);
        txtName.addFocusListener(this);
        brwSaveTo = new ComboBrowseFile("DownloadSaveTo");
        brwSaveTo.setEditable(true);
        brwSaveTo.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
        brwSaveTo.setText(JDUtilities.getDefaultDownloadDirectory());
        brwSaveTo.addActionListener(this);

        txtPassword = new JDTextField(true);
        txtPassword.addActionListener(this);
        txtPassword.addFocusListener(this);
        txtPassword2 = new JDTextField(true);
        txtPassword2.setEditable(false);
        txtComment = new JDTextField(true);
        txtComment.addActionListener(this);
        txtComment.addFocusListener(this);
        chbPostProcessing = new JCheckBox(JDL.L("gui.fileinfopanel.packagetab.chb.postProcessing", "Post Processing"));
        chbPostProcessing.setToolTipText(JDL.L("gui.fileinfopanel.packagetab.chb.postProcessing.toolTip", "Enable Post Processing for this FilePackge, like extracting or merging."));
        chbPostProcessing.setSelected(true);
        chbPostProcessing.setHorizontalTextPosition(JCheckBox.LEFT);
        chbPostProcessing.addActionListener(this);

        chbUseSubdirectory = new JCheckBox(JDL.L("gui.linkgrabber.packagetab.chb.useSubdirectory", "Use Subdirectory"));
        chbUseSubdirectory.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false));
        chbUseSubdirectory.setHorizontalTextPosition(JCheckBox.LEFT);
        chbUseSubdirectory.addActionListener(this);

        content.setLayout(new MigLayout("ins 5, wrap 3", "[]10[grow,fill]10[]", "[]5[]5[]5[]"));
        content.add(new JLabel(JDL.L("gui.linkgrabber.packagetab.lbl.name", "Package Name")));
        content.add(txtName, "span 2");
        content.add(new JLabel(JDL.L("gui.linkgrabber.packagetab.lbl.saveto", "Save to")));
        content.add(brwSaveTo.getInput(), "growx");
        content.add(brwSaveTo.getButton(), "pushx, growx");
        content.add(new JLabel(JDL.L("gui.linkgrabber.packagetab.lbl.password", "Archive Password")));
        content.add(txtPassword, "growx");
        content.add(chbPostProcessing, "alignx right");
        content.add(new JLabel(JDL.L("gui.linkgrabber.packagetab.lbl.password2", "Archive Password(auto)")));
        content.add(txtPassword2, "growx");
        content.add(chbUseSubdirectory, "alignx right");
        content.add(new JLabel(JDL.L("gui.linkgrabber.packagetab.lbl.comment", "Comment")));
        content.add(txtComment, "span 2");
    }

    public void actionPerformed(ActionEvent e) {
        if (fp == null) return;
        if (e.getSource() == txtName) fp.setName(txtName.getText());
        if (e.getSource() == brwSaveTo) fp.setDownloadDirectory(brwSaveTo.getText());
        if (e.getSource() == txtComment) fp.setComment(txtComment.getText());
        if (e.getSource() == txtPassword) fp.setPassword(txtPassword.getText());
        if (e.getSource() == chbPostProcessing) fp.setPostProcessing(chbPostProcessing.isSelected());
        if (e.getSource() == chbUseSubdirectory) fp.setUseSubDir(chbUseSubdirectory.isSelected());
        LinkGrabberController.getInstance().throwRefresh();
    }

    // @Override
    public void onShow() {
        update();
    }

    public void onHideSave() {
        if (fp == null) return;
        fp.setName(txtName.getText());
        fp.setComment(txtComment.getText());
        fp.setPassword(txtPassword.getText());
        fp.setDownloadDirectory(brwSaveTo.getText());
        fp.setPostProcessing(chbPostProcessing.isSelected());
        fp.setUseSubDir(chbUseSubdirectory.isSelected());
    }

    // @Override
    public void onHide() {
        if (this.fp == null) return;
        onHideSave();
        fp = null;
    }

    public void focusGained(FocusEvent e) {
    }

    public void focusLost(FocusEvent e) {
        if (fp == null) return;
        if (e.getSource() == txtName) fp.setName(txtName.getText());
        if (e.getSource() == brwSaveTo) fp.setDownloadDirectory(brwSaveTo.getText());
        if (e.getSource() == txtComment) fp.setComment(txtComment.getText());
        if (e.getSource() == txtPassword) fp.setPassword(txtPassword.getText());
        if (e.getSource() == chbPostProcessing) fp.setPostProcessing(chbPostProcessing.isSelected());
        if (e.getSource() == chbUseSubdirectory) fp.setUseSubDir(chbUseSubdirectory.isSelected());
        LinkGrabberController.getInstance().throwRefresh();
    }

    @Override
    public void onClosed() {
        LinkgrabberView.getInstance().setInfoPanel(null);
    }

}
