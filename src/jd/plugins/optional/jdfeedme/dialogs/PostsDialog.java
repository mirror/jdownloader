package jd.plugins.optional.jdfeedme.dialogs;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jd.gui.UserIO;
import jd.gui.swing.components.table.JDRowHighlighter;
import jd.plugins.optional.jdfeedme.JDFeedMeFeed;
import jd.plugins.optional.jdfeedme.posts.JDFeedMePost;
import jd.plugins.optional.jdfeedme.posts.PostsTable;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;

/* CODE_FOR_INTERFACE_5_START
public class PostsDialog extends AbstractDialog
CODE_FOR_INTERFACE_5_END */
/* CODE_FOR_INTERFACE_7_START */
public class PostsDialog extends AbstractDialog<Integer> {
/* CODE_FOR_INTERFACE_7_END */
	private static final long serialVersionUID = 7516288311787228152L;
	private JDFeedMeFeed feed;
	private ArrayList<JDFeedMePost> posts;
	private PostsTable table;

	public PostsDialog(int flag,JDFeedMeFeed feed, ArrayList<JDFeedMePost> posts) 
	{
		super(flag, "View Posts for "+feed.getAddress(), null, "OK", "Cancel");
		this.feed = feed;
		this.posts = posts;

		/* CODE_FOR_INTERFACE_5_START
	    init();
	    CODE_FOR_INTERFACE_5_END */
	}

	@Override
	/* CODE_FOR_INTERFACE_5_START
	public JComponent contentInit() {
	CODE_FOR_INTERFACE_5_END */
	/* CODE_FOR_INTERFACE_7_START */
	public JComponent layoutDialogContent() {
	/* CODE_FOR_INTERFACE_7_END */
		JPanel contentpane = new JPanel(new MigLayout("ins 0, w 750", "[grow, fill]", ""));
		  
		contentpane.add(new JScrollPane(table = new PostsTable(posts,feed)));
		
		table.addJDRowHighlighter(new JDRowHighlighter(new Color(204, 255, 170)) 
		{
			  
			@Override
		    public boolean doHighlight(final Object obj) 
			{
				return !((JDFeedMePost) obj).getAdded().equalsIgnoreCase(JDFeedMePost.ADDED_NO);
			}
		
		});
		
		table.addJDRowHighlighter(new JDRowHighlighter(new Color(255, 243, 197)) 
		{
			  
			@Override
		    public boolean doHighlight(final Object obj) 
			{
				return ((JDFeedMePost) obj).getNewpost();
			}
		
		});
		  
		table.getModel().refreshModel();
		table.getModel().fireTableDataChanged();
		  
		return contentpane;
	}

	protected void packed() {
	
	}

	public boolean isResultOK()
	{
	    /* CODE_FOR_INTERFACE_5_START
	    if ((this.getReturnValue() & UserIO.RETURN_OK) == 0) return false;
	    CODE_FOR_INTERFACE_5_END */
	    /* CODE_FOR_INTERFACE_7_START */
	    if ((this.createReturnValue() & UserIO.RETURN_OK) == 0) return false;
	    /* CODE_FOR_INTERFACE_7_END */
	    
		return true;
	}
	
	/* CODE_FOR_INTERFACE_7_START */
	@Override
	protected Integer createReturnValue() { return this.getReturnmask(); }
	/* CODE_FOR_INTERFACE_7_END */

}
