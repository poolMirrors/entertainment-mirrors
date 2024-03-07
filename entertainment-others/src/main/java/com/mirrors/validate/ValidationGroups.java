package com.mirrors.validate;

/**
 * 用于 分组参数校验
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/15 21:40
 */
public class ValidationGroups {
    /**
     * 新增分组（用于添加校验）
     */
    public interface Insert {
    }

    /**
     * 更新分组（用于更新校验）
     */
    public interface Update {
    }

    /**
     * 删除分组（用于删除校验）
     */
    public interface Delete {
    }
}
