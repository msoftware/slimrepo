// Copyright 2015 Denis Itskovich
// Refer to LICENSE.txt for license details
package com.slimgears.slimrepo.core.internal.sql;

import com.slimgears.slimrepo.core.interfaces.entities.Entity;
import com.slimgears.slimrepo.core.interfaces.entities.EntityType;
import com.slimgears.slimrepo.core.internal.AbstractSessionServiceProvider;
import com.slimgears.slimrepo.core.internal.interfaces.RepositoryCreator;
import com.slimgears.slimrepo.core.internal.interfaces.SessionEntityServiceProvider;
import com.slimgears.slimrepo.core.internal.interfaces.TransactionProvider;
import com.slimgears.slimrepo.core.internal.sql.interfaces.SqlCommandExecutor;
import com.slimgears.slimrepo.core.internal.sql.interfaces.SqlOrmServiceProvider;
import com.slimgears.slimrepo.core.internal.sql.interfaces.SqlSessionServiceProvider;

import java.io.IOException;

/**
 * Created by Denis on 14-Apr-15
 * <File Description>
 */
public abstract class AbstractSqlSessionServiceProvider extends AbstractSessionServiceProvider implements SqlSessionServiceProvider {
    private SqlCommandExecutor sqlExecutor;
    private TransactionProvider transactionProvider;
    private SqlOrmServiceProvider ormServiceProvider;

    public AbstractSqlSessionServiceProvider(SqlOrmServiceProvider serviceProvider) {
        this.ormServiceProvider = serviceProvider;
    }

    @Override
    public void close() throws IOException {
        sqlExecutor = null;
        transactionProvider = null;
    }

    protected abstract SqlCommandExecutor createCommandExecutor();
    protected abstract TransactionProvider createTransactionProvider();

    @Override
    protected <TKey, TEntity extends Entity<TKey>> SessionEntityServiceProvider<TKey, TEntity> createEntityServiceProvider(EntityType<TKey, TEntity> entityType) {
        return new SqlSessionEntityServiceProvider<>(this, entityType);
    }

    @Override
    public SqlCommandExecutor getExecutor() {
        return sqlExecutor != null
                ? sqlExecutor
                : (sqlExecutor = createCommandExecutor());
    }

    @Override
    public TransactionProvider getTransactionProvider() {
        return transactionProvider != null
                ? transactionProvider
                : (transactionProvider = createTransactionProvider());
    }

    @Override
    public RepositoryCreator createRepositoryCreator() {
        return new SqlRepositoryCreator(this);
    }

    @Override
    public SqlOrmServiceProvider getOrmServiceProvider() {
        return ormServiceProvider;
    }
}
