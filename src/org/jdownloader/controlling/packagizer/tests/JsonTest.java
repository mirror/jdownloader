package org.jdownloader.controlling.packagizer.tests;

import java.lang.reflect.Type;

import org.appwork.storage.JSONMapper;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.JsonSerializer;
import org.appwork.storage.SimpleMapper;
import org.appwork.storage.TypeRef;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.testframework.AWTest;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.myjdownloader.client.json.JSonHandler;
import org.jdownloader.myjdownloader.client.json.JsonFactoryInterface;
import org.jdownloader.myjdownloader.client.json.MyJDJsonMapper;

public class JsonTest extends AWTest {
    public static void main(String[] args) {
        run();
    }

    @Override
    public void runTest() throws Exception {
        testMapper(new SimpleMapper());
        testMapper(new JacksonMapper());
    }

    private void testMapper(JSONMapper mapper) throws Exception {
        JSonStorage.setMapper(mapper);
        mapper.addSerializer(JsonFactoryInterface.class, new JsonSerializer() {
            @Override
            public String toJSonString(Object object, Object mapper) {
                return ((JsonFactoryInterface) object).toJsonString();
            }
        });
        MyJDJsonMapper.HANDLER = new JSonHandler<Type>() {
            @Override
            public String objectToJSon(Object payload) {
                return JSonStorage.serializeToJson(payload);
            }

            @Override
            public <T> T jsonToObject(String dec, final Type clazz) {
                return JSonStorage.restoreFromString(dec, new TypeRef<T>(clazz) {
                });
            }
        };
        PackagizerRule rule = new PackagizerRule();
        final PackagizerRule clone = JSonStorage.restoreFromString(JSonStorage.serializeToJson(rule), new TypeRef<PackagizerRule>() {
        });
        assertEqualsDeep(rule, clone);
    }
}
