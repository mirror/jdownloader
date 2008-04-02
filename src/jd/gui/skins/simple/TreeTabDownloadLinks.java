package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JScrollPane;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

public class TreeTabDownloadLinks extends DownloadLinksView{

	private DownloadTreeTable internalTreeTable;

	public TreeTabDownloadLinks( SimpleGUI parent){
		super(parent, new BorderLayout());
		internalTreeTable = new DownloadTreeTable( new DownloadTreeTableModel( packages, allLinks));
		//right now, editing is not supported
		internalTreeTable.setEditable(false);
		//internalTreeTable.setRootVisible(true);

		JScrollPane scrollPane = new JScrollPane(internalTreeTable);
		scrollPane.setPreferredSize(new Dimension(800, 450));
		this.add(scrollPane);		
	}

	@Override
	public void fireTableChanged() {
		// TODO signed: selection related stuff
		internalTreeTable.getDownladTreeTableModel().refreshAll();
	}


	@Override
	public Vector<DownloadLink> getLinks() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void moveSelectedItems(int direction) {
		// TODO Auto-generated method stub

	}


	@Override
	public void removeSelectedLinks() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void checkColumnSize() {
		// TODO Auto-generated method stub

	}




	protected static class DownloadTreeTable extends JXTreeTable{

		public DownloadTreeTable(DownloadTreeTableModel treeModel) 
		{
			super(treeModel);
		}

		public DownloadTreeTableModel getDownladTreeTableModel(){
			return (DownloadTreeTableModel) getTreeTableModel();
		}
	}



	protected static class DownloadTreeTableModel extends AbstractTreeTableModel{

		/** table column names */
		static protected String[]  cNames = {"Nr.", "Datei", "Hoster", "Status", "Fortschritt"};

		private Vector<DownloadLink> allLinks = null;
		private Vector<FilePackage> packages = null;


		/** index of tree column */
		public static final int COL_NUMBER = 0;
		public static final int COL_FILE = 1;
		public static final int COL_HOSTER = 2;
		public static final int COL_STATUS = 3;
		public static final int COL_PROGRESS = 4;

		/**
		 * Creates a {@link ProjectsTreeTableModel} with an empty ProjectList.
		 * 
		 */
		public DownloadTreeTableModel( Vector<FilePackage> packages, Vector<DownloadLink> allLinks) {
			this(new String("Wurzel"), packages, allLinks);

		}

		/**
		 * Creates a {@link ProjectsTreeTableModel}
		 * 
		 * @param aList the ProjectList to start out with.
		 */
		public DownloadTreeTableModel(String rootName, Vector<FilePackage> packages, Vector<DownloadLink> allLinks) {
			super(rootName);
			this.allLinks = allLinks;
			this.packages = packages;
		}

		public void fillWithDummyData(){
			createDummyData(packages, allLinks);
		}

		public void refreshAll(){
			modelSupport.fireNewRoot();
		}

		static private void createDummyData(Vector<FilePackage> packages, Vector<DownloadLink>allLinks){
			FilePackage filePackage = null;

			for(int i=0; i<3; ++i){
				filePackage = new FilePackage();
				filePackage.setDownloadLinks(new Vector<DownloadLink>());
				filePackage.setName("pak #"+i);
				packages.add(filePackage);
			}

			DownloadLink link = null;

			for(int i=0; i<9; ++i){
				link = new DownloadLink(null, "dl #"+i, "rapidshare", "http://rapdishare.com/"+i+".html", true);
				int mod = i%packages.size();

				filePackage =packages.get(mod); 
				link.setFilePackage(filePackage);

				filePackage.getDownloadLinks().add(link);
				allLinks.add(link);
			}
		}


		/**
		 * How many columns do we display
		 */
		public int getColumnCount() {
			return cNames.length;
		}


		@Override
		public String getColumnName(int column) {
			if (column<0 || column >= cNames.length) {
				return "dummy";
			};
			return cNames[column];
		}


