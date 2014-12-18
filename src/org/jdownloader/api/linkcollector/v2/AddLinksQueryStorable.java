package org.jdownloader.api.linkcollector.v2;

import org.appwork.storage.Storable;
import org.appwork.storage.jackson.JacksonMapper;
import org.jdownloader.myjdownloader.client.bindings.AddLinksQuery;

public class AddLinksQueryStorable extends AddLinksQuery implements Storable {
    public AddLinksQueryStorable(/* Storable */) {

    }

    public static void main(String[] args) {
        System.out.println(new JacksonMapper().objectToString(new AddLinksQueryStorable()));
    }

}