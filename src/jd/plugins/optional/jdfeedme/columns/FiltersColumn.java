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

import javax.swing.SwingConstants;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.plugins.optional.jdfeedme.JDFeedMeFeed;
import jd.plugins.optional.jdfeedme.dialogs.FiltersDialog;
import jd.utils.JDTheme;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class FiltersColumn extends JDTableColumn {

    private static final long serialVersionUID = 8660856288827573254L;
    
    private JRendererLabel labelRend;
    private JRendererLabel labelLink;
    private Object obj;

    public FiltersColumn(String name, JDTableModel table) {
        super(name, table);
        
        labelRend = new JRendererLabel();
        labelRend.setBorder(null);
        labelRend.setHorizontalAlignment(SwingConstants.CENTER);
        labelRend.setIcon(JDTheme.II("gui.images.config.home", 16, 16));
        labelRend.setToolTipText("Define download filters");
        labelRend.setOpaque(false);
        
        labelLink = new JRendererLabel();
        labelLink.setBorder(null);
        labelLink.setHorizontalAlignment(SwingConstants.CENTER);
        labelLink.setIcon(JDTheme.II("gui.images.config.home", 16, 16));
        labelLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        labelLink.setOpaque(false);

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
        return ((JDFeedMeFeed) obj).isEnabled();
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
    	this.obj = value;
        return labelLink;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
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
    	
    	/* CODE_FOR_INTERFACE_5_START
        int flags = UserIO.NO_COUNTDOWN;
        CODE_FOR_INTERFACE_5_END */
        /* CODE_FOR_INTERFACE_7_START */
        int flags = 0;
        /* CODE_FOR_INTERFACE_7_END */
    	
    	FiltersDialog dialog = new FiltersDialog(flags, feed);
    	
    	/* CODE_FOR_INTERFACE_7_START */
        dialog.displayDialog();
        /* CODE_FOR_INTERFACE_7_END */
    	
    	if (dialog.isResultOK())
    	{
    		feed.setFilters(dialog.getResultFilters());
    		feed.setDoFilters(dialog.getResultCheckboxDofilters());
    		feed.setFiltersearchtitle(dialog.getResultCheckboxTitle());
    		feed.setFiltersearchdesc(dialog.getResultCheckboxDescription());
    	}
    }
    
    @Override
    protected int getMaxWidth() {
        return 60;
    }

}