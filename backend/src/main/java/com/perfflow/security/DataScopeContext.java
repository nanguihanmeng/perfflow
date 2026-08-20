package com.perfflow.security;

import com.perfflow.common.constant.RoleConst;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 当前用户上下文（基于 ThreadLocal）。
 *
 * <p>由 {@code DataScopeContextFilter} 在请求进入时写入，
 * 由过滤器 finally 块清理，避免线程复用导致脏读。
 *
 * @author PerfFlow
 */
public final class DataScopeContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    /** HashSet 初始容量阈值，避免小数据集扩容。 */
    private static final int DEFAULT_ROLE_CAPACITY = 8;

    private DataScopeContext() {
    }

    /**
     * 写入当前用户上下文，传 null 视作清除。
     *
     * @param user 当前用户
     */
    public static void set(CurrentUser user) {
        if (user == null) {
            HOLDER.remove();
            return;
        }
        HOLDER.set(user);
    }

    /**
     * 获取当前用户上下文，未登录返回 null。
     */
    public static CurrentUser current() {
        return HOLDER.get();
    }

    /**
     * 当前用户ID，未登录返回 null。
     */
    public static Long currentUserId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    /**
     * 当前用户所属部门ID，未登录返回 null。
     */
    public static Long currentDeptId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.getDeptId();
    }

    /**
     * 当前用户角色集合，未登录返回空集。
     */
    public static Set<String> currentRoles() {
        CurrentUser u = HOLDER.get();
        if (u == null || u.getRoles() == null || u.getRoles().isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(u.getRoles());
    }

    /**
     * 是否为部门负责人。
     */
    public static boolean currentIsDeptLead() {
        CurrentUser u = HOLDER.get();
        return u != null && Boolean.TRUE.equals(u.getDeptLead());
    }

    /**
     * 当前用户是否必须修改初始密码。
     */
    public static boolean currentMustChangePwd() {
        CurrentUser u = HOLDER.get();
        return u != null && Boolean.TRUE.equals(u.getMustChangePassword());
    }

    /**
     * 当前用户主角色是否等于指定角色（不区分大小写比较）。
     *
     * @param role 角色常量 {@link RoleConst}
     * @return true 当且仅当当前已登录且主角色匹配
     */
    public static boolean currentHasPrimaryRole(String role) {
        CurrentUser u = HOLDER.get();
        return u != null && role != null && role.equals(u.getPrimaryRole());
    }

    /**
     * 清理上下文（必须每次请求结束调用）。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 当前用户快照，仅用于在请求线程内传递数据权限相关信息。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentUser {
        private Long userId;
        private Long deptId;
        private String username;
        private String realName;
        /** 单角色：EMP / DEPT_LEAD / LEAD / HR / ADMIN。 */
        private String primaryRole;
        /** Spring Security 风格："ROLE_XXX" 集合（用于 @PreAuthorize）。 */
        private Set<String> roles;
        private Boolean deptLead;
        private Boolean mustChangePassword;

        /**
         * 工厂方法。
         *
         * @param user     当前登录用户实体
         * @param deptLead 是否为部门负责人（可独立于主角色判定）
         * @return 不可变快照
         */
        public static CurrentUser of(com.perfflow.module.system.entity.SysUser user, boolean deptLead) {
            Set<String> roleSet = new HashSet<>(DEFAULT_ROLE_CAPACITY);
            if (user.getRole() != null) {
                roleSet.add("ROLE_" + user.getRole());
            }
            return CurrentUser.builder()
                    .userId(user.getId())
                    .deptId(user.getDeptId())
                    .username(user.getUsername())
                    .realName(user.getRealName())
                    .primaryRole(user.getRole())
                    .roles(roleSet)
                    .deptLead(deptLead)
                    .mustChangePassword(user.getMustChangePassword())
                    .build();
        }
    }
}
