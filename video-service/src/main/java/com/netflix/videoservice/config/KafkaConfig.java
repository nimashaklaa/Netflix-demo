package com.netflix.videoservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    //published when video is uploaded in to S3
    @Bean
    public NewTopic videoUploadedTopic() {
        return TopicBuilder.name("video.uploaded").partitions(3).replicas(1).
                build();
    }

    //published when video is encoded
    @Bean
    public NewTopic videoEncodedTopic() {
        return TopicBuilder.name("video.encoded").partitions(3).replicas(1).
                build();
    }
}
