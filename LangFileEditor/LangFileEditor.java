import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import jd.gui.skins.simple.LocationListener;
import jd.parser.Regex;
import jd.utils.JDUtilities;

public class LangFileEditor implements ActionListener {
	
	private JFrame frame;
	private JLabel lblFolder, lblFile, lblFolderValue, lblFileValue, lblNumSource, lblNumFile, lblNumMissing, lblNumOld;
	private JButton btnAdoptMissing, btnDeleteOld, btnDelete, btnEdit, btnSave, btnBrowseFolder, btnBrowseFile, btnReload, btnAdopt;
	private JTable table;
	private MyTableModel tableModel;
	
	private File sourceFolder, languageFile;
	
	public static void main(String[] args) {
		
		LangFileEditor editor = new LangFileEditor();
		editor.showGui();
		
		if ( args.length == 2 ) {
			
			editor.sourceFolder = new File(args[0]);
			editor.languageFile = new File(args[1]);
			editor.lblFolderValue.setText(args[0]);
			editor.lblFileValue.setText(args[1]);
			editor.initList();
			
		}
		
	}
	
	private void initList() {
		
		Vector<String[]> source = new Vector<String[]>();
		if ( sourceFolder != null ) source = getSourceEntries(sourceFolder);
		Vector<String[]> file = new Vector<String[]>();
		if ( languageFile != null ) file = getLanguageFileEntries(languageFile);
		
		Vector<String[]> data = getData(source, file);
		tableModel.setData(data);
		setInfoLabels();
		
	}
	
	private void setInfoLabels() {
		
		Vector<String[]> data = tableModel.getData();
		int numSource=0, numFile=0, numMissing=0, numOld=0;
		
		for ( String[] entry : data ) {
			
			if ( entry[1] != "" && entry[2] != "" ) {
				numSource++;
				numFile++;
			} else if ( entry[1] == "" ) {
				numFile++;
				numOld++;
			} else if ( entry[2] == "" ) {
				numSource++;
				numMissing++;
			}
			
		}
		
        lblNumSource.setText("Source: "+numSource);
        lblNumFile.setText("File: "+numFile);
        lblNumMissing.setText("Missing: "+numMissing);
        lblNumOld.setText("Old: "+numOld);
        
	}
	
	class MyTableModel extends AbstractTableModel {
	    
		private static final long serialVersionUID = -5434313385327397539L;
		private String[] columnNames = { "Key", "Source Value", "Language File Value" };
	    private Vector<String[]> tableData = new Vector<String[]>();

	    public int getColumnCount() {
	        return columnNames.length;
	    }

	    public int getRowCount() {
	        return tableData.size();
	    }

	    public String getColumnName(int col) {
	        return columnNames[col];
	    }

	    public String getValueAt(int row, int col) {
	        return tableData.get(row)[col];
	    }

	    public Class getColumnClass(int c) {
	        return getValueAt(0, c).getClass();
	    }
	    
	    public boolean isCellEditable(int row, int col) {
	    	
	        if (col == 2) {
	            return false;
	        } else {
	            return true;
	        }
	        
	    }
	    
	    public void setValueAt(String value, int row, int col) {
	        
	    	tableData.get(row)[col] = value;
	        fireTableCellUpdated(row, col);
	        
	    }
	    
	    public void addRow(String[] value) {
	        
	    	tableData.add(value);
	        this.fireTableRowsInserted(tableData.size()-1, tableData.size()-1);
	        
	    }
	    
	    public void deleteRow(int index) {
	        
	    	tableData.remove(index);
	        this.fireTableRowsDeleted(index, index);
	        
	    }
	    
	    public void setData(Vector<String[]> newData) {
	        
	    	tableData = newData;
	        this.fireTableRowsInserted(0, tableData.size()-1);
	        
	    }
	    
	    public Vector<String[]> getData() {
	        
	    	return tableData;
	        
	    }
	    
	}

	public class MyTableCellRenderer extends DefaultTableCellRenderer {
		
		private static final long serialVersionUID = 4503845066884103663L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	    	Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	        
	    	if ( value == "" ) {
	    		cell.setBackground( Color.red );
	        } else {
	        	cell.setBackground( Color.white );
	        }
	    	
