package jd.gui.skins.simple.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import jd.plugins.Plugin;

public class BrowseFile extends JPanel implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JTextField txtInput;
    private JButton    btnBrowse;
    private File       currentPath;
    private int        fileSelectionMode = JFileChooser.FILES_ONLY;
    private String     approveButtonText = "OK";
    private boolean    editable          = false;
    @SuppressWarnings("unused")
    private Logger logger=Plugin.getLogger();
    public BrowseFile(int width) {
        super();
        txtInput = new JTextField(width);
        txtInput.setEditable(editable);
        txtInput.addActionListener(this);

        btnBrowse = new JButton("auswählen");
        btnBrowse.addActionListener(this);
        this.add(txtInput);
        this.add(btnBrowse);
        SwingUtilities.updateComponentTreeUI(this);

    }
public void setButtonText(String text){
    btnBrowse.setText(text);
}
    private File getPath() {
        JFileChooser fc = new JFileChooser();
        fc.setApproveButtonText(approveButtonText);
        if (currentPath != null)
            fc.setCurrentDirectory(currentPath);
        fc.setFileSelectionMode(fileSelectionMode);
        fc.showOpenDialog(this);
        File ret = fc.getSelectedFile();
        return ret;
    }

    public void actionPerformed(ActionEvent e) {
        File newPath;
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");

        if (e.getSource() == txtInput) {
            newPath = new File(txtInput.getText());
            setCurrentPath(newPath);
            this.dispatchEvent(event);
        }
        if (e.getSource() == btnBrowse) {
            newPath = getPath();
            setCurrentPath(newPath);
            this.dispatchEvent(event);
        }

    }

    public void setEditable(boolean value) {
        txtInput.setEditable(value);
        editable = value;
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

    /**
     * @param fileSelectionMode
     *            the fileSelectionMode to set
     */
    public void setFileSelectionMode(int fileSelectionMode) {
        this.fileSelectionMode = fileSelectionMode;
    }

    /**
     * @return the currentPath
     */
    public File getCurrentPath() {
        return currentPath;
    }
    public String getText(){
        return currentPath.getAbsolutePath();
    }
    public void setText(String text){
        if(text==null)text="";
        setCurrentPath(new File(text));
    }
    /**
     * @param currentPath
     *            the currentPath to set
     */
    public void setCurrentPath(final File currentPath) {
        if(currentPath==null)return;
        this.currentPath = currentPath;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                txtInput.setText(currentPath.getAbsolutePath());

            }
        });
        
    }

    /**
     * @return the approveButtonText
     */
    public String getApproveButtonText() {
        return approveButtonText;
    }

    /**
     * @param approveButtonText
     *            the approveButtonText to set
     */
    public void setApproveButtonText(String approveButtonText) {
        this.approveButtonText = approveButtonText;
    }

}
