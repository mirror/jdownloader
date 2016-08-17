package org.jdownloader.api.dialog;

import java.util.HashMap;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("dialogs")
public interface DialogApiInterface extends RemoteAPIInterface {
    @APIParameterNames({ "id", "icon", "properties" })
    public DialogInfo get(long id, boolean icon, boolean properties) throws InvalidIdException;

    @AllowNonStorableObjects
    @APIParameterNames({ "id", "data" })
    public void answer(long id, HashMap<String, Object> data) throws BadOrderException, InvalidIdException;

    public long[] list();

    @APIParameterNames({ "dialogType" })
    public DialogTypeInfo getTypeInfo(String dialogType) throws TypeNotFoundException;
}
