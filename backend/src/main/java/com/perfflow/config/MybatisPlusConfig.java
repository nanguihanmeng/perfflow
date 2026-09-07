package com.perfflow.config;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDateTime;
/**
 * MyBatis-Plus 配置：注册分页插件（限制单次查询最大返回行数）与公共字段（创建/更新时间）自动填充处理器。
 */
@Configuration
public class MybatisPlusConfig {

    private static final long MAX_PAGE_SIZE = 200L;
    /**
     * 分页插件（MySQL 方言）：对分页查询强制生效单页行数上限，避免一次拉取全表数据。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(MAX_PAGE_SIZE);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    /**
     * 公共字段自动填充：新增记录写入 createdAt/updatedAt，更新记录仅刷新 updatedAt。
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {

        return new MetaObjectHandler() {
            // 新增记录：填充创建时间与更新时间。
            @Override
            public void insertFill(MetaObject metaObject) {

                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
            }

            // 更新记录：仅刷新更新时间。
            @Override
            public void updateFill(MetaObject metaObject) {

                strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
