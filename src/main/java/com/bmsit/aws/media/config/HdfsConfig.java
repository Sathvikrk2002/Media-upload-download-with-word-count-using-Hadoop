package com.bmsit.aws.media.config;

import org.apache.hadoop.fs.FileSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class HdfsConfig {
    @Value("${hdfs.uri}")
    private String hdfsUri;

    @Bean
    public FileSystem fileSystem() throws IOException, URISyntaxException {
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        conf.set("fs.defaultFS", hdfsUri);
        conf.set("dfs.datanode.use.datanode.hostname", "true");
        conf.set("dfs.datanode.use.datanode.hostname", "true");
        return FileSystem.get(new URI(hdfsUri), conf);
    }
}
