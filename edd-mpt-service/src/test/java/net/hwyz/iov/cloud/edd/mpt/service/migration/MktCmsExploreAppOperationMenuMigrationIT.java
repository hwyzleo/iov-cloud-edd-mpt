package net.hwyz.iov.cloud.edd.mpt.service.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MKT-CMS 探索页菜单迁移至 App运营集成测试（V17）。
 * <p>
 * 在 V16 已建立营销管理下探索页的基础上执行 V17，校验页面移动、权限稳定、
 * 新上级目录授权及重复执行幂等性。
 *
 * @author hwyz_leo
 */
class MktCmsExploreAppOperationMenuMigrationIT {

    private static final String MYSQL_HOST = env("MKT_CMS_IT_MYSQL_HOST", "127.0.0.1");

    private static final String MYSQL_PORT = env("MKT_CMS_IT_MYSQL_PORT", "3306");

    private static final String HOST = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/";

    private static final String PARAMS = "?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true"
            + "&allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC"
            + "&forceConnectionTimeZoneToSession=true&preserveInstants=true";

    private static final String USER = "root";

    private static final String PASSWORD = "9jpHx:G9NBM@Wst";

    private static final String DATABASE = "db_mpt_mkt_cms_explore_app_menu_it";

    private static final String URL = HOST + DATABASE + PARAMS;

    @BeforeAll
    static void requireMysql() {
        Assumptions.assumeTrue(reachable(), "MySQL 不可用，跳过探索页 App运营菜单迁移集成测试");
    }

    @Test
    @DisplayName("V17 移动探索页至 App运营并保留权限与授权，且可重复执行")
    void migrateExploreToAppOperation() throws Exception {
        recreateDatabase();
        try {
            migrateTo("15");
            long marketingId = scalar("SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'marketing'");
            seedRolesAndGrants(marketingId);
            migrateTo("16");

            long exploreId = scalar("SELECT menu_id FROM sys_menu WHERE perms = 'mkt:cms:explore:query'");
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE menu_id = " + exploreId
                    + " AND parent_id = " + marketingId)).isEqualTo(1);

            migrateTo("17");

            long appOperationId = scalar("SELECT menu_id FROM sys_menu WHERE parent_id = 0"
                    + " AND path = 'app-operation'");
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE menu_id = " + appOperationId
                    + " AND menu_name = 'App运营' AND menu_type = 'M'")).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE menu_id = " + exploreId
                    + " AND parent_id = " + appOperationId
                    + " AND menu_name = '探索页管理' AND path = 'explore'"
                    + " AND component = 'cms/explore/index'"
                    + " AND perms = 'mkt:cms:explore:query'")).isEqualTo(1);

            // 按钮仍归属原页面，权限标识不变
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE parent_id = " + exploreId
                    + " AND menu_type = 'F' AND perms LIKE 'mkt:cms:explore:%'")).isEqualTo(4);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'mkt:cms:explore:%'"))
                    .isEqualTo(5);

            // 原页面角色保留页面授权并获得新上级目录，原营销管理授权不被误回收
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 2"
                    + " AND menu_id = " + exploreId)).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 2"
                    + " AND menu_id = " + appOperationId)).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 2"
                    + " AND menu_id = " + marketingId)).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1"
                    + " AND menu_id = " + appOperationId)).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1"
                    + " AND menu_id IN (SELECT menu_id FROM sys_menu WHERE perms LIKE 'mkt:cms:explore:%')"))
                    .isEqualTo(5);

            // 重复执行不产生重复目录、页面、按钮或授权
            migrateTo("17");
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE parent_id = 0"
                    + " AND path = 'app-operation'")).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE menu_id = " + exploreId
                    + " AND parent_id = " + appOperationId)).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE parent_id = " + exploreId
                    + " AND menu_type = 'F'")).isEqualTo(4);
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 2"
                    + " AND menu_id = " + appOperationId)).isEqualTo(1);
        } finally {
            dropDatabase();
        }
    }

    private void migrateTo(String version) {
        var configuration = Flyway.configure()
                .dataSource(URL, USER, PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .target(MigrationVersion.fromVersion(version));
        configuration.load().migrate();
    }

    /**
     * 预置超管/超用角色，并模拟已拥有营销管理目录的普通角色。
     */
    private void seedRolesAndGrants(long marketingId) throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO sys_role (role_id, role_name, role_key, role_sort, status, "
                    + "create_by, create_time) VALUES (1, '超级管理员', 'admin', 1, '0', 'admin', NOW())");
            statement.execute("INSERT INTO sys_role (role_id, role_name, role_key, role_sort, status, "
                    + "create_by, create_time) VALUES (100, '超级用户', 'SuperUser', 2, '0', 'admin', NOW())");
            statement.execute("INSERT INTO sys_role (role_id, role_name, role_key, role_sort, status, "
                    + "create_by, create_time) VALUES (2, '内容运营', 'content', 3, '0', 'admin', NOW())");
            try (PreparedStatement prepared = connection.prepareStatement(
                    "INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)")) {
                prepared.setLong(1, 2L);
                prepared.setLong(2, marketingId);
                prepared.executeUpdate();
            }
        }
    }

    private long scalar(String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).as("查询应有结果: " + sql).isTrue();
            return resultSet.getLong(1);
        }
    }

    private int count(String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static boolean reachable() {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(MYSQL_HOST, Integer.parseInt(MYSQL_PORT)), 1000);
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
