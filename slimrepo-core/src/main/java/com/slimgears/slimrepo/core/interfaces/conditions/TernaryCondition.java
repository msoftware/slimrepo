// Copyright 2015 Denis Itskovich
// Refer to LICENSE.txt for license details
package com.slimgears.slimrepo.core.interfaces.conditions;

/**
 * Created by Denis on 11-Apr-15
 * <File Description>
 */
public interface TernaryCondition<TEntity, T> extends FieldCondition<TEntity, T> {
    T getFirst();
    T getSecond();
}
