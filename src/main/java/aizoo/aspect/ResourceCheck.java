package aizoo.aspect;

import aizoo.common.ResourceType;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface ResourceCheck {
    ResourceType[] resourceTypes() default {};
}
