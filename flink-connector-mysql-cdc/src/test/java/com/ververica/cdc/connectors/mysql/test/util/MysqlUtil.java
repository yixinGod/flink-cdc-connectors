package com.ververica.cdc.connectors.mysql.test.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * @Author: fangjian.mei
 * @Date: 2018/9/11 17:35
 */
public class MysqlUtil {

    private static Connection connection;

    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlUtil.class);

    public static void main(String []args){


        if("insert,update,delete".contains("update".toLowerCase())){

            LOGGER.info("sssffff");
        }

    }


    public static Connection getConnection(Properties properties){
        if (connection==null){
            createConnection(properties);
        }
        return connection;
    }

    public static void createConnection(Properties properties)  {
        try {
            Class.forName(properties.getProperty("jdbc.driver"));
            connection = DriverManager.getConnection(properties.getProperty("jdbc.url"),properties.getProperty("jdbc.username"), properties.getProperty("jdbc.password"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        if (connection!=null){
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<Map<String,Object>> executeSelect(Connection conn,String sql){
        List<Map<String,Object>> result = null;
        try {
            ResultSet rs = conn.prepareStatement(sql).executeQuery();
            result = extractData(rs);
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public static void executeSql(Connection conn,String sql){

        try {
            conn.prepareStatement(sql).execute();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void executeSql(Connection conn,String sql,List list){

        try {
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            for (int i =0;i<list.size();i++){
                preparedStatement.setObject(i+1,list.get(i));
            }
            preparedStatement.execute();
        }catch (Exception e){
            e.printStackTrace();
        }
    }




    public static List<Map<String,Object>> extractData(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int num = md.getColumnCount();
        List listOfRows = new ArrayList();
        while (rs.next()) {
            Map mapOfColValues = new LinkedHashMap(num);
            for (int i = 1; i <= num; i++) {
                mapOfColValues.put(md.getColumnLabel(i), rs.getObject(i));
            }
            listOfRows.add(mapOfColValues);
        }
        return listOfRows;
    }

}
