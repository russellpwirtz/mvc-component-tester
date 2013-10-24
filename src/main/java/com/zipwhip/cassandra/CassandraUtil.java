package com.zipwhip.cassandra;

import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.ColumnList;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ali Serghini
 *         Date: 7/22/13
 *         Time: 5:24 PM
 */
public class CassandraUtil {

    private CassandraUtil() {
    }

    public static <C, V> List<V> convertList(final ColumnList<C> columnList, final Serializer<V> serializer, final List<V> defaultValue) {
        if (columnList == null || columnList.isEmpty()) return defaultValue;

        final List<V> values = new ArrayList<V>(columnList.size());
        V value = null;
        // We want to preserve the sorting order. That is why we are using getColumnByIndex()
        for (int i = 0; i < columnList.size(); i++) {
            value = columnList.getColumnByIndex(i).getValue(serializer);
            if (value != null) {
                values.add(value);
            }
        }

        return values;
    }

    public static <C, V> V convertValue(final com.netflix.astyanax.model.Column<C> column, final Serializer<V> serializer, final V defaultValue) {
        if (column == null) return defaultValue;

        return column.getValue(serializer);
    }

    public static <C, V> V convertFirst(final ColumnList<C> columnList, final Serializer<V> serializer, final V defaultValue) {
        if (columnList == null || columnList.isEmpty()) return defaultValue;

        return columnList.getColumnByIndex(0).getValue(serializer);
    }

}
