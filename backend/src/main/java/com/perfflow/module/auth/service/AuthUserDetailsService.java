package com.perfflow.module.auth.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysUserMapper;
import com.perfflow.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
// Spring Security 用户加载服务。
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;
    // 按用户名加载用户。
    @Override
    // 按用户名加载用户。

    // 执行业务处理
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 查询单条
        SysUser u = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));

        // 判空处理
        if (u == null) {

            log.warn("登录失败：用户不存在 username={}", username);
            throw new UsernameNotFoundException("user not found: " + username);
        }

        // 构建集合容器
        List<SimpleGrantedAuthority> auths = new ArrayList<>();

        // 非空才处理
        if (u.getRole() != null) {

            auths.add(new SimpleGrantedAuthority("ROLE_" + u.getRole()));
        }

        return new SecurityUser(
                u.getId(), u.getUsername(), u.getPassword(), u.getStatus() != null && u.getStatus() == 1,
                u.getDeptId(), u.getDeptLead(), u.getMustChangePassword(),
                // 集合流处理
                auths.stream().map(SimpleGrantedAuthority::getAuthority).toList()
        );
    }
}
