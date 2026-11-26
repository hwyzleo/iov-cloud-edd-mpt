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
 * MKT-CMS 探索页菜单与权限迁移集成测试（V16）。
 * <p>
 * 先迁移到 V15（营销管理/内容管理/协议管理），预置角色与营销管理目录授权后迁移 V16，校验：
 * 营销管理下新增二级页面「探索页管理」、五个权限点、角色授权自动补齐，且迁移可重复执行。
 *
 * @author hwyz_leo
 */
class MktCmsExploreMenuMigrationIT {

    private static final String MYSQL_HOST = env("MKT_CMS_IT_MYSQL_HOST", "127.0.0.1");

    private static final String MYSQL_PORT = env("MKT_CMS_IT_MYSQL_PORT", "3306");

    private static final String HOST = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/";

    private static final String PARAMS = "?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true"
            + "&allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC"
            + "&forceConnectionTimeZoneToSession=true&preserveInstants=true";

    private static final String USER = "root";

    private static final String PASSWORD = "9jpHx:G9NBM@Wst";

    private static final String DATABASE = "db_mpt_mkt_cms_explore_menu_it";

    private static final String URL = HOST + DATABASE + PARAMS;

    @BeforeAll
    static void requireMysql() {
        Assumptions.assumeTrue(reachable(), "MySQL 不可用，跳过探索页菜单迁移集成测试");
    }

    @Test
    @DisplayName("V16 新增探索页管理二级页面、五权限点与授权，且可重复执行")
    void migrateExploreMenus() throws Exception {
        recreateDatabase();
        try {
            migrateTo("15");
            long marketingId = scalar("SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'marketing'");
            seedRolesAndGrants(marketingId);

            migrateTo(null);

            long exploreId = scalar("SELECT menu_id FROM sys_menu WHERE parent_id = " + marketingId
                    + " AND path = 'explore'");
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE menu_id = " + exploreId
                    + " AND menu_name = '探索页管理' AND menu_type = 'C'"
                    + " AND component = 'cms/explore/index'"
                    + " AND perms = 'mkt:cms:explore:query'")).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE menu_id = " + exploreId
                    + " AND parent_id = " + marketingId)).isEqualTo(1);

            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE parent_id = " + exploreId
                    + " AND menu_type = 'F' AND perms LIKE 'mkt:cms:explore:%'")).isEqualTo(4);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'mkt:cms:explore:%'"))
                    .isEqualTo(5);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE parent_id = " + exploreId
                    + " AND perms IN ('mkt:cms:explore:add','mkt:cms:explore:edit',"
                    + "'mkt:cms:explore:publish','mkt:cms:explore:offline')")).isEqualTo(4);

            // 原持有营销管理目录的角色自动获得探索页管理页面
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 2"
                    + " AND menu_id = " + exploreId)).isEqualTo(1);
            // 超级管理员兜底：页面 + 4 个按钮
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1"
                    + " AND menu_id IN (SELECT menu_id FROM sys_menu WHERE perms LIKE 'mkt:cms:explore:%')"))
                    .isEqualTo(5);

            // 迁移重复执行不产生重复菜单/授权
            migrateTo(null);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE parent_id = " + marketingId
                    + " AND path = 'explore'")).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE parent_id = " + exploreId
                    + " AND menu_type = 'F'")).isEqualTo(4);
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1"
                    + " AND menu_id IN (SELECT menu_id FROM sys_menu WHERE perms LIKE 'mkt:cms:explore:%')"))
                    .isEqualTo(5);
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

    /**
     * 预置超管/超用角色，并模拟“已拥有营销管理目录”的普通角色，用于验证 V16 授权补齐。
     */
    private long seedRolesAndGrants(long marketingId) throws Exception {
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
            return 2L;
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
