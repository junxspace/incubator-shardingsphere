/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.shardingjdbc.executor;

import io.shardingsphere.core.constant.ConnectionMode;
import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.core.constant.SQLType;
import io.shardingsphere.core.constant.transaction.TransactionType;
import io.shardingsphere.core.executor.StatementExecuteUnit;
import io.shardingsphere.core.executor.sql.execute.SQLExecuteCallback;
import io.shardingsphere.core.executor.sql.execute.SagaSQLExecuteCallback;
import io.shardingsphere.core.routing.RouteUnit;
import io.shardingsphere.core.routing.SQLUnit;
import io.shardingsphere.core.transaction.TransactionTypeHolder;
import io.shardingsphere.spi.transaction.ShardingTransactionHandlerRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SQLExecuteCallbackFactoryTest {
    private static final TransactionType ORIGIN_TRANSACTION_TYPE = TransactionType.LOCAL;
    
    @Mock
    private PreparedStatement preparedStatement;
    
    @Mock
    private StatementExecutor.Updater updater;
    
    @Mock
    private StatementExecutor.Executor executor;
    
    @Mock
    private Connection connection;
    
    @Mock
    private DatabaseMetaData metaData;
    
    private StatementExecuteUnit unit;
    
    @BeforeClass
    public static void preLoad() {
        ShardingTransactionHandlerRegistry.load();
    }
    
    @Before
    public void setUp() throws SQLException {
        when(preparedStatement.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn("jdbc:mysql://localhost:3306/test");
        unit = new StatementExecuteUnit(new RouteUnit("ds", new SQLUnit("SELECT now()", Collections.<List<Object>>emptyList())), preparedStatement, ConnectionMode.CONNECTION_STRICTLY);
    }
    
    @After
    public void tearDown() {
        TransactionTypeHolder.set(ORIGIN_TRANSACTION_TYPE);
    }
    
    @Test
    public void assertGetPreparedUpdateSQLExecuteCallback() throws SQLException {
        SQLExecuteCallback sqlExecuteCallback = SQLExecuteCallbackFactory.getPreparedUpdateSQLExecuteCallback(DatabaseType.MySQL, SQLType.DML, true);
        sqlExecuteCallback.execute(unit, true, null);
        verify(preparedStatement).executeUpdate();
        TransactionTypeHolder.set(TransactionType.BASE);
        sqlExecuteCallback = SQLExecuteCallbackFactory.getPreparedUpdateSQLExecuteCallback(DatabaseType.MySQL, SQLType.DML, true);
        assertThat(sqlExecuteCallback instanceof SagaSQLExecuteCallback, is(true));
        sqlExecuteCallback.execute(unit, true, null);
        int result = (int) sqlExecuteCallback.execute(unit, true, null);
        assertThat(result, is(0));
    }
    
    @Test
    public void assertGetPreparedSQLExecuteCallback() throws SQLException {
        SQLExecuteCallback sqlExecuteCallback = SQLExecuteCallbackFactory.getPreparedSQLExecuteCallback(DatabaseType.MySQL, SQLType.DML, true);
        sqlExecuteCallback.execute(unit, true, null);
        verify(preparedStatement).execute();
        TransactionTypeHolder.set(TransactionType.BASE);
        sqlExecuteCallback = SQLExecuteCallbackFactory.getPreparedSQLExecuteCallback(DatabaseType.MySQL, SQLType.DML, true);
        assertThat(sqlExecuteCallback instanceof SagaSQLExecuteCallback, is(true));
        sqlExecuteCallback.execute(unit, true, null);
        boolean result = (boolean) sqlExecuteCallback.execute(unit, true, null);
        assertFalse(result);
    }
    
    @Test
    public void assertGetBatchPreparedSQLExecuteCallback() throws SQLException {
        SQLExecuteCallback sqlExecuteCallback = SQLExecuteCallbackFactory.getBatchPreparedSQLExecuteCallback(DatabaseType.MySQL, SQLType.DML, true);
        sqlExecuteCallback.execute(unit, true, null);
        verify(preparedStatement).executeBatch();
        TransactionTypeHolder.set(TransactionType.BASE);
        sqlExecuteCallback = SQLExecuteCallbackFactory.getBatchPreparedSQLExecuteCallback(DatabaseType.MySQL, SQLType.DML, true);
        assertThat(sqlExecuteCallback instanceof SagaSQLExecuteCallback, is(true));
        sqlExecuteCallback.execute(unit, true, null);
    }
    
    @Test
    public void assertGetSQLExecuteCallbackWithUpdater() throws SQLException {
        SQLExecuteCallback sqlExecuteCallback = SQLExecuteCallbackFactory.getSQLExecuteCallback(DatabaseType.MySQL, SQLType.DML, true, updater);
        sqlExecuteCallback.execute(unit, true, null);
        verify(updater).executeUpdate(unit.getStatement(), unit.getRouteUnit().getSqlUnit().getSql());
        TransactionTypeHolder.set(TransactionType.BASE);
        sqlExecuteCallback = SQLExecuteCallbackFactory.getSQLExecuteCallback(DatabaseType.MySQL, SQLType.DML, true, updater);
        assertThat(sqlExecuteCallback instanceof SagaSQLExecuteCallback, is(true));
        sqlExecuteCallback.execute(unit, true, null);
        int result = (int) sqlExecuteCallback.execute(unit, true, null);
        assertThat(result, is(0));
    }
    
    @Test
    public void assertGetSQLExecuteCallbackWithExecutor() throws SQLException {
        SQLExecuteCallback sqlExecuteCallback = SQLExecuteCallbackFactory.getSQLExecuteCallback(DatabaseType.MySQL, SQLType.DML, true, executor);
        sqlExecuteCallback.execute(unit, true, null);
        verify(executor).execute(unit.getStatement(), unit.getRouteUnit().getSqlUnit().getSql());
        TransactionTypeHolder.set(TransactionType.BASE);
        sqlExecuteCallback = SQLExecuteCallbackFactory.getSQLExecuteCallback(DatabaseType.MySQL, SQLType.DML, true, executor);
        assertTrue(sqlExecuteCallback instanceof SagaSQLExecuteCallback);
        boolean result = (boolean) sqlExecuteCallback.execute(unit, true, null);
        assertFalse(result);
    }
}
