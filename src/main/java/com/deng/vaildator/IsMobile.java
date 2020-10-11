package com.deng.vaildator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**3
 * 手机号码的校检注解
 */
@Target({ElementType.METHOD,ElementType.FIELD,ElementType.ANNOTATION_TYPE,ElementType.CONSTRUCTOR,
ElementType.PARAMETER,ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {IsMobileValidator.class})// 这个注解的参数指定用于校验工作的是哪个类
public @interface IsMobile {
    //默认手机号码不可为空
    boolean required() default true;
    //如果校检不通过时提示信息
    String message() default "手机号码格式错误";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default {};
}