		/**
		 * What is shown in a cell column for a node.
		 */
		public Object getValueAt(Object node, int column) {
			String value = null;

			if(node instanceof DownloadLink ) {
				DownloadLink downloadLink = (DownloadLink)node;
				switch(column){
				case COL_NUMBER:

					if( null == downloadLink.getFilePackage()){
						value = "-1";
					}else{
						value = downloadLink.getFilePackage().getDownloadLinks().indexOf(downloadLink)+"";
					}
					break;
				case COL_FILE:
					if (downloadLink.getFilePackage() == null) {
						value =  downloadLink.getName();
					}else{
						String str =(downloadLink.getFilePackage().getDownloadDirectoryName() + "/" + downloadLink.getName());
						value =  str;
					}
					break;
				case COL_HOSTER:
					value = downloadLink.getHost();
					break;
				case COL_STATUS:
					value = downloadLink.getStatusText();
					break;
				case COL_PROGRESS:
					value = "ToDo";
					break;
				default:
					value = "value missing column="+column;
				break;
				}
			}else if( node instanceof FilePackage){
				FilePackage filePackage = (FilePackage)node;
				switch(column){
				case COL_NUMBER:
					value = new Integer(packages.indexOf(filePackage)).toString();
					break;
				case COL_FILE:
					value = filePackage.getName();
					break;
				case COL_HOSTER:
					value = "drop down";
					break;
				case COL_STATUS:
					value = "active/wait";
					break;
				case COL_PROGRESS:
					value = "2/"+filePackage.getDownloadLinks().size();
					break;
				default:
					value = "value missing column="+column;
				break;
				}
			}else if( node instanceof String ){
				value = (0==column)?node.toString():"";
			}else{
				System.out.println("node.class: "+node.getClass());
			}

			return value;
		}

		/**
		 * Returns the child of <code>parent</code> at index <code>index</code>
		 * in the parent's
		 * child array.  <code>parent</code> must be a node previously obtained
		 * from this data source. This should not return <code>null</code>
		 * if <code>index</code>
		 * is a valid index for <code>parent</code> (that is <code>index >= 0 &&
		 * index < getChildCount(parent</code>)).
		 *
		 * @param   parent  a node in the tree, obtained from this data source
		 * @return  the child of <code>parent</code> at index <code>index</code>
		 * 
		 * Have to implement this:
		 */

		public Object getChild(Object parent, int index) {
			Object child = null;

			if( parent instanceof String){
				child = packages.get(index);
			}else if( parent instanceof FilePackage){
				FilePackage pack = (FilePackage) parent;
				child = pack.getDownloadLinks().get(index);
			}else if( parent instanceof DownloadLink){
				//for now, DownloadLinks do not have Children
			}

			return child;
		}

		/**
		 * Returns the number of children of <code>parent</code>.
		 * Returns 0 if the node
		 * is a leaf or if it has no children.  <code>parent</code> must be a node
		 * previously obtained from this data source.
		 *
		 * @param   parent  a node in the tree, obtained from this data source
		 * @return  the number of children of the node <code>parent</code>
		 */
		public int getChildCount(Object parent) {
			int count = 0;

			if( parent instanceof String){
				count = packages.size();
			}else if( parent instanceof FilePackage){
				FilePackage pack = (FilePackage) parent;
				count = pack.getDownloadLinks().size();
			}else if( parent instanceof DownloadLink){
				count = 0;
			}

			return count;
		}

		public int getIndexOfChild(Object parent, Object child) {
			int index = -1;
			if( parent instanceof String ){
				index = packages.indexOf(child);
			}else if (parent instanceof FilePackage){
				index = ((FilePackage)parent).getDownloadLinks().indexOf(child);
			}else if( parent instanceof DownloadLink){
				index = -1;
			}

			return index;
		}
	}








}
