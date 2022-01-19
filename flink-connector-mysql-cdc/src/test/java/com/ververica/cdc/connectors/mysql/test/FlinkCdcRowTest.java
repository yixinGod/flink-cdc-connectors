package com.ververica.cdc.connectors.mysql.test;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;

import java.util.Properties;

public class FlinkCdcRowTest {
    public static void main(String[] args) throws Exception {

        Properties properties = new Properties();
        properties.put("decimal.handling.mode","string"); //precise/double/string
        properties.put("time.precision.mode","connect"); //
        properties.put("bigint.unsigned.handling.mode", "long");

        MySqlSource<RowData> mySqlSource = MySqlSource.<RowData>builder()
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

//                .deserializer(new KeyJsonDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
//                .deserializer(new JsonDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
//                .deserializer(new StringDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
                .deserializer(RowDataDebeziumDeserializeSchema.newBuilder().build())
//                .deserializer(new RowDataDebeziumDeserializeSchema(RowType.of(new LogicalType)))
                .includeSchemaChanges(true)
                .debeziumProperties(properties)
                .build();

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.enableCheckpointing(10000);
        env.setStateBackend(new FsStateBackend("file:///tmp/flink/FlinkCdcTest/ck"));

        // enable checkpoint
        env.enableCheckpointing(3000);

//        env.fromSource(mySqlSource,WatermarkStrategy.noWatermarks(), "MySQL Source").print();
        DataStreamSource<RowData> dataStreamSource = env
                .fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source");
                // set 4 parallel source tasks
                dataStreamSource
                .print().setParallelism(1); // use parallelism 1 for sink to keep message ordering

//        dataStreamSource.addSink(new CommonKafkaSink()).name("sink kafka");

        env.execute("Print MySQL Snapshot + Binlog");
    }
}
