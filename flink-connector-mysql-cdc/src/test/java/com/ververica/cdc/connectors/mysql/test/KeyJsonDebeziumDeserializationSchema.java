/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.cdc.connectors.mysql.test;

import com.alibaba.fastjson.JSONObject;
import com.ververica.cdc.debezium.DebeziumDeserializationSchema;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.util.Collector;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.json.JsonConverterConfig;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.storage.ConverterConfig;
import org.apache.kafka.connect.storage.ConverterType;

import java.util.HashMap;

/**
 * A JSON format implementation of {@link DebeziumDeserializationSchema} which deserializes the
 * received {@link SourceRecord} to JSON String.
 */
public class KeyJsonDebeziumDeserializationSchema implements DebeziumDeserializationSchema<String> {

    private static final long serialVersionUID = 1L;

    private transient JsonConverter jsonConverter;

    /**
     * Configuration whether to enable {@link JsonConverterConfig to include
     * schema in messages.
     */
    private final Boolean includeSchema;

    public KeyJsonDebeziumDeserializationSchema() {
        this(false);
    }

    public KeyJsonDebeziumDeserializationSchema(Boolean includeSchema) {
        this.includeSchema = includeSchema;
    }

    @Override
    public void deserialize(SourceRecord record, Collector<String> out) throws Exception {
        if (jsonConverter == null) {
            // initialize jsonConverter
            jsonConverter = new JsonConverter();
            final HashMap<String, Object> configs = new HashMap<>(2);
            configs.put(ConverterConfig.TYPE_CONFIG, ConverterType.VALUE.getName());
            configs.put(JsonConverterConfig.SCHEMAS_ENABLE_CONFIG, includeSchema);
            jsonConverter.configure(configs);
        }
        byte[] bytes =
                jsonConverter.fromConnectData(record.topic(), record.valueSchema(), record.value());
        byte[] keys =
                jsonConverter.fromConnectData(record.topic(), record.keySchema(), record.key());

        JSONObject data = JSONObject.parseObject(new String(bytes));
        data.put("op",data.getOrDefault("op","alter"));
        data.put("table",data.getJSONObject("source").getString("table"));
        data.put("ts_ms",data.getJSONObject("source").getLong("ts_ms"));
        data.put("db",data.getJSONObject("source").getString("db"));
        if (keys != null){
            data.put("primary",JSONObject.parseObject(new String(keys)));
        }
        data.put("parse_time",System.currentTimeMillis());
        out.collect(data.toJSONString());
    }

    @Override
    public TypeInformation<String> getProducedType() {
        return BasicTypeInfo.STRING_TYPE_INFO;
    }
}



