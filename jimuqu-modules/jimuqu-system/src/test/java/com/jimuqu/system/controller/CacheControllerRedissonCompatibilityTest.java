package com.jimuqu.system.controller;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.*;
import org.redisson.config.Config;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheControllerRedissonCompatibilityTest {
    @Test
    void selectsTheSingleRedisInstance() {
        RedissonClient client = mock(RedissonClient.class);
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        when(client.getConfig()).thenReturn(config);
        RedisSingle single = mock(RedisSingle.class);
        RedisMaster master = mock(RedisMaster.class);
        when(client.getRedisNodes(RedisNodes.SINGLE)).thenReturn(single);
        when(single.getInstance()).thenReturn(master);
        assertSame(master, CacheController.monitoringNode(client));
    }

    @Test
    void selectsAClusterMasterAndRejectsAnEmptyCluster() {
        RedissonClient client = mock(RedissonClient.class);
        Config config = new Config();
        config.useClusterServers().addNodeAddress("redis://127.0.0.1:6379");
        when(client.getConfig()).thenReturn(config);
        RedisCluster cluster = mock(RedisCluster.class);
        RedisClusterMaster master = mock(RedisClusterMaster.class);
        when(client.getRedisNodes(RedisNodes.CLUSTER)).thenReturn(cluster);
        when(cluster.getMasters()).thenReturn(List.of(master), List.of());
        assertSame(master, CacheController.monitoringNode(client));
        assertThrows(IllegalStateException.class, () -> CacheController.monitoringNode(client));
    }

    @Test
    void selectsTheSentinelMaster() {
        RedissonClient client = mock(RedissonClient.class);
        Config config = new Config();
        config.useSentinelServers().setMasterName("master");
        when(client.getConfig()).thenReturn(config);
        RedisSentinelMasterSlave nodes = mock(RedisSentinelMasterSlave.class);
        RedisMaster master = mock(RedisMaster.class);
        when(client.getRedisNodes(RedisNodes.SENTINEL_MASTER_SLAVE)).thenReturn(nodes);
        when(nodes.getMaster()).thenReturn(master);
        assertSame(master, CacheController.monitoringNode(client));
    }
}
