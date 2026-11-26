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
 * MKT-CMS App运营目录归入营销管理集成测试（V18）。
 * <p>
 * 在 V17 已形成 App运营 / 探索页两级菜单的基础上执行 V18，校验最终层级为
 * 营销管理 / App运营 / 探索页管理，权限与角色授权保持稳定且迁移可重复执行。
 *
 * @author hwyz_leo
 */
class MktCmsAppOperationParentMenuMigrationIT {

    private static final String MYSQL_HOST = env("MKT_CMS_IT_MYSQL_HOST", "127.0.0.1");

    private static final String MYSQL_PORT = env("MKT_CMS_IT_MYSQL_PORT", "3306");

    private static final String HOST = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/";

    private static final String PARAMS = "?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true"
            + "&allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC"
            + "&forceConnectionTimeZoneToSession=true&preserveInstants=true";

    private static final String USER = "root";

    private static final String PASSWORD = "9jpHx:G9NBM@Wst";

    private static final String DATABASE = "db_mpt_mkt_cms_app_parent_menu_it";

    private static final String URL = HOST + DATABASE + PARAMS;

    @BeforeAll
    static void requireMysql() {
        Assumptions.assumeTrue(reachable(), "MySQL 不可用，跳过 App运营上级菜单迁移集成测试");
    }

    @Test
    @DisplayName("V18 将 App运营归入营销管理并保持探索页权限，且可重复执行")
    void migrateAppOperationUnderMarketing() throws Exception {
        recreateDatabase();
        try {
            migrateTo("15");
            long marketingId = scalar("SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'marketing'");
            seedRolesAndGrants(marketingId);
            migrateTo("16");
            long exploreId = scalar("SELECT menu_id FROM sys_menu WHERE perms = 'mkt:cms:explore:query'");
            migrateTo("17");
            long appOperationId = scalar("SELECT menu_id FROM sys_menu WHERE parent_id = 0"
                    + " AND path = 'app-operation'");

            migrateTo("18");

            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE menu_id = " + appOperationId
                    + " AND parent_id = " + marketingId
                    + " AND menu_name = 'App运营' AND menu_type = 'M'"
                    + " AND path = 'app-operation'")).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE menu_id = " + exploreId
                    + " AND parent_id = " + appOperationId
                    + " AND menu_name = '探索页管理' AND path = 'explore'"
                    + " AND component = 'cms/explore/index'"
                    + " AND perms = 'mkt:cms:explore:query'")).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE parent_id = " + exploreId
                    + " AND menu_type = 'F' AND perms LIKE 'mkt:cms:explore:%'")).isEqualTo(4);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'mkt:cms:explore:%'"))
                    .isEqualTo(5);

            // 页面与按钮授权保持不变，角色同时具备营销管理和 App运营上级目录
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 2"
                    + " AND menu_id = " + exploreId)).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 2"
                    + " AND menu_id = " + appOperationId)).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 2"
                    + " AND menu_id = " + marketingId)).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1"
                    + " AND menu_id = " + marketingId)).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1"
                    + " AND menu_id IN (SELECT menu_id FROM sys_menu WHERE perms LIKE 'mkt:cms:explore:%')"))
                    .isEqualTo(5);

            migrateTo("18");
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE menu_id = " + appOperationId
                    + " AND parent_id = " + marketingId)).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE parent_id = " + appOperationId
                    + " AND path = 'explore'")).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE parent_id = " + exploreId
                    + " AND menu_type = 'F'")).isEqualTo(4);
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 2"
                    + " AND menu_id = " + marketingId)).isEqualTo(1);
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
