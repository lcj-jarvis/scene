package com.mrlu.server;

import java.sql.*;

public class SavepointExample {

    private static final String DB_URL = "jdbc:mysql://192.168.15.104:3306/test?useUnicode=true&useSSL=false";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    public static void main(String[] args) {
        Connection conn = null;
        Savepoint savepoint1 = null;

        try {
            // 1. 获取数据库连接
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            // 2. 关闭自动提交模式
            conn.setAutoCommit(false);

            // 3. 插入第一条记录
            String sql1 = "INSERT INTO t_person (name, age) VALUES (?, ?)";
            try (PreparedStatement pstmt1 = conn.prepareStatement(sql1)) {
                pstmt1.setString(1, "John Doe");
                pstmt1.setInt(2, 18);
                pstmt1.executeUpdate();
            }

            // 4. 设置保存点
            savepoint1 = conn.setSavepoint("savepoint1");

            // 5. 插入第二条记录
            String sql2 = "INSERT INTO t_person(name, age) VALUES (?, ?)";
            try (PreparedStatement pstmt2 = conn.prepareStatement(sql2)) {
                pstmt2.setString(1, "Jane Doe");
                pstmt2.setInt(2, 20);
                pstmt2.executeUpdate();
            }

            // 模拟错误：将其注释掉以实际测试
            //int error = 1 / 0;

            // 6. 回滚到保存点
            conn.rollback(savepoint1);

            // 7. 插入另一条记录
            String sql3 = "INSERT INTO t_person(name, age) VALUES (?, ?)";
            try (PreparedStatement pstmt3 = conn.prepareStatement(sql3)) {
                pstmt3.setString(1, "Alice Smith");
                pstmt3.setInt(2, 25);
                pstmt3.executeUpdate();
            }

            // 8. 提交事务
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
