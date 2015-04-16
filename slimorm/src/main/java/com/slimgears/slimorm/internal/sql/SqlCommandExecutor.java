// Copyright 2015 Denis Itskovich
// Refer to LICENSE.txt for license details
package com.slimgears.slimorm.internal.sql;

import com.slimgears.slimorm.interfaces.entities.FieldValueLookup;
import com.slimgears.slimorm.internal.interfaces.CloseableIterator;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by Denis on 08-Apr-15
 * <File Description>
 */
public interface SqlCommandExecutor extends Closeable {
    int count(SqlCommand command) throws IOException;
    <T> CloseableIterator<FieldValueLookup<T>> select(SqlCommand command) throws IOException;
    void execute(SqlCommand command) throws IOException;
}
