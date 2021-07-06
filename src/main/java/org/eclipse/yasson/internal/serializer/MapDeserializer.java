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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

import org.eclipse.yasson.internal.JsonbParser;
import org.eclipse.yasson.internal.JsonbRiParser;
import org.eclipse.yasson.internal.ReflectionUtils;
import org.eclipse.yasson.internal.Unmarshaller;
import org.eclipse.yasson.internal.model.ClassModel;
import org.eclipse.yasson.internal.properties.MessageKeys;
import org.eclipse.yasson.internal.properties.Messages;

/**
 * Item implementation for {@link java.util.Map} fields.
 * According to JSON specification object can have only string keys, given that maps could only be parsed
 * from JSON objects, implementation is bound to String type.
 *
 * @param <T> map type
 */
public class MapDeserializer<T extends Map<?, ?>> extends AbstractContainerDeserializer<T> implements EmbeddedItem {

    private static final Logger LOGGER = Logger.getLogger(MapDeserializer.class.getName());

    /**
     * Type of the key in the map. MapSerializer allows String, Number or Enum keys.
     */
    private final Type mapKeyRuntimeType;

    /**
     * Deserializer if the key type is number or enum.
     */
    private final AbstractValueTypeDeserializer<?> keyDeserializer;

    /**
     * Type of value in the map.
     */
    private final Type mapValueRuntimeType;

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
        this.keyDeserializer = createKeyDeserializer(builder, mapKeyRuntimeType);
        this.instance = createInstance(builder);
    }

    @SuppressWarnings("unchecked")
    private T createInstance(DeserializerBuilder builder) {
        Class<?> rawType = ReflectionUtils.getRawType(getRuntimeType());
        return rawType.isInterface()
                ? (T) getMapImpl(rawType, builder)
                : (T) builder.getJsonbContext().getInstanceCreator().createInstance(rawType);
    }

    private static AbstractValueTypeDeserializer<?> createKeyDeserializer(DeserializerBuilder builder, Type mapKeyRuntimeType) {
        if (mapKeyRuntimeType instanceof Class &&
                (Number.class.isAssignableFrom((Class<?>) mapKeyRuntimeType) || ((Class<?>) mapKeyRuntimeType).isEnum())) {
            // if the key is a Number or a Enum we need to use a deserializer for it
            Class<?> keyClazz = (Class<?>) mapKeyRuntimeType;
            final Optional<SerializerProviderWrapper> numberSerializerProvider = DefaultSerializers.findValueSerializerProvider(keyClazz);
            if (numberSerializerProvider.isPresent()) {
                ClassModel classModel = builder.getJsonbContext().getMappingContext().getOrCreateClassModel(keyClazz);
                return numberSerializerProvider.get().getDeserializerProvider()
                        .provideDeserializer(classModel.getClassCustomization());
            }
        }
        return null;
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
        // in this MapDeserializer the appendResult without context should not be called
        throw new JsonbException("Invalid call to appendResult without context in MapDeserializer");
    }

    @Override
    public void appendResult(Object result, Unmarshaller context) {
        appendCaptor(getParserContext().getLastKeyName(), convertNullToOptionalEmpty(mapValueRuntimeType, result), context);
    }

    @SuppressWarnings("unchecked")
    private <V> void appendCaptor(String key, V value, Unmarshaller context) {
        Object keyObject = key;
        if (keyDeserializer != null) {
            try {
                keyObject = keyDeserializer.deserialize(key, context, mapKeyRuntimeType);
            } catch (Exception e) {
                if ("null".equals(key)) {
                    // null key (using strings this is indistinguishable)
                    keyObject = null;
                } else {
                    // because of compatibility just warn and continue using the String key as before
                    LOGGER.log(Level.WARNING, Messages.getMessage(MessageKeys.DESERIALIZE_VALUE_ERROR, mapKeyRuntimeType), e);
                }
            }
        }
        ((Map<Object, V>) getInstance(null)).put(keyObject, value);
    }

    @Override
    protected void deserializeNext(JsonParser parser, Unmarshaller context) {
        final JsonbDeserializer<?> deserializer = newCollectionOrMapItem(mapValueRuntimeType, context.getJsonbContext());
        appendResult(deserializer.deserialize(parser, context, mapValueRuntimeType), context);
    }

    @Override
    protected JsonbRiParser.LevelContext moveToFirst(JsonbParser parser) {
        parser.moveTo(JsonParser.Event.START_OBJECT);
        return parser.getCurrentLevel();
    }
}
