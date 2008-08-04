//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.gui.skins.simple.components;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ComboBrowseFile extends JPanel implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String approveButtonText = "OK";

    private JButton btnBrowse;

    private JComboBox cmboInput;

    private File currentPath;

    private boolean editable = false;

    private Vector<String> files;

    private int fileSelectionMode = JFileChooser.FILES_ONLY;

    @SuppressWarnings("unused")
    private Logger logger = JDUtilities.getLogger();

    @SuppressWarnings("unchecked")
    public ComboBrowseFile(String string) {
        Vector<String> list;
        try {
            list = (Vector<String>) JDUtilities.getSubConfig("GUI").getProperty(string, new Vector<String>());
        } catch (Exception e) {
            e.printStackTrace();
            list = new Vector<String>();
        }
        files = list;

        setName(string);
        initGUI();
    }

    public ComboBrowseFile(Vector<String> files) {

        super();
        if (files == null) {
            files = new Vector<String>();
        }
        this.files = files;
        initGUI();
    }

    public void actionPerformed(ActionEvent e) {
        File newPath;
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");

        if (e.getSource() == cmboInput) {
            Object sel = cmboInput.getSelectedItem();
            if (sel != null) {
                newPath = new File(sel.toString());
                setCurrentPath(newPath);
                dispatchEvent(event);
            }
        } else if (e.getSource() == btnBrowse) {
            newPath = getPath();
            setCurrentPath(newPath);
            dispatchEvent(event);
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
        String stringPath = cmboInput.getSelectedItem().toString();

        if (null != stringPath) {
            directory = new File(cmboInput.getSelectedItem().toString());
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
        JDFileChooser fc = new JDFileChooser();
        fc.setApproveButtonText(approveButtonText);
        fc.setFileSelectionMode(fileSelectionMode);
        fc.setCurrentDirectory(getDirectoryFromTxtInput());
        fc.showOpenDialog(this);
        File ret = fc.getSelectedFile();
        return ret;
    }

    public String getText() {
        return cmboInput.getSelectedItem().toString();
    }

    private void initGUI() {
        int n = 5;
        setLayout(new BorderLayout(n, n));
        cmboInput = new JComboBox(files);

        cmboInput.setEditable(editable);
        cmboInput.addActionListener(this);

        btnBrowse = new JButton(JDLocale.L("gui.btn_select", "auswählen"));
        btnBrowse.addActionListener(this);

        // JDUtilities.addToGridBag(this, txtInput, 0, 0, 1, 1, 1, 0, new
        // Insets(0, 0, 0, 0), GridBagConstraints.HORIZONTAL,
        // GridBagConstraints.WEST);
        // JDUtilities.addToGridBag(this, btnBrowse, 1, 0, 1, 1, 0, 0, new
        // Insets(0, 0, 0, 0), GridBagConstraints.NONE,
        // GridBagConstraints.EAST);

        add(cmboInput, BorderLayout.CENTER);
        add(btnBrowse, BorderLayout.EAST);
    }

    /**
     * @param approveButtonText
     *            the approveButtonText to set
     */
    public void setApproveButtonText(String approveButtonText) {
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
        if (currentPath == null) { return; }
        this.currentPath = currentPath;
        String item = currentPath.toString();
        if (!files.contains(item)) {
            files.add(0, item);

            JDUtilities.getSubConfig("GUI").setProperty(getName(), new Vector<String>(files.subList(0, Math.min(files.size(), 20))));
            JDUtilities.getSubConfig("GUI").save();
        }

        // cmboInput.removeAllItems();
        // for (String file : files) {
        // cmboInput.addItem(file);
        // }
        cmboInput.invalidate();

        cmboInput.setSelectedItem(item);

    }

    public void setEditable(boolean value) {
        cmboInput.setEditable(value);
        editable = value;
    }

    @Override
    public void setEnabled(boolean value) {
        cmboInput.setEnabled(value);
        btnBrowse.setEnabled(value);

    }

    /**
     * @param fileSelectionMode
     *            the fileSelectionMode to set
     */
    public void setFileSelectionMode(int fileSelectionMode) {
        this.fileSelectionMode = fileSelectionMode;
    }

    public void setText(String text) {
        if (text == null) {
            text = "";
        }
        setCurrentPath(new File(text));
    }

}
