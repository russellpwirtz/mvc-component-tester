package com.zipwhip.cassandra;

import com.google.common.base.Preconditions;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.ColumnFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ali Serghini
 *         Date: 8/20/13
 *         Time: 2:42 PM
 *         <p/>
 *         K -  Key Type
 *         C -  Column Type
 *         V -  Value Type
 */
public abstract class AbstractCassandraDao<K, C, V> implements ICassandraDao<K, C, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCassandraDao.class);

    private final Keyspace keyspace;
    private final ColumnFamily<K, C> columnFamily;
    private final Serializer<C> columnSerializer;
    private final Serializer<V> valueSerializer;

    public AbstractCassandraDao(final Keyspace keyspace, final ColumnFamily<K, C> columnFamily, final Serializer<C> columnSerializer, final Serializer<V> valueSerializer) {
        this.keyspace = keyspace;
        Preconditions.checkNotNull(this.keyspace, "Key space cannot be null");
        this.columnFamily = columnFamily;
        Preconditions.checkNotNull(this.columnFamily, "Column family cannot be null");
        this.columnSerializer = columnSerializer;
        Preconditions.checkNotNull(this.columnSerializer, "column serializer cannot be null");
        this.valueSerializer = valueSerializer;
        Preconditions.checkNotNull(this.valueSerializer, "value serializer cannot be null");
    }

    public Keyspace getKeyspace() {
        return keyspace;
    }

    public ColumnFamily<K, C> getColumnFamily() {
        return columnFamily;
    }

    public Serializer<C> getColumnSerializer() {
        return columnSerializer;
    }

    public Serializer<V> getValueSerializer() {
        return valueSerializer;
    }
}
