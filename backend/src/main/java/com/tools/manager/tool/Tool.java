package com.tools.manager.tool;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 工具注解，标注在工具实现类上。
 * 包含 @Component 元注解，使实现类会被 Spring 自动扫描为 Bean。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Tool {

    String id();

    String name();

    String description() default "";

    String icon() default "";

    String category() default "default";

    boolean needBackend() default false;

}
