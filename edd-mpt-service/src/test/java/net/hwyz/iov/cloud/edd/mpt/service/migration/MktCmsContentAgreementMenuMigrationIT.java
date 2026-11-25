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
 * MKT-CMS 内容管理菜单层级调整迁移集成测试（V15）。
 * <p>
 * 先迁移到 V14（协议内容挂在「营销管理 / 内容管理」二级页面），预置角色授权后迁移 V15，校验：
 * 内容管理升级为目录、新增三级页面「协议管理」、协议按钮改挂新页面、角色授权自动补齐。
 *
 * @author hwyz_leo
 */
class MktCmsContentAgreementMenuMigrationIT {

    private static final String MYSQL_HOST = env("MKT_CMS_IT_MYSQL_HOST", "127.0.0.1");

    private static final String MYSQL_PORT = env("MKT_CMS_IT_MYSQL_PORT", "3306");

    private static final String HOST = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/";

    private static final String PARAMS = "?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true"
            + "&allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC"
            + "&forceConnectionTimeZoneToSession=true&preserveInstants=true";

    private static final String USER = "root";

    private static final String PASSWORD = "9jpHx:G9NBM@Wst";

    private static final String DATABASE = "db_mpt_mkt_cms_menu_it";

    private static final String URL = HOST + DATABASE + PARAMS;

    @BeforeAll
    static void requireMysql() {
        Assumptions.assumeTrue(reachable(), "MySQL 不可用，跳过内容管理菜单迁移集成测试");
    }

    @Test
    @DisplayName("V15 内容管理目录化并新增协议管理三级页面")
    void migrateContentAgreementMenus() throws Exception {
        recreateDatabase();
        try {
            migrateTo("14");
            long marketingId = scalar("SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'marketing'");
            long contentId = scalar("SELECT menu_id FROM sys_menu WHERE parent_id = " + marketingId
                    + " AND path = 'content'");
            long roleId = seedRolesAndGrants(contentId);

            migrateTo(null);

            // 1. 内容管理：目录化，组件交由 ParentView，权限标识交由下级页面承载
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE menu_id = " + contentId
                    + " AND menu_type = 'M' AND component IS NULL AND perms = ''"
                    + " AND parent_id = " + marketingId)).isEqualTo(1);

            // 2. 协议管理：三级页面
            long agreementMenuId = scalar("SELECT menu_id FROM sys_menu WHERE path = 'agreement'"
                    + " AND parent_id = " + contentId);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE menu_id = " + agreementMenuId
                    + " AND menu_name = '协议管理' AND menu_type = 'C'"
                    + " AND component = 'cms/agreement/index'"
                    + " AND perms = 'mkt:cms:agreement:query'")).isEqualTo(1);

            // 3. 协议按钮全部改挂到协议管理页面，权限标识不变（查询权限由页面承载，故为 4 个按钮）
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE parent_id = " + agreementMenuId
                    + " AND menu_type = 'F' AND perms LIKE 'mkt:cms:agreement:%'")).isEqualTo(4);
            assertThat(count("SELECT COUNT(*) FROM sys_menu WHERE parent_id = " + contentId
                    + " AND menu_type = 'F'")).isZero();

            // 4. 原持有「内容管理」菜单的角色自动获得协议管理页面及上级目录
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = " + roleId
                    + " AND menu_id = " + agreementMenuId)).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = " + roleId
                    + " AND menu_id IN (" + marketingId + "," + contentId + ")")).isEqualTo(2);

            // 5. 超级管理员/超级用户兜底授权协议域全部菜单（2 个目录 + 页面 + 4 个按钮）
            assertThat(count("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1"
                    + " AND menu_id IN (SELECT menu_id FROM sys_menu WHERE perms LIKE 'mkt:cms:agreement:%'"
                    + " OR menu_id IN (" + marketingId + "," + contentId + "))")).isEqualTo(7);
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
     * 预置超管/超用角色，并模拟“已拥有内容管理菜单”的普通角色，用于验证 V15 的授权补齐。
     */
    private long seedRolesAndGrants(long contentId) throws Exception {
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
                prepared.setLong(2, contentId);
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
