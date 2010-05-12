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

package jd.gui.swing.components;

import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class BrowseFile extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private String approveButtonText = "OK";

    private JButton btnBrowse;

    private File currentPath;

    private boolean editable = false;

    private int fileSelectionMode = JFileChooser.FILES_ONLY;

    private JTextField txtInput;

    public BrowseFile() {
        this(null);
    }

    public BrowseFile(final LayoutManager layout) {
        super(layout != null ? layout : new MigLayout("ins 0", "[grow 100,fill,160:null:null]3[grow 30,fill]"));
        initGUI();
    }

    public void actionPerformed(final ActionEvent e) {
        final File newPath;
        final ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");

        if (e.getSource() == txtInput) {
            newPath = new File(txtInput.getText());
            setCurrentPath(newPath);
            dispatchEvent(event);
        } else if (e.getSource() == btnBrowse) {
            newPath = getPath();
            if (newPath != null) {
                setCurrentPath(newPath);
                dispatchEvent(event);
            }
        }
    }

    /**
     * @return the approveButtonText
     */
    public String getApproveButtonText() {
        return approveButtonText;
    }

    /**
     * @return the currentPath
     */
    public File getCurrentPath() {
        return currentPath;
    }

    /**
     * 
     * @return null or a File object pointing to a directory
     */
    private File getDirectoryFromTxtInput() {
        File directory = null;
        final String stringPath = txtInput.getText();

        if (null != stringPath) {
            directory = new File(txtInput.getText());
            if (directory.exists()) {
                if (directory.isFile()) {
                    directory = directory.getParentFile();
                }
            } else {
                directory = null;
            }
        }
        return directory;
    }

    public boolean getEditable() {
        return editable;
    }

    /**
     * @return the fileSelectionMode
     */
    public int getFileSelectionMode() {
        return fileSelectionMode;
    }

    private File getPath() {
        final JDFileChooser fc = new JDFileChooser();
        fc.setApproveButtonText(approveButtonText);
        fc.setFileSelectionMode(fileSelectionMode);
        fc.setCurrentDirectory(getDirectoryFromTxtInput());
        if (fc.showOpenDialog(this) == JDFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        } else {
            return null;
        }
    }

    public String getText() {
        return txtInput.getText();
    }

    private void initGUI() {
        txtInput = new JTextField();
        txtInput.setEditable(editable);
        txtInput.addActionListener(this);

        btnBrowse = new JButton(JDL.L("gui.btn_select", "Browse"));
        btnBrowse.addActionListener(this);

        add(txtInput);
        add(btnBrowse);
    }

    /**
     * @param approveButtonText
     *            the approveButtonText to set
     */
    public void setApproveButtonText(final String approveButtonText) {
        this.approveButtonText = approveButtonText;
    }

    public void setButtonText(String text) {
        btnBrowse.setText(text);
    }

    /**
     * @param currentPath
     *            the currentPath to set
     */
    public void setCurrentPath(final File currentPath) {
        if (currentPath != null) {
            this.currentPath = currentPath;
            txtInput.setText(currentPath.getAbsolutePath());
        }
    }

    public void setEditable(boolean value) {
        txtInput.setEditable(value);
        editable = value;
    }

    @Override
    public void setToolTipText(String text) {
        txtInput.setToolTipText(text);
        btnBrowse.setToolTipText(text);
        super.setToolTipText(text);
    }

    @Override
    public void setEnabled(final boolean value) {
        txtInput.setEnabled(value);
        btnBrowse.setEnabled(value);
    }

    /**
     * @param fileSelectionMode
     *            the fileSelectionMode to set
     */
    public void setFileSelectionMode(final int fileSelectionMode) {
        this.fileSelectionMode = fileSelectionMode;
    }

    public void setText(final String text) {
        setCurrentPath(new File(text == null ? "" : text));
    }

    public JTextField getTextField() {
        return txtInput;
    }

}
