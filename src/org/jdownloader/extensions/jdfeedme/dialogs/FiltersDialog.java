package org.jdownloader.extensions.jdfeedme.dialogs;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.UserIO;
import jd.gui.swing.components.JDTextArea;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.extensions.jdfeedme.JDFeedMeFeed;

/* CODE_FOR_INTERFACE_5_START
public class FiltersDialog extends AbstractDialog {
CODE_FOR_INTERFACE_5_END */
/* CODE_FOR_INTERFACE_7_START */
public class FiltersDialog extends AbstractDialog <Integer> {
/* CODE_FOR_INTERFACE_7_END */

private static final long serialVersionUID = 3516288388787228122L;
private JDTextArea textArea;
private JCheckBox checkboxDofilters;
private JCheckBox checkboxTitle;
private JCheckBox checkboxDescription;
private JDFeedMeFeed feed;

public FiltersDialog(int flag,JDFeedMeFeed feed) {
  super(flag, "Edit Filters for "+feed.getAddress(), null, "OK", "Cancel");
  this.feed = feed;
  
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
  JPanel contentpane = new JPanel(new MigLayout("ins 0, wrap 2, w 400", "[grow, fill]", "[]12[][]12[][][][]16[][][]"));
  
  checkboxDofilters = new JCheckBox("Limit automatic downloads to posts which match my filters", feed.getDoFilters());
  contentpane.add(checkboxDofilters, "span 2");
  
  JLabel label = new JLabel("Filters:");
  Font f = label.getFont();
  label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
  contentpane.add(label,"span 2");
  
  textArea = new JDTextArea(feed.getFilters());
  if (!feed.getDoFilters()) textArea.setEnabled(false);
  contentpane.add(new JScrollPane(textArea),"h 130!, span 2");
  
  label = new JLabel("Examples:");
  f = label.getFont();
  label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
  contentpane.add(label,"span 2");
    
  contentpane.add(new JLabel("blue 720"));
  label = new JLabel("matches 'Blue S01E02 720p' but also 'Blueberry 720p'");
  label.setForeground(Color.gray);
  contentpane.add(label);
  
  contentpane.add(new JLabel("\"blue\" 720"));
  label = new JLabel("only matches 'Blue S01E02 720p'");
  label.setForeground(Color.gray);
  contentpane.add(label);
  
  contentpane.add(new JLabel("blue -720"));
  label = new JLabel("matches 'Blue S01E02 HDTV' but not 'Blue S01E02 720p'");
  label.setForeground(Color.gray);
  contentpane.add(label);
  
  label = new JLabel("Where To Search:");
  f = label.getFont();
  label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
  contentpane.add(label,"span 2");
  
  checkboxTitle = new JCheckBox("Post Title", feed.getFiltersearchtitle());
  contentpane.add(checkboxTitle);
  
  checkboxDescription = new JCheckBox("Post Description", feed.getFiltersearchdesc());
  contentpane.add(checkboxDescription);
  
  checkboxDofilters.addChangeListener(new ChangeListener() {

    /* CODE_FOR_INTERFACE_5_START  
	@Override
	CODE_FOR_INTERFACE_5_END */
	public void stateChanged(ChangeEvent e) {
		textArea.setEnabled(checkboxDofilters.isSelected());
	}
	  
  });
  
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

public String getResultFilters()
{
	return textArea.getText();
}

public boolean getResultCheckboxTitle()
{
	return checkboxTitle.isSelected();
}

public boolean getResultCheckboxDescription()
{
	return checkboxDescription.isSelected();
}

public boolean getResultCheckboxDofilters()
{
	return checkboxDofilters.isSelected();
}

/* CODE_FOR_INTERFACE_7_START */
@Override
protected Integer createReturnValue() { return this.getReturnmask(); }
/* CODE_FOR_INTERFACE_7_END */

}
