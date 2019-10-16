/*
 * Copyright (c) 2016, 2019 Oracle and/or its affiliates. All rights reserved.
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

import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;

/**
 * Serializes byte array as JSON array of ints.
 */
public class ByteArraySerializer extends AbstractArraySerializer<byte[]> {

    /**
     * Creates new instance of byte array serializer.
     *
     * @param builder serializer builder
     */
    protected ByteArraySerializer(SerializerBuilder builder) {
        super(builder);
    }

    @Override
    protected void serializeInternal(byte[] obj, JsonGenerator generator, SerializationContext ctx) {
        for (byte b : obj) {
            generator.write(b);
        }
    }

}
