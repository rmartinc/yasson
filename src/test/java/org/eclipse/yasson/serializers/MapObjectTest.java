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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    public static class MapObjectEnumLocal extends MapObject<EnumKey, Locale> {};

    public static class MapObjectLocalString extends MapObject<Locale, String> {};

    /**
     * Test for Integer/String map (MapToObjectSerializer).
     */
    @Test
    public void testIntegerString() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig());

        MapObjectIntegerString mapObject = new MapObjectIntegerString();
        mapObject.getValues().put(12, "twelve");
        mapObject.getValues().put(48, "forty eight");
        mapObject.getValues().put(256, "two hundred fifty-six");

        String json = jsonb.toJson(mapObject);
        MapObjectIntegerString resObject = jsonb.fromJson(json, MapObjectIntegerString.class);
        assertEquals(mapObject, resObject);
    }

    /**
     * Test for Enum/Locale map (MapToObjectSerializer).
     */
    @Test
    public void testEnumLocale() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withSerializers(new LocaleSerializer())
                .withDeserializers(new LocaleDeserializer()));

        MapObjectEnumLocal mapObject = new MapObjectEnumLocal();
        mapObject.getValues().put(EnumKey.ONE, Locale.US);
        mapObject.getValues().put(EnumKey.TWO, Locale.ENGLISH);

        String json = jsonb.toJson(mapObject);
        MapObjectEnumLocal resObject = jsonb.fromJson(json, MapObjectEnumLocal.class);
        assertEquals(mapObject, resObject);
    }

    /**
     * Test for Locale/String map (MapToEntriesArraySerializer).
     */
    @Test
    public void testLocaleString() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withSerializers(new LocaleSerializer())
                .withDeserializers(new LocaleDeserializer()));

        MapObjectLocalString mapObject = new MapObjectLocalString();
        mapObject.getValues().put(Locale.US, "us");
        mapObject.getValues().put(Locale.ENGLISH, "en");
        mapObject.getValues().put(Locale.JAPAN, "jp");

        String json = jsonb.toJson(mapObject);
        MapObjectLocalString resObject = jsonb.fromJson(json, MapObjectLocalString.class);
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

        MapObjectEnumLocal mapObject = new MapObjectEnumLocal();
        mapObject.getValues().put(null, Locale.US);
        mapObject.getValues().put(EnumKey.ONE, Locale.ENGLISH);
        mapObject.getValues().put(EnumKey.TWO, Locale.GERMAN);
        assertEquals(Locale.US, mapObject.getValues().get(null));

        String json = jsonb.toJson(mapObject);
        MapObjectEnumLocal resObject = jsonb.fromJson(json, MapObjectEnumLocal.class);
        assertEquals(Locale.US, resObject.getValues().get(null));
        assertEquals(mapObject, resObject);
    }
}
