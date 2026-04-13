package net.hwyz.iov.cloud.edd.mpt.api.domain;

import lombok.Getter;
import lombok.Setter;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.io.Serial;
import java.util.Date;

/**
 * TSP服务网关路由表 tb_route
 *
 * @author hwyz_leo
 */
@Getter
@Setter
public class TspSgwRoute extends BaseRequest {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 断言集合
     */
    private String predicates;

    /**
     * 过滤器集合
     */
    private String filters;

    /**
     * 目标类型
     */
    private String targetType;

    /**
     * 目标URI
     */
    private String targetUri;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 修改时间
     */
    private Date modifyTime;

    /**
     * 修改者
     */
    private String modifyBy;

    /**
     * 记录是否有效
     */
    private Boolean rowValid;

    public TspSgwRoute() {

    }

}
