package com.ververica.cdc.connectors.mysql.test;

import com.ververica.cdc.connectors.mysql.test.util.MysqlUtil;
import org.junit.Test;

import java.sql.Connection;
import java.util.Properties;

public class MysqlTEst {
//    @Test
    public void test(){

        Properties properties = new Properties();
        properties.put("jdbc.driver","com.mysql.jdbc.Driver");
        properties.put("jdbc.url","jdbc:mysql://81.69.18.16:3306/streamx");
//        properties.put("jdbc.url","jdbc:mysql://81.69.18.16:3306/streamx?characterEncoding=UTF-8&amp;autoReconnect=true&amp;failOverReadOnly=false");
        properties.put("jdbc.username","root");
        properties.put("jdbc.password","mfj112021");
        Connection connection = MysqlUtil.getConnection(properties);
        MysqlUtil.executeSql(connection,"insert into test_cdc_key1 (ID,APP_ID) values (100,5)");


    }
}
