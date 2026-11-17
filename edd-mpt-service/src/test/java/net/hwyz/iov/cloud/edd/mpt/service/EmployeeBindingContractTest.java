package net.hwyz.iov.cloud.edd.mpt.service;

import net.hwyz.iov.cloud.edd.mpt.api.domain.SysUser;
import net.hwyz.iov.cloud.edd.mpt.api.model.LoginUser;
import net.hwyz.iov.cloud.edd.mpt.service.util.EmployeeContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EDD-MPT-DSN-CR-002：账号与员工绑定契约门禁测试。
 * <p>
 * 断言：Flyway V9 结构、Mapper 绑定语句、模型字段（SysUser/LoginUser）、框架 employeeId 取值方法。
 *
 * @author hwyz_leo
 * @since 2026-11-17
 */
class EmployeeBindingContractTest {

    @Test
    @DisplayName("Flyway V9 新增 employee_id 唯一键、pwd_reset_required 与索引")
    void flywayAddsBindingColumns() throws IOException {
        String sql = readClasspath("/db/migration/V9__sys_user_employee_binding.sql");
        assertThat(sql).contains("ADD COLUMN `employee_id` BIGINT DEFAULT NULL");
        assertThat(sql).contains("ADD COLUMN `pwd_reset_required` TINYINT NOT NULL DEFAULT 0");
        assertThat(sql).contains("CREATE UNIQUE INDEX `uk_employee_id` ON `sys_user` (`employee_id`)");
        assertThat(sql).contains("CREATE INDEX `idx_user_employee` ON `sys_user` (`employee_id`, `del_flag`)");
    }

    @Test
    @DisplayName("Mapper 提供按员工查询、绑定集合、解绑、按员工停用")
    void mapperExposesEmployeeBindingStatements() throws IOException {
        String xml = readClasspath("/mappers/SysUserMapper.xml");
        assertThat(xml).contains("id=\"selectUserByEmployeeId\"");
        assertThat(xml).contains("id=\"selectBoundEmployeeIds\"");
        assertThat(xml).contains("id=\"updateUserEmployeeUnbind\"");
        assertThat(xml).contains("id=\"updateStatusByEmployeeId\"");
        assertThat(xml).contains("id=\"selectUserIdsByEmployeeId\"");
        // 绑定列必须出现在查询列与结果映射中
        assertThat(xml).contains("u.employee_id");
        assertThat(xml).contains("column=\"employee_id\"");
        assertThat(xml).contains("column=\"pwd_reset_required\"");
    }

    @Test
    @DisplayName("模型暴露 employeeId / pwdResetRequired，且会话可携带 employeeId")
    void modelsExposeEmployeeBinding() {
        List<String> userFields = declaredFieldNames(SysUser.class);
        assertThat(userFields).contains("employeeId", "pwdResetRequired");

        List<String> loginFields = declaredFieldNames(LoginUser.class);
        assertThat(loginFields).contains("employeeId");

        Method getEmployeeId = Arrays.stream(EmployeeContext.class.getDeclaredMethods())
                .filter(method -> "getEmployeeId".equals(method.getName()))
                .findFirst()
                .orElse(null);
        assertThat(getEmployeeId).as("EmployeeContext.getEmployeeId()").isNotNull();
        assertThat(getEmployeeId.getReturnType()).isEqualTo(Long.class);
    }

    private List<String> declaredFieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !field.isSynthetic() && !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .map(Field::getName)
                .collect(Collectors.toList());
    }

    private String readClasspath(String path) throws IOException {
        try (InputStream inputStream = EmployeeBindingContractTest.class.getResourceAsStream(path)) {
            assertThat(inputStream).as(path).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
