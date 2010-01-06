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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import jd.config.SubConfiguration;
import jd.gui.swing.GuiRunnable;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class ComboBrowseFile extends JPanel implements ActionListener {

    private static final long serialVersionUID = -3852915099917640687L;

    private Object LOCK = new Object();

    private ArrayList<ActionListener> listenerList = new ArrayList<ActionListener>();

    private String approveButtonText;

    private JButton btnBrowse;

    private JComboBox cmboInput;

    private File currentPath;

    private Vector<String> files;

    private int fileSelectionMode = JFileChooser.FILES_ONLY;

    private FileFilter fileFilter;

    private boolean dispatchingDisabled = false;

    public ComboBrowseFile(final String string) {
        final Vector<String> list = SubConfiguration.getConfig("GUI").getGenericProperty(string, new Vector<String>());
        setFiles(list);

        this.setName(string);
        initGUI();
    }

    public ComboBrowseFile(final Vector<String> files) {
        super();
        setFiles(files == null ? new Vector<String>() : files);
        initGUI();
    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cmboInput) {
            final Object sel = cmboInput.getSelectedItem();
            if (sel != null) {
                setCurrentPath(new File(sel.toString()));
            }
            if (!dispatchingDisabled) {
                for (ActionListener l : listenerList) {
                    l.actionPerformed(new ActionEvent(this, e.getID(), e.getActionCommand()));
                }
            }
        } else if (e.getSource() == btnBrowse) {
            setCurrentPath(getPath());
            for (ActionListener l : listenerList) {
                l.actionPerformed(new ActionEvent(this, e.getID(), e.getActionCommand()));
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
     * @return null or a File object pointing to a directory
     */
    private File getDirectoryFromTxtInput() {
        File directory = null;

        final Object sel = cmboInput.getSelectedItem();
        if (sel != null) {
            directory = new File(sel.toString());
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
        return cmboInput.isEditable();
    }

    /**
     * @return the fileFilter or null, when no fileFilter is specified
     */
    public FileFilter getFileFilter() {
        return fileFilter;
    }

    /**
     * @return the fileSelectionMode
     */
    public int getFileSelectionMode() {
        return fileSelectionMode;
    }

    private File getPath() {
        final JDFileChooser fc = new JDFileChooser();
        if (approveButtonText != null) {
            fc.setApproveButtonText(approveButtonText);
        }
        fc.setFileSelectionMode(fileSelectionMode);
        if (fileFilter != null) {
            fc.setFileFilter(fileFilter);
        }
        fc.setCurrentDirectory(getDirectoryFromTxtInput());
        return fc.showOpenDialog(this) == JDFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
    }

    public String getText() {
        if (cmboInput.getSelectedItem() == null) return "";
        return cmboInput.getSelectedItem().toString();
    }

    private void initGUI() {
        this.setLayout(new MigLayout("insets 0", "[left, grow]15px[]", ""));

        cmboInput = new JComboBox(files) {

            private static final long serialVersionUID = 5288948184335860046L;

            public Dimension getMaximumSize() {
                return new Dimension(20, 20);
            }

            public Dimension getMinimumSize() {
                return new Dimension(20, 20);
            }

            public Dimension getPreferredSize() {
                return new Dimension(20, 20);
            }

        };
        cmboInput.setEditable(false);
        cmboInput.addActionListener(this);
        if (cmboInput.getItemCount() > 0) {

            synchronized (LOCK) {
                dispatchingDisabled = true;
                cmboInput.setSelectedIndex(0);
                dispatchingDisabled = false;
            }

        }

        btnBrowse = new JButton(JDL.L("gui.btn_select", "Auswählen"));
        btnBrowse.addActionListener(this);

        this.add(cmboInput, "grow");
        this.add(btnBrowse, "wrap");
    }

    public JButton getButton() {
        return btnBrowse;
    }

    public JComboBox getInput() {
        return cmboInput;
    }

    /**
     * @param approveButtonText
     *            the approveButtonText to set
     */
    public void setApproveButtonText(final String approveButtonText) {
        this.approveButtonText = approveButtonText;
    }

    public void setButtonText(final String text) {
        btnBrowse.setText(text);
    }

    /**
     * @param currentPath
     *            the currentPath to set
     */
    public void setCurrentPath(final File currentPath) {
        if (currentPath != null && !currentPath.equals(this.currentPath)) {
            this.currentPath = currentPath;
            final String item = currentPath.toString();
            files.remove(item);
            files.add(0, item);

            SubConfiguration guiConfig = SubConfiguration.getConfig("GUI");
            guiConfig.setProperty(getName(), new Vector<String>(files.subList(0, Math.min(files.size(), 20))));
            guiConfig.save();

            final String text = getText();
            if (text == null || !text.equalsIgnoreCase(currentPath.toString())) {
                synchronized (LOCK) {
                    dispatchingDisabled = true;
                    cmboInput.setSelectedIndex(0);
                    dispatchingDisabled = false;
                }
                /* EXPERIMENTAL: rausgenommen da freezes verursacht hat */
                // cmboInput.invalidate();
            }
        }
    }

    public void setEditable(final boolean value) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                cmboInput.setEditable(value);
            }
        });
    }

    // @Override
    public void setEnabled(final boolean value) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                cmboInput.setEnabled(value);
                btnBrowse.setEnabled(value);
            }
        });
    }

    /**
     * @param fileFilter
     *            the fileFilter to set
     */
    public void setFileFilter(final FileFilter fileFilter) {
        this.fileFilter = fileFilter;
        for (int i = files.size() - 1; i >= 0; --i) {
            if (!fileFilter.accept(new File(files.get(i)))) {
                files.remove(i);
            }
        }
    }

    private void setFiles(final Vector<String> files) {
        for (int i = files.size() - 1; i >= 0; --i) {
            if (!new File(files.get(i)).exists()) {
                files.remove(i);
            }
        }
        this.files = files;
    }

    /**
     * @param fileSelectionMode
     *            the fileSelectionMode to set
     */
    public void setFileSelectionMode(final int fileSelectionMode) {
        this.fileSelectionMode = fileSelectionMode;
        if (fileSelectionMode == JFileChooser.DIRECTORIES_ONLY) {
            for (int i = files.size() - 1; i >= 0; --i) {
                if (!new File(files.get(i)).isDirectory()) {
                    files.remove(i);
                }
            }
        } else if (fileSelectionMode == JFileChooser.FILES_ONLY) {
            for (int i = files.size() - 1; i >= 0; --i) {
                if (!new File(files.get(i)).isFile()) {
                    files.remove(i);
                }
            }
        }
    }

    public void setText(final String text) {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                if (text == null) {
                    setCurrentPath(new File(""));
                } else {
                    setCurrentPath(new File(text));
                }
                return null;
            }

        }.start();

    }

    /**
     * Adds an <code>ActionListener</code> to the ComboBrowseFile.
     * 
     * @param l
     *            the <code>ActionListener</code> to be added
     */
    public void addActionListener(final ActionListener l) {
        listenerList.add(l);
    }

    /**
     * Removes an <code>ActionListener</code> from the ComboBrowseFile.
     * 
     * @param l
     *            the <code>ActionListener</code> to be removed
     */
    public void removeActionListener(final ActionListener l) {
        listenerList.remove(l);
    }

}
