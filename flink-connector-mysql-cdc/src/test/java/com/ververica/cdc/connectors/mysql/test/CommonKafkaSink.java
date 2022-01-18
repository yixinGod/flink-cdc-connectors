package com.ververica.cdc.connectors.mysql.test;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
import com.google.common.hash.Hashing;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class CommonKafkaSink extends RichSinkFunction<String> {

    protected static final Logger LOG = LoggerFactory.getLogger(CommonKafkaSink.class);
    private transient KafkaProducer<String, String> kafkaProducer;
//    private transient JsonParser parser;

    @Override
    public void open(Configuration parameters) {
        Properties prop = new Properties();
        prop.put("bootstrap.servers", "ithdp001:6667,ithdp002:6667");
        prop.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        prop.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        prop.put("request.timeout.ms", "10");
        kafkaProducer = new KafkaProducer<>(prop);
//        parser = new JsonParser();

    }

    @Override
    public void invoke(String element, Context context) {

        JSONObject jsonObject = JSON.parseObject(element);
        String db = jsonObject.getString("db");
        String table = jsonObject.getString("table");
        // topic 不存在就自动创建

        String topic = "bigdata-mysql-cdc-test" + "." + db;
        String key = db + "." + table;

        Integer partition = Hashing.murmur3_128().hashBytes(key.getBytes(StandardCharsets.UTF_8)).asInt() % 6;
//        new ProducerRecord<>()
        ProducerRecord<String, String> record = new ProducerRecord<String,String>(topic,partition,key, element);
        kafkaProducer.send(record);
    }

    @Override
    public void close() {
        kafkaProducer.close();
    }

}