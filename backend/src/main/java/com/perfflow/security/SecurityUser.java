package com.perfflow.security;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.stream.Collectors;
/**
 * Spring Security 用户详情：将登录用户 ID、角色码、部门等信息封装为认证主体，
 * 供权限判断与业务侧通过 SecurityContext 获取当前用户。
 */
@Data
@AllArgsConstructor
public class SecurityUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final Long deptId;
    private final Boolean deptLead;
    private final Boolean mustChangePassword;
    private final Collection<String> roleCodes;

    // 将角色码集合映射为 Spring Security 权限对象，供方法级权限注解判定。
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override public String getPassword() { return password; }

    @Override public String getUsername() { return username; }

    @Override public boolean isAccountNonExpired() { return true; }

    @Override public boolean isAccountNonLocked() { return true; }

    @Override public boolean isCredentialsNonExpired() { return true; }

    @Override public boolean isEnabled() { return enabled; }
}
