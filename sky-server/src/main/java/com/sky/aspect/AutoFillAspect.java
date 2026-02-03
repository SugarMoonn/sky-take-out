package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

import static com.sky.constant.AutoFillConstant.*;

/**
 * 切面类：实现功能字段的自动填充
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点
     */
    @Pointcut("execution( * com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {

    }

    /**
     * 前置通知：在切入点方法执行之前执行，在通知中进行公共字段的复制
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段自动填充...");
        //获取当前被拦截的方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        OperationType operationType = signature.getMethod().getAnnotation(AutoFill.class).value();

        //获取到当前被拦截的方法的参数对象
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0){
            return;
        }

        Object entity = args[0];
        //准备赋值的数据

        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();


        //根据当前不同的操作类型，进行不同的公共字段赋值
        if(operationType == OperationType.INSERT){
            try{
                //使用反射机制，动态调用实体类的set方法，进行赋值
                entity.getClass().getMethod(SET_CREATE_TIME, LocalDateTime.class).invoke(entity, now);
                entity.getClass().getMethod(SET_UPDATE_TIME, LocalDateTime.class).invoke(entity, now);
                entity.getClass().getMethod(SET_CREATE_USER, Long.class).invoke(entity, currentId);
                entity.getClass().getMethod(SET_UPDATE_USER, Long.class).invoke(entity, currentId);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }


        } else if (operationType == OperationType.UPDATE) {
            try {
                log.info("开始为 UPDATE 操作填充公共字段，实体类: {}", entity.getClass().getSimpleName());
                log.info("当前用户ID: {}, 当前时间: {}", currentId, now);

                // 尝试设置 updateTime
                try {
                    entity.getClass().getMethod(SET_UPDATE_TIME, LocalDateTime.class)
                            .invoke(entity, now);
                    log.info("成功设置 updateTime");
                } catch (NoSuchMethodException e) {
                    log.error("找不到 setUpdateTime 方法！方法名: {}, 实体: {}", SET_UPDATE_TIME, entity.getClass().getName());
                    // 可以选择继续执行其他字段，或者直接抛出
                    // 这里选择继续执行其他字段（更宽容）
                } catch (Exception e) {
                    log.error("设置 updateTime 失败", e);
                }

                // 尝试设置 updateUser
                try {
                    entity.getClass().getMethod(SET_UPDATE_USER, Long.class)
                            .invoke(entity, currentId);
                    log.info("成功设置 updateUser");
                } catch (NoSuchMethodException e) {
                    log.error("找不到 setUpdateUser 方法！方法名: {}, 实体: {}", SET_UPDATE_USER, entity.getClass().getName());
                } catch (Exception e) {
                    log.error("设置 updateUser 失败", e);
                }

                // 如果你希望“只要有一个字段设置失败就抛异常”，可以在最后加判断
                // 或者保持现状：部分成功也继续执行 mapper.update

            } catch (Exception unexpected) {
                // 捕获所有未预料的异常（比如 invoke 时的其他问题）
                log.error("UPDATE 公共字段填充发生未预期异常", unexpected);
                throw new RuntimeException("UPDATE 操作公共字段自动填充失败", unexpected);
            }
        }

    }

}