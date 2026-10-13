package com.ylh.picturego.aop;

import com.ylh.picturego.annotation.AuthCheck;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.enums.UserRoleEnum;
import com.ylh.picturego.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 获取请求对象
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = null;
        if (requestAttributes != null) {
            request = ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwException(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 判断是否需要权限
        UserRoleEnum mustRole = UserRoleEnum.getEnumByValue(authCheck.mustRole());
        // 无需权限，直接放行
        if (mustRole == null) {
            return joinPoint.proceed();
        }
        // 需要权限，判断角色
        UserRoleEnum userRole = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        ThrowUtils.throwException(userRole == null || userRole.getLevel() < mustRole.getLevel(), ErrorCode.NO_AUTH_ERROR);

        return joinPoint.proceed();
    }
}
