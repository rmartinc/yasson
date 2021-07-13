/*
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

package org.eclipse.yasson.internal.serializer;

import java.io.StringReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonParser;

import org.eclipse.yasson.internal.JsonbParser;
import org.eclipse.yasson.internal.JsonbRiParser;
import org.eclipse.yasson.internal.ReflectionUtils;
import org.eclipse.yasson.internal.Unmarshaller;
import org.eclipse.yasson.internal.properties.MessageKeys;
import org.eclipse.yasson.internal.properties.Messages;

/**
 * Item implementation for {@link java.util.Map} fields.
 * According to JSON specification object can have only string keys.
 * Nevertheless the implementation lets the key be a basic object that was
 * serialized into a string representation. The key is parsed using a different
 * deserializer that tries to convert the String key into the parametrized type.
 * If not possible implementation is bound to String type.
 *
 * @param <T> map type
 */
public class MapDeserializer<T extends Map<?, ?>> extends AbstractContainerDeserializer<T> implements EmbeddedItem {

    private static final Logger LOGGER = Logger.getLogger(MapDeserializer.class.getName());

    /**
     * Type of the key in the map.
     */
    private final Type mapKeyRuntimeType;

    /**
     * Type of value in the map.
     */
    private final Type mapValueRuntimeType;

    /**
     * The jsonProvider used to create the parsers for keys
     */
    private final JsonProvider jsonProvider;

    /**
     * The unmarshaller used to deserialize keys
     */
    private final Unmarshaller keyUnmarshaller;

    /**
     * Key used for null in the MapToObjectSerializer.
     */
    private final String mapToObjectSerializerNullKey;

    private final T instance;

    /**
     * Create instance of current item with its builder.
     *
     * @param builder {@link DeserializerBuilder} used to build this instance
     */
    protected MapDeserializer(DeserializerBuilder builder) {
        super(builder);
        mapKeyRuntimeType = getRuntimeType() instanceof ParameterizedType
                ? ReflectionUtils.resolveType(this, ((ParameterizedType) getRuntimeType()).getActualTypeArguments()[0])
                : Object.class;
        mapValueRuntimeType = getRuntimeType() instanceof ParameterizedType
                ? ReflectionUtils.resolveType(this, ((ParameterizedType) getRuntimeType()).getActualTypeArguments()[1])
                : Object.class;

        mapToObjectSerializerNullKey = builder.getJsonbContext().getConfigProperties().getMapToObjectSerializerNullKey();
        jsonProvider = builder.getJsonbContext().getJsonProvider();
        keyUnmarshaller = new Unmarshaller(builder.getJsonbContext());
        this.instance = createInstance(builder);
    }

    @SuppressWarnings("unchecked")
    private T createInstance(DeserializerBuilder builder) {
        Class<?> rawType = ReflectionUtils.getRawType(getRuntimeType());
        return rawType.isInterface()
                ? (T) getMapImpl(rawType, builder)
                : (T) builder.getJsonbContext().getInstanceCreator().createInstance(rawType);
    }

    private Map getMapImpl(Class ifcType, DeserializerBuilder builder) {
        if (ConcurrentMap.class.isAssignableFrom(ifcType)) {
            if (SortedMap.class.isAssignableFrom(ifcType) || NavigableMap.class.isAssignableFrom(ifcType)) {
                return new ConcurrentSkipListMap<>();
            } else {
                return new ConcurrentHashMap<>();
            }
        }
        // SortedMap, NavigableMap
        if (SortedMap.class.isAssignableFrom(ifcType)) {
            Class<?> defaultMapImplType = builder.getJsonbContext().getConfigProperties().getDefaultMapImplType();
            return SortedMap.class.isAssignableFrom(defaultMapImplType)
                    ? (Map) builder.getJsonbContext().getInstanceCreator().createInstance(defaultMapImplType)
                    : new TreeMap<>();
        }
        return new HashMap<>();
    }

    @Override
    public T getInstance(Unmarshaller unmarshaller) {
        return instance;
    }

    @Override
    public void appendResult(Object result) {
        appendCaptor(getParserContext().getLastKeyName(), convertNullToOptionalEmpty(mapValueRuntimeType, result));
    }

    @SuppressWarnings("unchecked")
    private <V> void appendCaptor(String key, V value) {
        Object keyObject = key;
        if (mapToObjectSerializerNullKey.equals(key)) {
            keyObject = null;
        } else {
            try {
                // try to deserialize the key into the type
                keyObject = keyUnmarshaller.deserialize(mapKeyRuntimeType, new JsonbRiParser(jsonProvider.createParser(new StringReader("\"" + key + "\""))));
            } catch (Exception e) {
                // because of compatibility just warn and continue using the String key as before
                LOGGER.log(Level.WARNING, Messages.getMessage(MessageKeys.DESERIALIZE_VALUE_ERROR, mapKeyRuntimeType), e);
            }
        }
        ((Map<Object, V>) getInstance(null)).put(keyObject, value);
    }

    @Override
    protected void deserializeNext(JsonParser parser, Unmarshaller context) {
        final JsonbDeserializer<?> deserializer = newCollectionOrMapItem(mapValueRuntimeType, context.getJsonbContext());
        appendResult(deserializer.deserialize(parser, context, mapValueRuntimeType));
    }

    @Override
    protected JsonbRiParser.LevelContext moveToFirst(JsonbParser parser) {
        parser.moveTo(JsonParser.Event.START_OBJECT);
        return parser.getCurrentLevel();
    }
}
