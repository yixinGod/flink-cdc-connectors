package com.ververica.cdc.connectors.mysql.test;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import com.ververica.cdc.debezium.StringDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class FlinkCdcTest {
    public static void main(String[] args) throws Exception {
        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname("81.69.18.16")
                .port(3306)
                .databaseList("streamx,real_task_manage") // set captured database
//                .tableList("streamx.test_cdc")
                .tableList("streamx.*\\..*,real_task_manage.*\\..*") // set captured table.*\\..*
                .startupOptions(StartupOptions.latest())
                .username("root")
                .password("mfj112021")
                .deserializer(new JsonDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
//                .deserializer(new StringDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
                .build();

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // enable checkpoint
        env.enableCheckpointing(3000);

//        env.fromSource(mySqlSource,WatermarkStrategy.noWatermarks(), "MySQL Source").print();
        DataStreamSource<String> dataStreamSource = env
                .fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source");
                // set 4 parallel source tasks
                dataStreamSource
                .print().setParallelism(1); // use parallelism 1 for sink to keep message ordering

//        dataStreamSource.addSink(new CommonKafkaSink()).setParallelism(1);

        env.execute("Print MySQL Snapshot + Binlog");
    }
}
