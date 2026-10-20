package com.jimuqu.common.excel.utils;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.xml.XmlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelDependencyCompatibilityTest {
    @TempDir
    Path directory;

    @Test
    void initializesTheExcelCacheXmlBindingAndStoresValues() throws Exception {
        Path xml = directory.resolve("ehcache.xml");
        Files.writeString(xml, """
                <config xmlns="http://www.ehcache.org/v3">
                  <cache alias="compat">
                    <key-type>java.lang.String</key-type>
                    <value-type>java.lang.String</value-type>
                    <heap unit="entries">10</heap>
                  </cache>
                </config>
                """);
        try (CacheManager manager = CacheManagerBuilder.newCacheManager(new XmlConfiguration(xml.toUri().toURL()))) {
            manager.init();
            Cache<String, String> cache = manager.getCache("compat", String.class, String.class);
            cache.put("key", "value");
            assertEquals("value", cache.get("key"));
        }
    }
}
