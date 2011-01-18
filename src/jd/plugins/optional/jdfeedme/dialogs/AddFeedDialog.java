package jd.plugins.optional.jdfeedme.dialogs;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.UserIO;
import jd.gui.swing.components.JDTextField;
import jd.plugins.optional.jdfeedme.JDFeedMeFeed;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;

/* CODE_FOR_INTERFACE_5_START
public class AddFeedDialog extends AbstractDialog {
CODE_FOR_INTERFACE_5_END */
/* CODE_FOR_INTERFACE_7_START */
public class AddFeedDialog extends AbstractDialog<Integer> {
/* CODE_FOR_INTERFACE_7_END */

private static final long serialVersionUID = 3516288388787228122L;

private JDTextField textFieldAddress;
private JComboBox comboGetOld;
private JCheckBox checkboxDofilters;

public AddFeedDialog(int flag) 
{
  super(flag, "Add Feed", null, "Add", "Cancel");
  
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
   
  JPanel contentpane = new JPanel(new MigLayout("ins 0,wrap 2", "[fill]","[]12[][][]12"));
  
  contentpane.add(new JLabel("Add an RSS feed which links to downloadable content"),"span 2");
  
  contentpane.add(new JLabel("Feed Address:"));
  textFieldAddress = new JDTextField();
  textFieldAddress.setText("http://");
  contentpane.add(textFieldAddress,"w 300!");
  
  contentpane.add(new JLabel("Get Old Posts:"));
  comboGetOld = new JComboBox(JDFeedMeFeed.GET_OLD_OPTIONS);
  comboGetOld.setSelectedIndex(0);
  contentpane.add(comboGetOld);
  
  checkboxDofilters = new JCheckBox("Limit automatic downloads to posts which match my filters", false);
  contentpane.add(checkboxDofilters, "span 2");
  
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

public String getResultAddress()
{
	return textFieldAddress.getText();
}

public String getResultGetOld()
{
	if ((this.getReturnValue() & UserIO.RETURN_OK) == 0) return null;
	return comboGetOld.getSelectedItem().toString();
}

public boolean getResultDofilters()
{
	return checkboxDofilters.isSelected();
}


/* CODE_FOR_INTERFACE_7_START */
@Override
protected Integer createReturnValue() { return this.getReturnmask(); }
/* CODE_FOR_INTERFACE_7_END */

}
