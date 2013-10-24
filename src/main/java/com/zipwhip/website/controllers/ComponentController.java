/**
 *
 */
package com.zipwhip.website.controllers;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.serializers.StringSerializer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Controller
public class ComponentController implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentController.class);

    private static final ColumnFamily<String, String> CF_CLIENT_TOPOLOGY = ColumnFamily.newColumnFamily(
            "ClientTopology",
            StringSerializer.get(),
            StringSerializer.get(),
            StringSerializer.get());

    @Autowired
    JmsTemplate jmsTemplate;

    @Autowired
    CuratorFramework curatorFramework;

    @Autowired
    Keyspace cassandraKeyspace;

    @RequestMapping(value = "/jms/send", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> sendJms(HttpServletRequest request) {
        String path = request.getParameter("path");
        String message = request.getParameter("message");

        jmsTemplate.convertAndSend(path, message);

        return Collections.singletonMap("sent", (Object)Boolean.TRUE);
    }

    @RequestMapping(value = "/jms/receive", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> receiveJms(HttpServletRequest request) {
        String path = request.getParameter("path");

        return Collections.singletonMap(path, jmsTemplate.receiveAndConvert(path));
    }

    @RequestMapping(value = "/cassandra/connect", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> connectToCassandra(HttpServletRequest request) throws Exception {
        String clientAddress = request.getParameter("clientAddress");
        String serverAddress = request.getParameter("serverAddress");

        boolean connected = connect(clientAddress, serverAddress);

        return Collections.singletonMap("success", (Object)connected);
    }

    @RequestMapping(value = "/cassandra/disconnect", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> disconnectFromCassandra(HttpServletRequest request) throws Exception {
        String clientAddress = request.getParameter("clientAddress");
        String serverAddress = request.getParameter("serverAddress");

        boolean disconnected = disconnect(clientAddress, serverAddress);

        return Collections.singletonMap("success", (Object)disconnected);
    }

    @RequestMapping(value = "/cassandra/get", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, String> getFromCassandra(HttpServletRequest request) throws ConnectionException {
        String clientAddress = request.getParameter("clientAddress");

        Set<String> serverAddresses = get(clientAddress);

        if (serverAddresses == null) {
            return Collections.singletonMap("serverAddresses", "");
        }

        StringBuilder sb = new StringBuilder();
        for (String serverAddress : serverAddresses) {
            sb.append(serverAddress).append(",");
        }
        String servers = sb.toString().substring(0, sb.toString().length()-1);//trim last ','

        return Collections.singletonMap("serverAddress", servers);
    }

    @RequestMapping(value = "/curator/lock", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, String> lockCurator(HttpServletRequest request) throws Exception {
        final String path = request.getParameter("path");

        InterProcessLock lock = null;
        try {
            lock = lockOrFail(path);
            Thread.sleep(500);
        } catch (Exception e) {
            return Collections.singletonMap("success", "false: " + e.getMessage());
        } finally {
            if (lock == null) {
                return Collections.singletonMap("success", "false: lock was null!");
            }
            lock.release();
        }

        return Collections.singletonMap("success", Boolean.TRUE.toString());
    }

    public boolean connect(String clientAddress, String serverAddress) throws Exception {
            InterProcessLock lock = lockOrFail("/" + clientAddress);

            try {
                Set<String> set = get(clientAddress);

                if (set != null && set.contains(serverAddress)) {
                    return false;
                }

                MutationBatch batch = cassandraKeyspace.prepareMutationBatch();

                batch.withRow(CF_CLIENT_TOPOLOGY, clientAddress)
                    .putEmptyColumn(serverAddress);

                batch.execute();

                return true;
            } finally {
                lock.release();
            }
        }

    public boolean disconnect(String clientAddress, String  serverAddress) throws Exception {
           InterProcessLock lock = lockOrFail("/" + clientAddress);

           try {
               Set<String> set = get(clientAddress);

               if (set == null) {
                   return false;
               } else if (!set.contains(serverAddress)) {
                   return false;
               }

               MutationBatch batch = cassandraKeyspace.prepareMutationBatch();

               batch.withRow(CF_CLIENT_TOPOLOGY, clientAddress)
                       .deleteColumn(serverAddress);

               batch.execute();

               return true;
           } finally {
               lock.release();
           }
       }

    private Set<String> get(String clientAddressString) throws ConnectionException {
           ColumnList<String> columnList = cassandraKeyspace.prepareQuery(CF_CLIENT_TOPOLOGY)
                   .getRow(clientAddressString)
                   .execute()
                   .getResult();

           if (columnList == null || columnList.isEmpty()) {
               return null;
           }

           Set<String > set = new HashSet<String>();

           for (Column<String> column : columnList) {
               String addressString = column.getName();

               set.add(addressString);
           }

           return set;
       }

    private InterProcessMutex lockOrFail(String lockPath) throws Exception {
        InterProcessMutex mutex;

        try {
            mutex = new InterProcessMutex(curatorFramework, lockPath);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Illegal path: " + lockPath + " - " + e.getMessage());
        }

        if (!mutex.acquire(10, TimeUnit.SECONDS)) {
            throw new RuntimeException(String.format("Unable to acquire lock for %s in 10 seconds. Giving up???!", lockPath));
        }

        return mutex;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (curatorFramework.getState() != CuratorFrameworkState.STARTED) {
            curatorFramework.start();
        }
    }
}
