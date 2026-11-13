package net.hwyz.iov.cloud.edd.mpt.service.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 头像列宽迁移集成测试（EDD-MPT-DSN-CR-001 §3.1）。
 * <p>
 * 在本机 MySQL 上重建 MPT 库结构，先迁移到 V5 校验基线列宽，再执行 V6 校验 avatar 加宽且字段数量不变。
 *
 * @author hwyz_leo
 */
class SysUserAvatarMigrationIT {

    private static final String MYSQL_HOST = env("MPT_IT_MYSQL_HOST", "127.0.0.1");

    private static final String MYSQL_PORT = env("MPT_IT_MYSQL_PORT", "3306");

    private static final String HOST = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/";

    private static final String PARAMS = "?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true"
            + "&allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC"
            + "&forceConnectionTimeZoneToSession=true&preserveInstants=true";

    private static final String USER = env("MPT_IT_MYSQL_USER", "root");

    private static final String PASSWORD = env("MPT_IT_MYSQL_PASSWORD", "9jpHx:G9NBM@Wst");

    private static final String DATABASE = "db_mpt_avatar_it";

    private static final String URL = HOST + DATABASE + PARAMS;

    @BeforeAll
    static void requireMysql() {
        Assumptions.assumeTrue(reachable(), "MySQL 不可用，跳过头像列宽迁移集成测试");
    }

    @Test
    @DisplayName("V6 将 sys_user.avatar 由 varchar(100) 加宽至 varchar(500) 且不改动其他字段")
    void widenAvatarColumn() throws Exception {
        recreateDatabase();
        try {
            migrateTo("5");
            assertThat(columnLength("avatar")).isEqualTo(100L);
            assertThat(columnCount()).isEqualTo(19);

            migrateTo(null);

            assertThat(columnLength("avatar")).isEqualTo(500L);
            assertThat(columnCount()).isEqualTo(19);
            assertThat(columnComment("avatar")).isEqualTo("头像地址");
            assertThat(appliedVersion()).isEqualTo("6");
        } finally {
            dropDatabase();
        }
    }

    private void migrateTo(String version) {
        var configuration = Flyway.configure()
                .dataSource(URL, USER, PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true);
        if (version != null) {
            configuration.target(MigrationVersion.fromVersion(version));
        }
        configuration.load().migrate();
    }

    private Long columnLength(String column) throws Exception {
        String sql = "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = '" + DATABASE + "' AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = '" + column + "'";
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getLong(1);
        }
    }

    private String columnComment(String column) throws Exception {
        String sql = "SELECT COLUMN_COMMENT FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = '" + DATABASE + "' AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = '" + column + "'";
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }

    private int columnCount() throws Exception {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = '" + DATABASE + "' AND TABLE_NAME = 'sys_user'";
        return queryInt(sql);
    }

    private String appliedVersion() throws Exception {
        String sql = "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1";
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }

    private int queryInt(String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static boolean reachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(MYSQL_HOST, Integer.parseInt(MYSQL_PORT)), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private void recreateDatabase() throws Exception {
        try (Connection connection = DriverManager.getConnection(HOST + PARAMS, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + DATABASE);
            statement.execute("CREATE DATABASE " + DATABASE + " DEFAULT CHARSET utf8mb4");
        }
    }

    private void dropDatabase() {
        try (Connection connection = DriverManager.getConnection(HOST + PARAMS, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + DATABASE);
        } catch (Exception ignored) {
            // 清理失败不影响测试结论
        }
    }

}
