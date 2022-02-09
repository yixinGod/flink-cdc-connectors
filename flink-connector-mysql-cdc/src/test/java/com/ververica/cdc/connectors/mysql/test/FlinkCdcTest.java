package com.ververica.cdc.connectors.mysql.test;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import com.ververica.cdc.debezium.StringDebeziumDeserializationSchema;
import com.ververica.cdc.debezium.table.RowDataDebeziumDeserializeSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Properties;

public class FlinkCdcTest {
    public static void main(String[] args) throws Exception {

        Properties properties = new Properties();
        properties.put("decimal.handling.mode","string"); //precise/double/string
        properties.put("time.precision.mode","connect"); //
        properties.put("bigint.unsigned.handling.mode", "long");

        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname("81.69.18.16")
                .port(3306)
                .databaseList("streamx,real_task_manage") // set captured database
//                .tableList("streamx.test_cdc")
                .tableList("streamx.*\\..*,real_task_manage.*\\..*") // set captured table.*\\..*
//                .startupOptions(StartupOptions.initial())
                .startupOptions(StartupOptions.latest())
                .username("bigdata")
                .password("bookface06")
                .serverTimeZone("GMT+8")

//                .fetchSize(100)
//                .splitSize(1)
//                .serverTimeZone("")
//                .connectionPoolSize(5)
//                .serverId()

                .deserializer(new KeyJsonDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
//                .deserializer(new JsonDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
//                .deserializer(new StringDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
//                .deserializer(RowDataDebeziumDeserializeSchema.newBuilder().build())
//                .deserializer(new RowDataDebeziumDeserializeSchema())
                .includeSchemaChanges(true)
                .debeziumProperties(properties)
                .build();

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.enableCheckpointing(10000);
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(10000);
        env.getCheckpointConfig().setCheckpointTimeout(1000*300);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        env.getCheckpointConfig().setPreferCheckpointForRecovery(true);

        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3,2000L));
        env.setStateBackend(new FsStateBackend("file:///tmp/flink/FlinkCdcTest/ck"));

        // enable checkpoint
//        env.enableCheckpointing(3000);

//        env.fromSource(mySqlSource,WatermarkStrategy.noWatermarks(), "MySQL Source").print();
        DataStreamSource<String> dataStreamSource = env
                .fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source");
                // set 4 parallel source tasks
                dataStreamSource
                .print().setParallelism(1); // use parallelism 1 for sink to keep message ordering

        dataStreamSource.addSink(new CommonKafkaSink()).name("sink kafka");

        env.execute("Print MySQL Snapshot + Binlog");
    }
}
