package com.zipwhip.cassandra;

import com.google.common.base.Preconditions;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.util.RangeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Ali Serghini
 *         Date: 7/22/13
 *         Time: 3:26 PM
 *         <p/>
 *         K -  Key Type
 *         C -  Column Type
 *         R -  Result
 *         Base implemenation of the cassandra DAO.
 */
public class SimpleCassandraDao<K, C, V> extends AbstractCassandraDao<K, C, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCassandraDao.class);

    public SimpleCassandraDao(final Keyspace keyspace, final ColumnFamily<K, C> columnFamily, final Serializer<C> columnSerializer, final Serializer<V> valueSerializer) {
        super(keyspace, columnFamily, columnSerializer, valueSerializer);
    }

    @Override
    public void put(final K rowKey, final C columnName, final V value) throws ConnectionException {
        put(rowKey, columnName, value, null);
    }

    @Override
    public void put(final K rowKey, final C columnName, final V value, final Integer ttl) throws ConnectionException {
        Preconditions.checkNotNull(rowKey, "Row Key cannot be null");
        Preconditions.checkNotNull(columnName, "Column Name cannot be null");

        getKeyspace().prepareColumnMutation(getColumnFamily(), rowKey, columnName)
                .putValue(value, getValueSerializer(), ttl)
                .execute();
    }

    @Override
    public V get(final K rowKey, final C columnName) throws ConnectionException {
        Preconditions.checkNotNull(rowKey, "Row Key cannot be null");
        Preconditions.checkNotNull(columnName, "Column Name cannot be null");

        final Column<C> result;
        try {
            result = getKeyspace().prepareQuery(getColumnFamily())
                    .getKey(rowKey)
                    .getColumn(columnName)
                    .execute().getResult();
        } catch (NotFoundException e) {
//            LOGGER.warn(String.format("==W No columns found for column family [%s] and row [%s]", getColumnFamily().getName(), rowKey), e);
            return null;
        }

        return CassandraUtil.convertValue(result, getValueSerializer(), null);
    }

    @Override
    public V getMaxColumnValue(final K rowKey) throws ConnectionException {
        Preconditions.checkNotNull(rowKey, "Row Key cannot be null");

        final ColumnList<C> result;
        try {
            result = getKeyspace().prepareQuery(getColumnFamily())
                    .getKey(rowKey)
                    .withColumnRange(new RangeBuilder().setReversed(true).setLimit(1).build())
                    .execute().getResult();
        } catch (NotFoundException e) {
            LOGGER.warn(String.format("==W No columns found for column family [%s] and row [%s]", getColumnFamily().getName(), rowKey), e);
            return null;
        }

        return CassandraUtil.convertFirst(result, getValueSerializer(), null);
    }

    @Override
    public List<V> getRange(final K rowKey, C columnNameStart, C columnNameEnd, final int limit) throws ConnectionException {
        Preconditions.checkNotNull(rowKey, "Row Key cannot be null");
        Preconditions.checkNotNull(columnNameStart, "columnNameStart cannot be null");

        final ColumnList<C> result;
        try {
            result = getKeyspace().prepareQuery(getColumnFamily())
                    .getKey(rowKey)
                    .withColumnRange(new RangeBuilder()
                            .setStart(columnNameStart, getColumnSerializer())
                            .setEnd(columnNameEnd, getColumnSerializer())
                            .setLimit(limit)
                            .build()
                    )
                    .execute().getResult();
        } catch (NotFoundException e) {
            LOGGER.warn(String.format("==W No columns found for column family [%s] and row [%s] after column [%s]", getColumnFamily().getName(), rowKey, columnNameStart), e);
            return null;
        }

        return CassandraUtil.convertList(result, getValueSerializer(), null);
    }
}
