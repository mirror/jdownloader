package org.jdownloader.actions.event;

import java.beans.PropertyChangeEvent;
import java.util.EventListener;

public interface AppActionListener extends EventListener {

    void onActionPropertyChanged(PropertyChangeEvent parameter);

}