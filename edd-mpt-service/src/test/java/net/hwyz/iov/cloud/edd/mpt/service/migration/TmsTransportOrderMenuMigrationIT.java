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
 * OTD-TMS 运单管理菜单迁移集成测试（V10）。
 * <p>
 * 预置历史运输域菜单（含装载车旧的 loadingVehicle:list 权限标识）后执行 V10 迁移，校验：
 * 运单管理页面与 7 个按钮权限、装载车权限标识对齐服务端、超级管理员授权。
 *
 * @author hwyz_leo
 */
class TmsTransportOrderMenuMigrationIT {

    private static final String MYSQL_HOST = env("TMS_IT_MYSQL_HOST", "127.0.0.1");

    private static final String MYSQL_PORT = env("TMS_IT_MYSQL_PORT", "3306");

    private static final String HOST = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/";

    private static final String PARAMS = "?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true"
            + "&allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC"
            + "&forceConnectionTimeZoneToSession=true&preserveInstants=true";

    private static final String USER = "root";

    private static final String PASSWORD = "9jpHx:G9NBM@Wst";

    private static final String DATABASE = "db_mpt_transport_order_it";

    private static final String URL = HOST + DATABASE + PARAMS;

    @BeforeAll
    static void requireMysql() {
        Assumptions.assumeTrue(reachable(), "MySQL 不可用，跳过运单菜单迁移集成测试");
    }

    @Test
    @DisplayName("V10 补齐运单管理页面与按钮权限并对齐装载车权限标识")
    void migrateTransportOrderMenus() throws Exception {
        recreateDatabase();
        try {
            migrateTo("9");
            long rootId = seedLegacyMenus();
            migrateTo(null);

            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE perms = "
                    + "'completeVehicle:transport:transportOrder:list' AND menu_type = 'C' "
                    + "AND component = 'completevehicle/transport/transportorder/index' "
                    + "AND parent_id = " + rootId)).isEqualTo(1);

            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE perms IN ("
                    + "'completeVehicle:transport:transportOrder:query',"
                    + "'completeVehicle:transport:transportOrder:dispatch',"
                    + "'completeVehicle:transport:transportOrder:depart',"
                    + "'completeVehicle:transport:transportOrder:handover',"
                    + "'completeVehicle:transport:transportOrder:receipt',"
                    + "'completeVehicle:transport:transportOrder:cancel',"
                    + "'completeVehicle:transport:transportOrder:manualClose') "
                    + "AND menu_type = 'F'")).isEqualTo(7);

            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE perms = "
                    + "'completeVehicle:transport:loadingVehicle:list'")).isZero();
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE perms = "
                    + "'completeVehicle:transport:loadVehicle:list'")).isEqualTo(1);

            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id IN "
                    + "(SELECT menu_id FROM sys_menu WHERE perms = 'completeVehicle:transport:transportOrder:list' "
                    + "OR perms LIKE 'completeVehicle:transport:transportOrder:%')")).isGreaterThanOrEqualTo(8);
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
            long rootId = insertMenu(connection, 0L, "运输管理", "transport", "M", null);
            long baseInfoId = insertMenu(connection, rootId, "基础信息管理", "baseInfo", "M", null);
            insertMenu(connection, baseInfoId, "物流据点类型管理", "logisticsNodeType",
                    "C", "completeVehicle:transport:logisticsNodeType:list", "completevehicle/transport/logisticsnodetype/index");
            insertMenu(connection, baseInfoId, "物流据点管理", "logisticsNode",
                    "C", "completeVehicle:transport:logisticsNode:list", "completevehicle/transport/logisticsnode/index");
            insertMenu(connection, baseInfoId, "装载车管理", "loadingVehicle",
                    "C", "completeVehicle:transport:loadingVehicle:list", "completevehicle/transport/loadingvehicle/index");
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO sys_role (role_id, role_name, role_key, role_sort, status, "
                        + "create_by, create_time) VALUES (1, '超级管理员', 'admin', 1, '0', 'admin', NOW())");
                statement.execute("INSERT INTO sys_role (role_id, role_name, role_key, role_sort, status, "
                        + "create_by, create_time) VALUES (100, '超级用户', 'SuperUser', 1, '0', 'admin', NOW())");
            }
            return rootId;
        }
    }

    private long insertMenu(Connection connection, Long parentId, String name, String path,
                            String menuType, String perms) throws Exception {
        return insertMenu(connection, parentId, name, path, menuType, perms, null);
    }

    private long insertMenu(Connection connection, Long parentId, String name, String path,
                            String menuType, String perms, String component) throws Exception {
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
