package com.zipwhip.cassandra;

import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;

/**
 * Date: 5/19/13
 * Time: 2:31 PM
 *
 * @author Michael
 * @version 1
 */
public class AstyanaxFactory {

    public static AstyanaxContext<Keyspace> createContext(String name, String cluster, String keyspace, String seeds, int maxConnsPerHost, int port) {
        AstyanaxContext<Keyspace> context = new AstyanaxContext.Builder()
                .forCluster(cluster)
                .forKeyspace(keyspace)
                .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                        .setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE)
                )
                .withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl(name)
                        .setPort(port)
                        .setMaxConnsPerHost(maxConnsPerHost)
                        .setSeeds(seeds)
                )
                .withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
                .buildKeyspace(ThriftFamilyFactory.getInstance());

        context.start();

        return context;
    }

    public static Keyspace createKeyspace(AstyanaxContext<Keyspace> context) {
        return context.getClient();
    }

}
