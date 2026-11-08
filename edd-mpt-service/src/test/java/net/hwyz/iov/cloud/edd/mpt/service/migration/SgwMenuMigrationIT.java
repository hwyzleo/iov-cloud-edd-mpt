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
 * 服务网关管理菜单迁移集成测试。
 * <p>
 * 在本机 MySQL 上重建 MPT 库结构，预置历史网关菜单后执行 V3 迁移并校验权限与菜单结果。
 *
 * @author hwyz_leo
 */
class SgwMenuMigrationIT {

    private static final String MYSQL_HOST = env("SGW_IT_MYSQL_HOST", "127.0.0.1");

    private static final String MYSQL_PORT = env("SGW_IT_MYSQL_PORT", "3306");

    private static final String HOST = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/";

    private static final String PARAMS = "?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true"
            + "&allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC"
            + "&forceConnectionTimeZoneToSession=true&preserveInstants=true";

    private static final String USER = "root";

    private static final String PASSWORD = "9jpHx:G9NBM@Wst";

    private static final String DATABASE = "db_mpt_migration_it";

    private static final String URL = HOST + DATABASE + PARAMS;

    @BeforeAll
    static void requireMysql() {
        Assumptions.assumeTrue(reachable(), "MySQL 不可用，跳过菜单迁移集成测试");
    }

    @Test
    @DisplayName("V3 修正历史权限前缀并补齐服务网关管理菜单与授权")
    void migrateSgwMenus() throws Exception {
        recreateDatabase();
        try {
            migrateTo("2");
            long rootId = seedLegacyMenus();
            migrateTo(null);

            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'tsp:sgw:%'")).isZero();
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE perms = 'edd:sgw:route:import'")).isZero();
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE perms IN ("
                    + "'edd:sgw:policy:list','edd:sgw:policy:query','edd:sgw:policy:add',"
                    + "'edd:sgw:policy:edit','edd:sgw:policy:remove','edd:sgw:policy:export',"
                    + "'edd:sgw:config:list','edd:sgw:config:query','edd:sgw:config:publish',"
                    + "'edd:sgw:config:rollback','edd:sgw:publication:query','edd:sgw:publication:retry',"
                    + "'edd:sgw:runtime:list','edd:sgw:upstream:list','edd:sgw:route:list',"
                    + "'edd:sgw:route:query','edd:sgw:route:add','edd:sgw:route:edit',"
                    + "'edd:sgw:route:remove','edd:sgw:route:export')")).isEqualTo(20);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE parent_id = " + rootId
                    + " AND path IN ('route','policies','versions','publications','runtime')")).isEqualTo(5);
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id IN "
                    + "(SELECT menu_id FROM sys_menu WHERE menu_id = " + rootId
                    + " OR perms LIKE 'edd:sgw:%')")).isGreaterThan(20);
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

    private long seedLegacyMenus() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            long rootId = insertMenu(connection, 2000L, "服务网关", "sgw", null, "M", "");
            long routeId = insertMenu(connection, rootId, "路由管理", "route", "edd/sgw/route/index",
                    "C", "tsp:sgw:route:list");
            insertMenu(connection, routeId, "路由查询", "", null, "F", "tsp:sgw:route:query");
            insertMenu(connection, routeId, "路由新增", "", null, "F", "tsp:sgw:route:add");
            insertMenu(connection, routeId, "路由修改", "", null, "F", "tsp:sgw:route:edit");
            insertMenu(connection, routeId, "路由删除", "", null, "F", "tsp:sgw:route:remove");
            insertMenu(connection, routeId, "路由导出", "", null, "F", "tsp:sgw:route:export");
            insertMenu(connection, routeId, "路由导入", "", null, "F", "tsp:sgw:route:import");
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO sys_role (role_id, role_name, role_key, role_sort, status, "
                        + "create_by, create_time) VALUES (1, '超级管理员', 'admin', 1, '0', 'admin', NOW())");
                statement.execute("INSERT INTO sys_role (role_id, role_name, role_key, role_sort, status, "
                        + "create_by, create_time) VALUES (100, '超级用户', 'SuperUser', 1, '0', 'admin', NOW())");
            }
            return rootId;
        }
    }

    private long insertMenu(Connection connection, Long parentId, String name, String path, String component,
                            String menuType, String perms) throws Exception {
        String sql = "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, "
                + "visible, status, perms, create_by, create_time) VALUES (?,?,?,?,?,?,'0','0',?,'admin',NOW())";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setLong(2, parentId);
            statement.setInt(3, 1);
            statement.setString(4, path);
            statement.setString(5, component);
            statement.setString(6, menuType);
            statement.setString(7, perms);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
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
