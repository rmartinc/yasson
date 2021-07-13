/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

package org.eclipse.yasson.serializers;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.eclipse.yasson.YassonConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Generic tests for maps of different types.
 *
 * @author rmartinc
 */
public class MapObjectTest {

    public static class LocaleSerializer implements JsonbSerializer<Locale> {

        @Override
        public void serialize(Locale obj, JsonGenerator generator, SerializationContext ctx) {
            generator.write(obj.toLanguageTag());
        }
    }

    public static class LocaleDeserializer implements JsonbDeserializer<Locale> {

        @Override
        public Locale deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            return Locale.forLanguageTag(parser.getString());
        }
    }

    public static class MapObject<K, V> {

        private Map<K, V> values;

        public MapObject() {
            this.values = new HashMap<>();
        }

        public Map<K, V> getValues() {
            return values;
        }

        public void setValues(Map<K, V> values) {
            this.values = values;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MapObject) {
                MapObject<?,?> to = (MapObject<?,?>) o;
                return values.equals(to.values);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.values);
        }

        @Override
        public String toString() {
            return values.toString();
        }
    }

    public static enum EnumKey {ZERO, ONE, TWO};

    public static class MapObjectIntegerString extends MapObject<Integer, String> {};

    public static class MapObjectBigIntegerString extends MapObject<BigInteger, String> {};

    public static class MapObjectEnumString extends MapObject<EnumKey, String> {};

    public static class MapObjectStringString extends MapObject<String, String> {};

    public static class MapObjectLocaleString extends MapObject<Locale, String> {};

    public static class MapObjectBooleanString extends MapObject<Boolean, String> {};

    /**
     * Test for Integer/String map (MapToObjectSerializer).
     */
    @Test
    public void testBigIntegerString() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig());

        MapObjectBigIntegerString mapObject = new MapObjectBigIntegerString();
        mapObject.getValues().put(new BigInteger("12"), "twelve");
        mapObject.getValues().put(new BigInteger("48"), "forty eight");
        mapObject.getValues().put(new BigInteger("256"), "two hundred fifty-six");

        String json = jsonb.toJson(mapObject);
        System.err.println(json);
        MapObjectBigIntegerString resObject = jsonb.fromJson(json, MapObjectBigIntegerString.class);
        assertEquals(mapObject, resObject);
    }

    @Test
    public void testNotParametrizedMap() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig());

        Map<Integer,String> mapObject = new HashMap<>();
        mapObject.put(12, "twelve");
        mapObject.put(48, "forty eight");
        mapObject.put(256, "two hundred fifty-six");

        String json = jsonb.toJson(mapObject);
        System.err.println(json);
        Map resObject = jsonb.fromJson(json, Map.class);
        assertEquals(3, resObject.size());
        assertTrue(resObject.keySet().iterator().next() instanceof String);
    }

    @Test
    public void testIntegerString() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig());

        MapObjectIntegerString mapObject = new MapObjectIntegerString();
        mapObject.getValues().put(12, "twelve");
        mapObject.getValues().put(48, "forty eight");
        mapObject.getValues().put(256, "two hundred fifty-six");

        String json = jsonb.toJson(mapObject);
        System.err.println(json);
        MapObjectIntegerString resObject = jsonb.fromJson(json, MapObjectIntegerString.class);
        assertEquals(mapObject, resObject);
    }

    @Test
    public void testLocaleStringMapToObjectSerializer() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withSerializers(new LocaleSerializer())
                .withDeserializers(new LocaleDeserializer()));

        String json = "{\"values\":{\"en-US\":\"us\",\"en-GB\":\"uk\"}}";
        MapObjectLocaleString resObject = jsonb.fromJson(json, MapObjectLocaleString.class);
        assertEquals(2, resObject.getValues().size());
        assertEquals("us", resObject.getValues().get(Locale.US));
        assertEquals("uk", resObject.getValues().get(Locale.UK));
    }

    @Test
    public void testBooleanStringMapToObjectSerializer() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withSerializers(new LocaleSerializer())
                .withDeserializers(new LocaleDeserializer()));

        String json = "{\"values\":{\"true\":\"TRUE\",\"false\":\"FALSE\",\"null\":\"NULL\"}}";
        MapObjectBooleanString resObject = jsonb.fromJson(json, MapObjectBooleanString.class);
        assertEquals(3, resObject.getValues().size());
        assertEquals("TRUE", resObject.getValues().get(true));
        assertEquals("FALSE", resObject.getValues().get(false));
        assertEquals("NULL", resObject.getValues().get(null));
    }

    /**
     * Test for Enum/Locale map (MapToObjectSerializer).
     */
    @Test
    public void testEnumString() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withSerializers(new LocaleSerializer())
                .withDeserializers(new LocaleDeserializer()));

        MapObjectEnumString mapObject = new MapObjectEnumString();
        mapObject.getValues().put(EnumKey.ONE, "one");
        mapObject.getValues().put(EnumKey.TWO, "two");

        String json = jsonb.toJson(mapObject);
        System.err.println(json);
        MapObjectEnumString resObject = jsonb.fromJson(json, MapObjectEnumString.class);
        assertEquals(mapObject, resObject);
    }

    @Test
    public void testStringString() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().setProperty("lala", "lala"));

        MapObjectStringString mapObject = new MapObjectStringString();
        mapObject.getValues().put("one", "one");
        mapObject.getValues().put("two", "two");

        String json = jsonb.toJson(mapObject);
        System.err.println(json);
        MapObjectStringString resObject = jsonb.fromJson(json, MapObjectStringString.class);
        assertEquals(mapObject, resObject);
    }

    @Test
    public void testLocaleString() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withSerializers(new LocaleSerializer())
                .withDeserializers(new LocaleDeserializer()));

        MapObjectLocaleString mapObject = new MapObjectLocaleString();
        mapObject.getValues().put(Locale.US, "us");
        mapObject.getValues().put(Locale.UK, "uk");

        String json = jsonb.toJson(mapObject);
        System.err.println(json);
        MapObjectLocaleString resObject = jsonb.fromJson(json, MapObjectLocaleString.class);
        assertEquals(mapObject, resObject);
    }

    /**
     * Test null for Integer/String map (MapToObjectSerializer).
     */
    @Test
    public void testNullValueForInteger() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig());

        MapObjectIntegerString mapObject = new MapObjectIntegerString();
        mapObject.getValues().put(null, "null Value");
        mapObject.getValues().put(48, "forty eight");
        mapObject.getValues().put(256, "two hundred fifty-six");
        assertEquals("null Value", mapObject.getValues().get(null));

        String json = jsonb.toJson(mapObject);
        MapObjectIntegerString resObject = jsonb.fromJson(json, MapObjectIntegerString.class);
        assertEquals("null Value", resObject.getValues().get(null));
        assertEquals(mapObject, resObject);
    }

    /**
     * Test null for Enum/Locale map (MapToObjectSerializer).
     */
    @Test
    public void testNullValueForEnum() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withSerializers(new LocaleSerializer())
                .withDeserializers(new LocaleDeserializer()));

        MapObjectEnumString mapObject = new MapObjectEnumString();
        mapObject.getValues().put(null, "null");
        mapObject.getValues().put(EnumKey.ONE, "one");
        mapObject.getValues().put(EnumKey.TWO, "two");
        assertEquals("null", mapObject.getValues().get(null));

        String json = jsonb.toJson(mapObject);
        MapObjectEnumString resObject = jsonb.fromJson(json, MapObjectEnumString.class);
        assertEquals("null", resObject.getValues().get(null));
        assertEquals(mapObject, resObject);
    }

    @Test
    public void testNullValueForStringDefault() {
        Jsonb jsonb = JsonbBuilder.create();

        MapObjectStringString mapObject = new MapObjectStringString();
        mapObject.getValues().put(null, "null");
        mapObject.getValues().put("one", "one");
        mapObject.getValues().put("two", "two");
        assertEquals("null", mapObject.getValues().get(null));

        String json = jsonb.toJson(mapObject);
        MapObjectStringString resObject = jsonb.fromJson(json, MapObjectStringString.class);
        assertEquals("null", resObject.getValues().get(null));
        assertEquals(mapObject, resObject);
    }

    @Test
    public void testNullValueForStringCustomValue() {
        Jsonb jsonb = JsonbBuilder.create(new YassonConfig().withMapToObjectSerializerNullKey("yasson.null"));

        MapObjectStringString mapObject = new MapObjectStringString();
        mapObject.getValues().put(null, "null");
        mapObject.getValues().put("null", "null-string");
        mapObject.getValues().put("one", "one");
        mapObject.getValues().put("two", "two");
        assertEquals("null", mapObject.getValues().get(null));
        assertEquals("null-string", mapObject.getValues().get("null"));

        String json = jsonb.toJson(mapObject);
        MapObjectStringString resObject = jsonb.fromJson(json, MapObjectStringString.class);
        assertEquals("null", resObject.getValues().get(null));
        assertEquals("null-string", mapObject.getValues().get("null"));
        assertEquals(mapObject, resObject);
    }
}
