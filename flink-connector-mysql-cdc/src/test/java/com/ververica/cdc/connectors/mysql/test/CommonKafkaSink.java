package com.ververica.cdc.connectors.mysql.test;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
import com.google.common.hash.Hashing;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
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

        Properties props = new Properties();
        props.put("bootstrap.servers", "ithdp001:6667,ithdp002:6667");
        props.put("acks", "all");
        props.put("retries", 3);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());

        kafkaProducer = new KafkaProducer<String,String>(props);
//        parser = new JsonParser();

    }

    @Override
    public void invoke(String element, Context context) {

//        JSONObject data = JSON.parseObject(element);
//        data.put("push_time",System.currentTimeMillis());
//        JSONObject jsonObject = data.getJSONObject("source");
        JSONObject jsonObject = JSON.parseObject(element).getJSONObject("source");

        String db = jsonObject.getString("db");
        String table = jsonObject.getString("table");
        // topic 不存在就自动创建

        String topic = "bigdata-mysql-cdc-test" + "." + db;
        String key = db + "." + table;

        Integer partition = Hashing.murmur3_128().hashBytes(key.getBytes(StandardCharsets.UTF_8)).asInt() % 6;
        partition = 0;
//        new ProducerRecord<>()
        ProducerRecord<String, String> record = new ProducerRecord<String,String>(topic,partition,key, element);
        kafkaProducer.send(record);
    }

    @Override
    public void close() {
        kafkaProducer.close();
    }

}