package com.zipwhip.cassandra;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import java.util.List;

/**
 * @author Ali Serghini
 *         Date: 8/19/13
 *         Time: 11:56 AM
 *         <p/>
 *         K -  Key Type
 *         C -  Column Type
 *         V -  Value Type
 */
public interface ICassandraDao<K, C, V> {

    /**
     * Put a column name and value
     * ttl is not set.
     *
     * @param rowKey     - row key
     * @param columnName column name
     * @param value      - value
     * @throws ConnectionException
     */
    public void put(final K rowKey, final C columnName, final V value) throws ConnectionException;

    /**
     * Put a column name and value
     *
     * @param rowKey     - row key
     * @param columnName column name
     * @param value      - value
     * @param ttl        - ttl
     * @throws ConnectionException
     */
    public void put(final K rowKey, final C columnName, final V value, final Integer ttl) throws ConnectionException;

    /**
     * Get a column value by column name and row key
     *
     * @param rowKey     - row key
     * @param columnName column name
     * @return column value
     * @throws ConnectionException
     */
    public V get(final K rowKey, final C columnName) throws ConnectionException;

    /**
     * Get the max column value in the row
     *
     * @param rowKey - row key
     * @return max column value
     * @throws ConnectionException
     */
    public V getMaxColumnValue(final K rowKey) throws ConnectionException;

    /**
     * Get a range of values based on the column name start and end.
     *
     * @param rowKey          - row key
     * @param columnNameStart - start column name
     * @param columnNameEnd   - end column name
     * @param limit           - limit values returned
     * @return list of columns values
     * @throws ConnectionException
     */
    public List<V> getRange(final K rowKey, C columnNameStart, C columnNameEnd, final int limit) throws ConnectionException;
}
