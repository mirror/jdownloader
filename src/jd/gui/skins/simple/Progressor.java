package jd.gui.skins.simple;

import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JFrame;

public abstract class Progressor extends JDialog {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public abstract int getMaximum();

    public abstract String getMessage() ;

    public abstract int getMinimum();

    public abstract String getString();

    public abstract int getValue();

    public abstract void setMaximum(int value);

    public abstract void setMessage(String txt);

    public abstract void setMinimum(int value);

    public abstract void setString(String txt);

    public abstract void setStringPainted(boolean v);

    public abstract void setThread(Thread th);

    public abstract void setValue(int value);
    public Progressor() {
	}
    public Progressor(Frame owner) {
		super(owner);
	}
}