	        return cell;
	        
	    }
	    
	}
	
	private void showGui() {
    	
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Language File Editor");
        frame.setPreferredSize(new Dimension(1000, 800));
        frame.setName("LANGFILEEDIT");
        LocationListener listener = new LocationListener();
        frame.addComponentListener(listener);
        frame.addWindowListener(listener);    
        
        tableModel = new MyTableModel();
        table = new JTable(tableModel);
        table.setDefaultRenderer(String.class, new MyTableCellRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        
        btnBrowseFolder = new JButton("Browse");
        btnBrowseFile = new JButton("Browse");
        btnAdoptMissing = new JButton("Adopt Missing Entries");
        btnDeleteOld = new JButton("Delete Old Entries");
        btnAdopt = new JButton("Adopt Default");
        btnDelete = new JButton("Delete Key");
        btnEdit = new JButton("Edit Value");
        btnSave = new JButton("Save As");
        btnReload = new JButton("Reload");
        
        btnBrowseFolder.addActionListener(this);
        btnBrowseFile.addActionListener(this);
        btnAdoptMissing.addActionListener(this);
        btnDeleteOld.addActionListener(this);
        btnDelete.addActionListener(this);
        btnEdit.addActionListener(this);
        btnSave.addActionListener(this);
        btnReload.addActionListener(this);
        btnAdopt.addActionListener(this);
        
        lblFolder = new JLabel("Source Folder: ");
        lblFile = new JLabel("Language File: ");
        lblFolderValue = new JLabel("/.../...");
        lblFileValue = new JLabel("/.../...");
        lblNumSource = new JLabel("Source: ");
        lblNumFile = new JLabel("File: ");
        lblNumMissing = new JLabel("Missing:");
        lblNumOld = new JLabel("Old: ");
	      
        JPanel main = new JPanel(new BorderLayout(5,5));
	    main.setBorder(new EmptyBorder(10,10,10,10));
	    frame.setContentPane(main);
        
	    JPanel top = new JPanel(new BorderLayout(5,5));
	    JPanel bottom = new JPanel(new BorderLayout());

	    JPanel top1 = new JPanel(new BorderLayout(5,5));
	    JPanel top2 = new JPanel(new BorderLayout(5,5));
	    JPanel infos1 = new JPanel(new BorderLayout(5,5));
	    JPanel infos2 = new JPanel(new FlowLayout());
	    JPanel buttons = new JPanel(new FlowLayout());
	    
	    top.add(top1, BorderLayout.PAGE_START);
	    top.add(top2, BorderLayout.PAGE_END);
	    
	    bottom.add(infos1 , BorderLayout.PAGE_START);
	    bottom.add(buttons, BorderLayout.LINE_END);
	    
	    top1.add(lblFolder, BorderLayout.LINE_START);
	    top1.add(lblFolderValue, BorderLayout.CENTER);
	    top1.add(btnBrowseFolder, BorderLayout.EAST);
	    
	    top2.add(lblFile, BorderLayout.LINE_START);
	    top2.add(lblFileValue, BorderLayout.CENTER);
	    top2.add(btnBrowseFile, BorderLayout.EAST);
	    
	    infos1.add(infos2, BorderLayout.LINE_START);
	    
	    infos2.add(lblNumSource);
	    infos2.add(lblNumFile);
	    infos2.add(lblNumMissing);
	    infos2.add(lblNumOld);

	    buttons.add(btnSave);
	    buttons.add(btnReload);
	    buttons.add(btnDeleteOld);
	    buttons.add(btnAdoptMissing);
	    buttons.add(btnDelete);
	    buttons.add(btnAdopt);
	    buttons.add(btnEdit);
	    
	    main.add(top, BorderLayout.PAGE_START);
	    main.add(new JScrollPane(table), BorderLayout.CENTER);
	    main.add(bottom, BorderLayout.PAGE_END);
        
        frame.setResizable(true);
        frame.pack();
        frame.setVisible(true);
        
    }

	public void actionPerformed(ActionEvent e) {

		if ( e.getSource().equals(btnBrowseFolder) ) {
			
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int value = chooser.showOpenDialog(frame);
			
		    if ( value == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile().isDirectory() ) {
			    sourceFolder = chooser.getSelectedFile();
			    lblFolderValue.setText(sourceFolder.getAbsolutePath());
			    initList();
		    }
		    
		} else if ( e.getSource().equals(btnBrowseFile) ) {

			JFileChooser chooser = new JFileChooser();
			int vlaue = chooser.showOpenDialog(frame);
			
		    if ( vlaue == JFileChooser.APPROVE_OPTION) {
			    languageFile = chooser.getSelectedFile();
			    lblFileValue.setText(languageFile.getAbsolutePath());
			    initList();
		    }
		    
		} else if ( e.getSource().equals(btnSave) ) {
			
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogType(JFileChooser.SAVE_DIALOG);
			int vlaue = chooser.showOpenDialog(frame);
			
		    if ( vlaue == JFileChooser.APPROVE_OPTION) {
			    
		    	File file = chooser.getSelectedFile();
			    Vector<String[]> data = tableModel.getData();
		    	String content = "";
			    
		    	for ( String[] entry : data ) {
		    		
		    		if ( entry[2] != "" ) {
		    			content += entry[0] + " = " + entry[2] + "\n";
		    		}
		    		
		    	}
			    
		    	JDUtilities.writeLocalFile(file, content);
		        JOptionPane.showMessageDialog(frame, "Saved.");
		    	
		    }
			
		} else if ( e.getSource().equals(btnEdit) ) {

			int i = table.getSelectedRow();
			
			if ( i > -1 ) {
				
				String k = tableModel.getValueAt(i, 0);
				String s = tableModel.getValueAt(i, 1);
				String f = tableModel.getValueAt(i, 2);
				EditDialog dialog = new EditDialog(frame, k, s, f);
				
				if ( dialog.value != null ) {
					tableModel.setValueAt(dialog.value, i, 2);
				}
				
			}
			
			setInfoLabels();
			
		} else if ( e.getSource().equals(btnDelete) ) {
			
			int i = table.getSelectedRow();
			
			if ( i > -1 ) {
				tableModel.deleteRow(i);
			}
			
			setInfoLabels();
			
		} else if ( e.getSource().equals(btnDeleteOld) ) {

	        int ret = JOptionPane.showConfirmDialog(frame,
	        		"Attention!\nSome Keys are dynamic or simply could not be found.\nYou shouldn't use this function.\nGo on?");
	        if ( ret > 0 ) return;
	        
	        Vector<String[]> data = tableModel.getData();
			
			for ( int i=0; i<data.size(); i++ ) {
				
				if ( data.get(i)[1] == "" && data.get(i)[2] != "" ) {
					//System.out.println(data.get(i)[0]);
					tableModel.deleteRow(i);
					i--;
				}
				
			}
			
			setInfoLabels();
			
		} else if ( e.getSource().equals(btnAdoptMissing) ) {
			
			Vector<String[]> data = tableModel.getData();
			
			for ( int i=0; i<data.size(); i++ ) {
				
				if ( data.get(i)[2] == "") {
					tableModel.setValueAt(data.get(i)[1], i, 2);
				}
				
			}
			
			setInfoLabels();
			
		} else if ( e.getSource().equals(btnReload) ) {
			
			initList();
			
		} else if ( e.getSource().equals(btnAdopt) ) {
			
			int i = table.getSelectedRow();
			
			if ( i > -1 ) {
				
				String def = tableModel.getValueAt(i, 1);
				tableModel.setValueAt(def, i, 2);
				
			}
			
			setInfoLabels();
			
		}
		
	}
	
	private Vector<String[]> getData(Vector<String[]> sourceEntries, Vector<String[]> fileEntries) {
		
		Vector<String[]> data = new Vector<String[]>();
		
		for ( String[] entry : sourceEntries ) {
			
			String[] temp = new String[3];
			temp[0] = entry[0];
			temp[1] = entry[1];
			temp[2] = getValue(fileEntries, entry[0]);
			if ( temp[2] == null ) temp[2] = "";
			
			data.add(temp);
			
		}
		
		for ( String[] entry : fileEntries ) {
			
			if ( getValue(data, entry[0]) == null ) {
				
				String[] temp = new String[3];
				temp[0] = entry[0];
				temp[1] = "";
				temp[2] = entry[1];
				
				data.add(temp);
				
			}
			
		}
		
		Collections.sort(data, new StringArrayComparator());
		return data;
		
	}
	
	private String getValue(Vector<String[]> vector, String key) {
		
		String result = null;
		
		for ( String[] entry : vector ) {
			
			if ( entry[0].equals(key) ) {
				result = entry[1];
				break;
			}
			
		}
		
		return result;
		
	}
	
	private Vector<String[]> getLanguageFileEntries(File file) {
		
		String content = JDUtilities.getLocalFile(file);
		Vector<String[]> entries = new Vector<String[]>();
		String keys = "";
		
		Regex regex = new Regex(Pattern.compile("\\s*(.*?)\\s*=\\s*(.*?)\\s*\\n").matcher(content));
		
		for ( String[] match : regex.getMatches() ) {

			if ( !keys.contains(match[0].trim()) ) {
				keys += match[0].trim() + "\n";
				String[] temp = new String[] { match[0].trim(), match[1].trim() };
				entries.add(temp);
			}
			
		}
		
		Collections.sort(entries, new StringArrayComparator());
		return entries;
		
	}
	
	private Vector<String[]> getSourceEntries(File dir) {
		
		Vector<String> fileContents = getFileContents(dir, "java");
		Vector<String[]> entries = new Vector<String[]>();
		String keys = "";
		
		for ( String file : fileContents ) {
			
			// "JDLocale.L(\\s*?\"\\s*?(.*?)\\s*?\"\\s*?(,\\s*?\"\\s*?(.*?)\\s*?\"\\s*?){0,1}?)"
			// "JDLocale.L\\(\\s*?\"\\s*?(.*?)\\s*?\"\\s*?(,\\s*?\"\\s*?(.*?)\\s*?\"\\s*?)*?\\)"
			Regex regex = new Regex(Pattern.compile("JDLocale.L" +
					"[\\n\\s]*?\\([\\n\\s]*?" +
					"\"\\s*?(.*?)\\s*?\"" +
					"[\\n\\s]*?(,[\\n\\s]*?" +
					"\"\\s*?(.*?)\\s*?\"" +
					"[\\n\\s]*?)*\\)").matcher(file));
			
			for ( String[] match : regex.getMatches() ) {
				
				if ( !keys.contains(match[0].trim()+"\n") ) {
					
					keys += match[0].trim() + "\n";
					String k = match[0].trim();
					String v = "<no default value>";
					
					try {
						v = match[2].trim();
					} catch (Exception e) { }
					
					String[] temp = new String[] { k, v };
					entries.add(temp);
					
				}
				
			}
			
		}
		
		Collections.sort(entries, new StringArrayComparator());
		return entries;
		
	}
	
	private Vector<String> getFileContents(File directory, String filter) {

		Vector<String> fileContents = new Vector<String>();
		File[] entries = directory.listFiles();
		
		for ( int i=0; i<entries.length; i++ ) {
			
			if ( entries[i].isDirectory() ) {
				
				fileContents.addAll(getFileContents(entries[i], filter));
				
			} else if ( entries[i].isFile ( ) ) {
				
				String extension = JDUtilities.getFileExtension(entries[i]);
				
				if ( extension.equals(filter) ) {
					fileContents.add(JDUtilities.getLocalFile(entries[i]));
				}
				
			}
			
		}
		
		return fileContents;
		
	}
	
	private class StringArrayComparator implements Comparator {
	  
		public int compare( Object o1, Object o2 ) {
		    
			String[] s1 = (String[]) o1;
			String[] s2 = (String[]) o2;
			
			return s1[0].compareTo(s2[0]);
			
		}
	  
	}
	
	private class EditDialog extends JDialog implements ActionListener {
		
		private static final long serialVersionUID = 1L;
		
	    private JButton     btnOK = new JButton("OK");
	    private JButton     btnCancel = new JButton("Cancel");
	    private JFrame      owner;
	    private JLabel		lblKey = new JLabel("");
	    private JTextArea	taSourceValue = new JTextArea(5,20);
	    private JTextArea	taFileValue = new JTextArea(5,20);
	    
	    public String 		value;
	    
	    public EditDialog(JFrame owner, String key, String sourceValue, String fileValue) {
	        
	    	super(owner);
	        this.owner = owner;
	        
	        setModal(true);
	        setLayout(new BorderLayout(5,5));
	        setTitle("Edit Value");
	        getRootPane().setDefaultButton(btnOK);
		    
	        btnOK.addActionListener(this);
	        btnCancel.addActionListener(this);
	        
	        lblKey.setText("Key: "+key);
	        taSourceValue.setText(sourceValue);
	        taFileValue.setText(fileValue);
	        taSourceValue.setEditable(false);
	        
	        JPanel main = new JPanel(new BorderLayout(5,5));
	        main.setBorder(new EmptyBorder(10,10,10,10));
	        JPanel fields = new JPanel(new BorderLayout(5,5));
	        JPanel buttons1 = new JPanel(new BorderLayout(5,5));
	        JPanel buttons2 = new JPanel(new FlowLayout());
	        
	        main.add(lblKey, BorderLayout.PAGE_START);
	        main.add(fields, BorderLayout.CENTER);
	        main.add(buttons1, BorderLayout.PAGE_END);
	        
	        fields.add(new JScrollPane(taSourceValue), BorderLayout.PAGE_START);
	        fields.add(new JScrollPane(taFileValue), BorderLayout.PAGE_END);

	        buttons1.add(buttons2, BorderLayout.LINE_END);
	        buttons2.add(btnOK);
	        buttons2.add(btnCancel);
	        
	        setContentPane(main);
	        pack();
	        setLocation(JDUtilities.getCenterOfComponent(owner, this));
	        setVisible(true);
	        
	    }
	    
	    public void actionPerformed(ActionEvent e) {
	    	
	        if (e.getSource() == btnOK) {

	        	value = taFileValue.getText();
	        	dispose();
			    owner.setVisible(true);
	        	
	        } else if (e.getSource() == btnCancel) {

	        	value = null;
	        	dispose();
			    owner.setVisible(true);
	        	
	        }
	        
	    }
	    
	}
	
}
