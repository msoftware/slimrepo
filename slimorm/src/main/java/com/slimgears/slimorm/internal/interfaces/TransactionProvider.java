// Copyright 2015 Denis Itskovich
// Refer to LICENSE.txt for license details
package com.slimgears.slimorm.internal.interfaces;

import java.io.IOException;

/**
 * Created by Denis on 14-Apr-15
 * <File Description>
 */
public interface TransactionProvider {
    void beginTransaction() throws IOException;
    void commitTransaction() throws IOException;
    void cancelTransaction() throws IOException;
}
