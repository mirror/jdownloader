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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;

import jd.HostPluginWrapper;
import jd.gui.swing.components.JDLabelContainer;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.views.settings.JDLabelListRenderer;
import jd.plugins.optional.jdfeedme.JDFeedMeFeed;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class HosterColumn extends JDTableColumn {

    private static final long serialVersionUID = 4660856288527573254L;
    private JRendererLabel jlr;
    private JComboBox hosters;
    private static ArrayList<HosterLabel> labels = null;

    public HosterColumn(String name, JDTableModel table) {
        super(name, table);
        
        this.setClickstoEdit(2);
        
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        
        if (labels == null)
        {
        	ArrayList<HostPluginWrapper> plugins = HostPluginWrapper.getHostWrapper();
            Collections.sort(plugins, new Comparator<HostPluginWrapper>() {
                public int compare(HostPluginWrapper a, HostPluginWrapper b) {
                    return a.getHost().compareToIgnoreCase(b.getHost());
                }
            });
            
	        labels = new ArrayList<HosterLabel>();
	        labels.add(new HosterLabel(JDFeedMeFeed.HOSTER_ANY_HOSTER,null));
	        labels.add(new HosterLabel(JDFeedMeFeed.HOSTER_ANY_PREMIUM,null));
	        for (final HostPluginWrapper plugin : plugins)
	        {
	        	labels.add(new HosterLabel(plugin.getLabel(),plugin.getIcon()));
	        }
        }
        
        hosters = new JComboBox(labels.toArray());
        hosters.setRenderer(new JDLabelListRenderer());
        //hosters.setBorder(null);
    }

    @Override
    public Object getCellEditorValue() {
        return hosters.getSelectedItem().toString();
    }

    @Override
    public boolean isEditable(Object obj) {
        return isEnabled(obj);
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
        for (final HosterLabel label : labels)
        {
        	if (label.toString().equalsIgnoreCase(((JDFeedMeFeed) value).getHoster()))
        	{
        		hosters.setSelectedItem(label);
        		return hosters;
        	}
        }
    	hosters.setSelectedIndex(0);
        return hosters;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        jlr.setText(((JDFeedMeFeed) value).getHoster());
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
        ((JDFeedMeFeed) object).setHoster(value.toString());
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

}

class HosterLabel implements JDLabelContainer
{
	private ImageIcon icon;
	private String label;
	
	public HosterLabel(String label, ImageIcon icon)
	{
		this.label = label;
		this.icon = icon;
	}
	
	public ImageIcon getIcon() {
        return icon;
    }

    public String getLabel() {
        return label;
    }
    
    @Override
    public String toString() {
        return label;
    }
}