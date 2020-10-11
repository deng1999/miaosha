package com.deng.vaildator;

import com.deng.until.ValidatorUtil;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;

public class IsMobileValidator implements ConstraintValidator<IsMobile,String> {
    //用于获取校检字段是否可以为空
    private boolean required=false;

    /**
     * 用于获取注解
     * @param constraintAnnotation
     */

    public void initialize(IsMobile constraintAnnotation) {
           required=constraintAnnotation.required();
    }

    /**
     * 用于校检字段是否合法
     *
     * @param value 带校检的字段
     * @param constraintValidatorContext
     * @return 字段检验结果
     */

    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
       //如果所检验字段可以为空

        if (required){
            return ValidatorUtil.isMobile(value);//检验结果
        }else {
            if (StringUtils.isEmpty(value))
                return true;
            else
                return ValidatorUtil.isMobile(value);//检验结果
        }
    }


}
