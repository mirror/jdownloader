/*
 * $Id: JXRadioGroup.java,v 1.8 2006/09/04 13:03:49 kleopatra Exp $
 * 
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle, Santa Clara, California 95054, U.S.A. All rights
 * reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package org.jdesktop.swingx;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * A group of radio buttons that functions as a unit.
 * 
 * Notes
 * 
 * (1) Enabling and disabling the JXRadioGroup will enable/disable all of the
 *   child buttons inside the JXRadioGroup.
 * 
 * (2) If the generic type parameter of JXRadioGroup is a subclass of {@link AbstractButton}, then
 *   the buttons will be added "as is" to the container. 
 *   If the generic type is anything else, buttons will be created as {@link JRadioButton} objects,
 *   and the button text will be set by calling toString() on the value object.
 *   
 * (3) Alternatively, if you want to configure the buttons individually, construct the JXRadioGroup
 *   normally, and then call {@link #getChildButton(int)} or {@link #getChildButton(Object)} 
 *   and configure the buttons.
 *  
 * @author Amy Fowler
 * @author Noel Grandin
 * @version 1.0
 */
public class JXRadioGroup<T> extends JPanel {

    private static final long serialVersionUID = 3257285842266567986L;

    private ButtonGroup buttonGroup;

    private final List<T> values = new ArrayList<T>();

    private ActionSelectionListener actionHandler;

    private List<ActionListener> actionListeners;

    /**
     * Create a default JXRadioGroup with a default layout axis of {@link BoxLayout#X_AXIS}.
     */
    public JXRadioGroup() {
	setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	buttonGroup = new ButtonGroup();
    }

    /**
     * Set the layout axis of the radio group.
     * 
     * @param axis values from {@link BoxLayout}.
     */
    public void setLayoutAxis(int axis)
    {
	setLayout(new BoxLayout(this, axis));
    }

    /**
     * Convenience factory method.
     * Reduces code clutter when dealing with generics.
     * 
     * @param radioValues the list of values used to create the group.
     */
    public static <T> JXRadioGroup<T> create(T[] radioValues)
    {
	return new JXRadioGroup<T>(radioValues);
    }
    
    /**
     * Create a default JXRadioGroup with a default layout axis of {@link BoxLayout#X_AXIS}.
     * 
     * @param radioValues the list of values used to create the group.
     */
    public JXRadioGroup(T[] radioValues) {
	this();
	for (int i = 0; i < radioValues.length; i++) {
	    add(radioValues[i]);
	}
    }

    /**
     */
    public void setValues(T[] radioValues) {
	clearAll();
	for (int i = 0; i < radioValues.length; i++) {
	    add(radioValues[i]);
	}
    }

    private void clearAll() {
	values.clear();
	buttonGroup = new ButtonGroup();
	// remove all the child components
	removeAll();
    }

    /**
     * You can use this method to manually add your own AbstractButton objects, provided you declared
     * the class as <code>JXRadioGroup&lt;JRadioButton&gt;</code>.
     */
    public void add(T radioValue) {
	if (values.contains(radioValue))
	{
	    throw new IllegalArgumentException("cannot add the same value twice " + radioValue);
	}
	if (radioValue instanceof AbstractButton) {
	    values.add(radioValue);
	    addButton((AbstractButton) radioValue);
	} else {
	    values.add(radioValue);
	    // Note: the "quote + object" trick here allows null values
	    addButton(new JRadioButton(""+radioValue));
	}
    }

    private void addButton(AbstractButton button) {
	buttonGroup.add(button);
	super.add(button);
	if (actionHandler == null) {
	    actionHandler = new ActionSelectionListener();
	}
	button.addActionListener(actionHandler);
	button.addItemListener(actionHandler);
    }

    private class ActionSelectionListener implements ActionListener, ItemListener
    {
	public void actionPerformed(ActionEvent e) {
	    fireActionEvent(e);
	}

	public void itemStateChanged(ItemEvent e) {
	    fireActionEvent(null);
	}
    }

    public AbstractButton getSelectedButton() {
	final ButtonModel selectedModel = buttonGroup.getSelection();
	final AbstractButton children[] = getButtonComponents();
	for (int i = 0; i < children.length; i++) {
	    AbstractButton button = children[i];
	    if (button.getModel() == selectedModel) {
		return button;
	    }
	}
	return null;
    }

    private AbstractButton[] getButtonComponents() {
	final Component[] children = getComponents();
	final List<AbstractButton> buttons = new ArrayList<AbstractButton>();
	for (int i = 0; i < children.length; i++) {
	    if (children[i] instanceof AbstractButton) {
		buttons.add((AbstractButton) children[i]);
	    }
	}
	return buttons.toArray(new AbstractButton[buttons.size()]);
    }

    private int getSelectedIndex() {
	final ButtonModel selectedModel = buttonGroup.getSelection();
	final Component children[] = getButtonComponents();
	for (int i = 0; i < children.length; i++) {
	    AbstractButton button = (AbstractButton) children[i];
	    if (button.getModel() == selectedModel) {
		return i;
	    }
	}
	return -1;
    }

    public T getSelectedValue() {
	final int index = getSelectedIndex();
	return (index < 0 || index >= values.size()) ? null : values.get(index);
    }

    public void setSelectedValue(T value) {
	final int index = values.indexOf(value);
	AbstractButton button = getButtonComponents()[index];
	button.setSelected(true);
    }
    
    /**
     * Retrieve the child button by index.
     */
    public AbstractButton getChildButton(int index) {
	return getButtonComponents()[index];
    }

    /**
     * Retrieve the child button that represents this value.
     */
    public AbstractButton getChildButton(T value) {
	final int index = values.indexOf(value);
	return getButtonComponents()[index];
    }

    /**
     * Get the number of child buttons.
     */
    public int getChildButtonCount() {
	return getButtonComponents().length;
    }
    
    public void addActionListener(ActionListener l) {
	if (actionListeners == null) {
	    actionListeners = new ArrayList<ActionListener>();
	}
	actionListeners.add(l);
    }

    public void removeActionListener(ActionListener l) {
	if (actionListeners != null) {
	    actionListeners.remove(l);
	}
    }

    public ActionListener[] getActionListeners() {
	if (actionListeners != null) {
	    return actionListeners.toArray(new ActionListener[0]);
	}
	return new ActionListener[0];
    }

    protected void fireActionEvent(ActionEvent e) {
	if (actionListeners != null) {
	    for (int i = 0; i < actionListeners.size(); i++) {
		ActionListener l = actionListeners.get(i);
		l.actionPerformed(e);
	    }
	}
    }

    /**
     * Enable/disable all of the child buttons
     * 
     * @see JComponent#setEnabled(boolean)
     */
    @Override
    public void setEnabled(boolean enabled) {
	super.setEnabled(enabled);
	for (Enumeration en = buttonGroup.getElements(); en.hasMoreElements();) {
	    final AbstractButton button = (AbstractButton) en.nextElement();
	    /* We don't want to enable a button where the action does not
             * permit it. */
	    if (enabled && button.getAction() != null
		    && !button.getAction().isEnabled()) {
		// do nothing
	    } else {
		button.setEnabled(enabled);
	    }
	}
    }

}