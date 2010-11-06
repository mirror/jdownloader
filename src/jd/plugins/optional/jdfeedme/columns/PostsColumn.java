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

package jd.plugins.optional.jdfeedme.columns;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.SwingConstants;

import org.jdesktop.swingx.renderer.JRendererLabel;

import jd.gui.UserIO;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.plugins.optional.jdfeedme.JDFeedMe;
import jd.plugins.optional.jdfeedme.JDFeedMeFeed;
import jd.plugins.optional.jdfeedme.JDFeedMeTableModel;
import jd.plugins.optional.jdfeedme.dialogs.PostsDialog;
import jd.plugins.optional.jdfeedme.posts.JDFeedMePost;
import jd.utils.JDTheme;

public class PostsColumn extends JDTableColumn {

    private static final long serialVersionUID = 7660856282857573284L;
    
    private JDFeedMeTableModel table;
    private JRendererLabel labelRend;
    private JRendererLabel labelLink;
    private Object obj;

    public PostsColumn(String name, JDFeedMeTableModel table) {
        super(name, table);
        
        this.table = table;
        
        labelRend = new JRendererLabel();
        labelRend.setBorder(null);
        labelRend.setHorizontalAlignment(SwingConstants.CENTER);
        labelRend.setIcon(JDTheme.II("gui.images.search", 16, 16));
        labelRend.setToolTipText("View recent posts");
        
        labelLink = new JRendererLabel();
        labelLink.setBorder(null);
        labelLink.setHorizontalAlignment(SwingConstants.CENTER);
        labelLink.setIcon(JDTheme.II("gui.images.search", 16, 16));
        labelLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        labelLink.addMouseListener(new JDMouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent evt) {
            	
            }

            @Override
            public void mouseExited(MouseEvent evt) {
            }

            public void mouseClicked(MouseEvent e) {
                actionPerformed();
            }

        });
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isEditable(Object obj) {
        return true;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
    	this.obj = value;
    	
    	if (((JDFeedMeFeed)value).getNewposts()) labelLink.setIcon(JDTheme.II("gui.images.taskpanes.linkgrabber", 16, 16));
    	else labelLink.setIcon(JDTheme.II("gui.images.search", 16, 16));
    	
    	labelLink.setBackground(backgroundselected);
        return labelLink;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        
    	if (((JDFeedMeFeed)value).getNewposts()) labelRend.setIcon(JDTheme.II("gui.images.taskpanes.linkgrabber", 16, 16));
    	else labelRend.setIcon(JDTheme.II("gui.images.search", 16, 16));
    	
    	return labelRend;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }
    
    public void actionPerformed() {
    	JDFeedMeFeed feed = ((JDFeedMeFeed)this.obj);
    	ArrayList<JDFeedMePost> posts = table.getPosts().get(feed.getUniqueid());
    	if (posts == null) posts = new ArrayList<JDFeedMePost>();
    	
    	/* CODE_FOR_INTERFACE_5_START
    	int flags = UserIO.NO_COUNTDOWN | UserIO.NO_CANCEL_OPTION;
    	CODE_FOR_INTERFACE_5_END */
    	/* CODE_FOR_INTERFACE_7_START */
    	int flags = UserIO.NO_CANCEL_OPTION;
    	/* CODE_FOR_INTERFACE_7_END */
    	
    	PostsDialog dialog = new PostsDialog(flags, feed, posts);
    	
    	/* CODE_FOR_INTERFACE_7_START */
        dialog.displayDialog();
        /* CODE_FOR_INTERFACE_7_END */
    	
    	JDFeedMe.getGui().setFeedNewposts(feed, false);
    	this.fireEditingStopped();
    }
    
    @Override
    protected int getMaxWidth() {
        return 50;
    }

}