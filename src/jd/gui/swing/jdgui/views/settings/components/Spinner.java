package jd.gui.swing.jdgui.views.settings.components;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class Spinner extends JSpinner implements SettingsComponent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public Spinner(int min, int max) {

        super(new SpinnerNumberModel(min, min, max, 1));
        setEditor(new JSpinner.NumberEditor(this, "#"));
        // final DefaultFormatterFactory factory = new
        // DefaultFormatterFactory(new NumberFormatter() {
        //
        // @Override
        // public void setMinimum(Comparable minimum) {
        // ((SpinnerNumberModel) Spinner.this.getModel()).setMinimum(minimum);
        // }
        //
        // @Override
        // public Comparable getMinimum() {
        // return ((SpinnerNumberModel) Spinner.this.getModel()).getMinimum();
        // }
        //
        // @Override
        // public void setMaximum(Comparable max) {
        // ((SpinnerNumberModel) Spinner.this.getModel()).setMaximum(max);
        // }
        //
        // @Override
        // public Comparable getMaximum() {
        // return ((SpinnerNumberModel) Spinner.this.getModel()).getMaximum();
        // }
        //
        // @Override
        // public String valueToString(Object value) throws ParseException {
        // return super.valueToString(value);
        // }
        //
        // @Override
        // public Object stringToValue(String text) throws ParseException {
        // return super.stringToValue(text);
        // }
        //
        // });
        // ((JSpinner.DefaultEditor)
        // getEditor()).getTextField().setFormatterFactory(factory);

    }

    public String getConstraints() {
        return null;
    }

    public boolean isMultiline() {
        return false;
    }

    /**
     * Set the Spinner renderer and editor format.
     * 
     * @see http 
     *      ://download.oracle.com/javase/1.4.2/docs/api/java/text/DecimalFormat
     *      .html
     * @param formatString
     */
    public void setFormat(String formatString) {
        setEditor(new JSpinner.NumberEditor(this, formatString));
    }

}
