package jd.plugins.optional.jdfeedme.dialogs;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import jd.gui.UserIO;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;

/* CODE_FOR_INTERFACE_5_START
public class ComboDialog extends AbstractDialog {
CODE_FOR_INTERFACE_5_END */
/* CODE_FOR_INTERFACE_7_START */
public class ComboDialog extends AbstractDialog <Integer> {
/* CODE_FOR_INTERFACE_7_END */


private static final long serialVersionUID = 4516288388787228122L;
private String message_combo;
private JComboBox inputCombo;
private int defaultAnswer;
private Object[] options;
private ListCellRenderer renderer;

public ComboDialog(int flag, String title, String question_combo, Object[] options, int defaultSelection, ImageIcon icon, String okText, String cancelText, ListCellRenderer renderer) {
  super(flag, title, icon, okText, cancelText);
  message_combo = question_combo;
  this.renderer = renderer;
  this.defaultAnswer = defaultSelection;
  this.options = options;
  
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
  JPanel contentpane = new JPanel(new MigLayout("ins 0,wrap 2", "[fill]"));
  
  contentpane.add(new JLabel(message_combo));
  
  inputCombo = new JComboBox(options);
  if (renderer != null) inputCombo.setRenderer(renderer);
  inputCombo.setSelectedIndex(this.defaultAnswer);

  contentpane.add(inputCombo,"w 300!");
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

public String getResultCombo()
{
	if ((this.getReturnValue() & UserIO.RETURN_OK) == 0) return null;
	return inputCombo.getSelectedItem().toString();
}

/* CODE_FOR_INTERFACE_7_START */
@Override
protected Integer createReturnValue() { return this.getReturnmask(); }
/* CODE_FOR_INTERFACE_7_END */

}
