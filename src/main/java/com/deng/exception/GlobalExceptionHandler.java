package com.deng.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.xml.transform.Result;

@ControllerAdvice //通过Advice可知，这个处理器实际上是个切面
@ResponseBody
public class GlobalExceptionHandler {

}
