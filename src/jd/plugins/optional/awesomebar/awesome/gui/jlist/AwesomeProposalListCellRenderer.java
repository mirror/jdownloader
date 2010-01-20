package jd.plugins.optional.awesomebar.awesome.gui.jlist;

import java.awt.Component;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import net.miginfocom.swing.MigLayout;

public class AwesomeProposalListCellRenderer extends JPanel implements ListCellRenderer {
	
	private static final MigLayout miglayout = new MigLayout("insets 0,fill");
	/**
	 * 
	 */
	private static final long serialVersionUID = -5846868530031974952L;

	
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		this.removeAll();
		this.add((Component) value);
		this.setLayout(miglayout);
		if (isSelected) {
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		} else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		setEnabled(list.isEnabled());
		setFont(list.getFont());
		setOpaque(true);

		return this;
	}

}