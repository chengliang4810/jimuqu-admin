package com.jimuqu.common.log.aspect;

import cn.dev33.satoken.router.SaHttpMethod;
import com.jimuqu.common.core.constant.HttpStatus;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.sensitive.utils.SensitiveUtil;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessStatus;
import com.jimuqu.common.log.event.OperLogEvent;
import com.jimuqu.common.satoken.utils.LoginHelper;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Body;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.handle.*;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

import java.lang.reflect.Array;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 操作日志记录处理
 *
 * @author Lion Li,chengliang4810
 */
@Slf4j
@Component(index = -99)
public class LogAspect implements RouterInterceptor {

    private static final int MAX_URL_LENGTH = 255;
    private static final int MAX_CLIENT_KEY_LENGTH = 32;
    private static final int MAX_CONTENT_LENGTH = 3800;

    /**
     * 排除敏感属性字段
     */
    public static final String[] EXCLUDE_PROPERTIES = {"password", "oldPassword", "newPassword", "confirmPassword", "authorization"};


    /**
     * 计算操作消耗时间
     */
    private static final ThreadLocal<StopWatch> TIME_THREADLOCAL = new ThreadLocal<>();


    @Override
    public void doIntercept(Context ctx, Handler mainHandler, RouterInterceptorChain chain) throws Throwable {

        if (mainHandler == null){
            log.error("Resource Not Found : {}", ctx.path());
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        Log anno = null;
        Action action = ctx.action();
        if (action != null) {
            anno = action.method().getAnnotation(Log.class);
        }

        if (anno == null) {
            //如果没有注解
            chain.doIntercept(ctx, mainHandler);
        } else {
            //1.开始计时
            StopWatch stopWatch = new StopWatch();
            TIME_THREADLOCAL.set(stopWatch);
            stopWatch.start();
            try {
                chain.doIntercept(ctx, mainHandler);
                // 执行成功
                handleLog(ctx, anno, null, ctx.result);
            } catch (Exception e) {
                // 执行错误
                handleLog(ctx, anno, e, null);
                throw e;
            }
        }
    }

    protected void handleLog(final Context joinPoint, Log controllerLog, final Exception e, Object jsonResult) {
        try {

            // *========数据库日志=========*//
            OperLogEvent operLog = new OperLogEvent();
            operLog.setStatus(BusinessStatus.SUCCESS.ordinal());
            // 请求的地址
            String ip = joinPoint.realIp();
            operLog.setOperIp(ip);
            operLog.setOperUrl(StringUtil.substring(joinPoint.path(), 0, MAX_URL_LENGTH));
            LoginUser loginUser = LoginHelper.getLoginUser();
            snapshotOperator(operLog, loginUser, joinPoint.header(LoginHelper.CLIENT_KEY));

            if (e != null) {
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                operLog.setErrorMsg(StringUtil.substring(e.getMessage(), 0, MAX_CONTENT_LENGTH));
            }
            // 设置方法名称
            String className = joinPoint.action().controller().clz().getName();
            String methodName = joinPoint.action().method().getName();
            operLog.setMethod(className + "." + methodName + "()" );
            // 设置请求方式
            operLog.setRequestMethod(joinPoint.method());
            // 处理设置注解上的参数
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);
            // 设置消耗时间
            StopWatch stopWatch = TIME_THREADLOCAL.get();
            stopWatch.stop();
            operLog.setCostTime(stopWatch.getTotalTimeMillis());
            // 发布事件保存数据库
            EventBus.publish(operLog);
        } catch (Exception exp) {
            // 记录本地异常日志
            log.error("记录操作日志异常", exp);
        } finally {
            TIME_THREADLOCAL.remove();
        }
    }

    /**
     * 获取注解中对方法的描述信息 用于Controller层注解
     *
     * @param log     日志
     * @param operLog 操作日志
     * @throws Exception
     */
    public void getControllerMethodDescription(Context joinPoint, Log log, OperLogEvent operLog, Object jsonResult) throws Exception {
        // 设置action动作
        operLog.setBusinessType(log.businessType().ordinal());
        // 设置标题
        operLog.setTitle(log.title());
        // 设置操作人类别
        operLog.setOperatorType(log.operatorType().ordinal());
        // 是否需要保存request，参数和值
        if (log.isSaveRequestData()) {
            // 获取参数的信息，传入到数据库中。
            setRequestValue(joinPoint, operLog, log.excludeParamNames());
        }
        // 是否需要保存response，参数和值
        if (!(jsonResult instanceof DownloadedFile)
                && !(jsonResult instanceof Throwable)
                && log.isSaveResponseData()
                && ObjectUtil.isNotNull(jsonResult)) {
            operLog.setJsonResult(StringUtil.substring(JsonUtil.toString(jsonResult), 0, MAX_CONTENT_LENGTH));
        }
    }

    /**
     * 获取请求的参数，放到log中
     *
     * @param operLog 操作日志
     * @throws Exception 异常
     */
    private void setRequestValue(Context joinPoint, OperLogEvent operLog, String[] excludeParamNames) throws Exception {
        Map<String, List<String>> paramsMap = joinPoint.paramMap().toValuesMap();
        String requestMethod = operLog.getRequestMethod();
        if (MapUtil.isEmpty(paramsMap) && (SaHttpMethod.PUT.name().equals(requestMethod)
                || SaHttpMethod.POST.name().equals(requestMethod)
                || SaHttpMethod.DELETE.name().equals(requestMethod))) {
            String params = sanitizeRequestBody(joinPoint.body(), requestBodyType(joinPoint), excludeParamNames);
            operLog.setOperParam(StringUtil.substring(params, 0, MAX_CONTENT_LENGTH));
        } else {
            MapUtil.removeAny(paramsMap, EXCLUDE_PROPERTIES);
            MapUtil.removeAny(paramsMap, excludeParamNames);
            operLog.setOperParam(StringUtil.substring(JsonUtil.toString(paramsMap), 0, MAX_CONTENT_LENGTH));
        }
    }

    static void snapshotOperator(OperLogEvent operLog, LoginUser loginUser, String requestClientKey) {
        operLog.setClientKey(StringUtil.substring(requestClientKey, 0, MAX_CLIENT_KEY_LENGTH));
        if (loginUser == null) {
            return;
        }
        operLog.setOperName(loginUser.getUsername());
        operLog.setUserId(loginUser.getUserId());
        operLog.setDeptId(loginUser.getDeptId());
        operLog.setDeptName(loginUser.getDeptName());
        operLog.setDeviceType(loginUser.getDeviceType());
        operLog.setBrowser(loginUser.getBrowser());
        operLog.setOs(loginUser.getOs());
        if (StringUtil.isBlank(operLog.getClientKey())) {
            operLog.setClientKey(loginUser.getClientKey());
        }
    }

    static String sanitizeRequestBody(String body, String[] excludeParamNames) {
        return sanitizeRequestBody(body, Object.class, excludeParamNames);
    }

    static String sanitizeRequestBody(String body, Type bodyType, String[] excludeParamNames) {
        if (StringUtil.isBlank(body)) {
            return body;
        }
        try {
            Object value = JsonUtil.toObject(body, bodyType == null ? Object.class : bodyType);
            value = SensitiveUtil.desensitizeObject(value);
            Set<String> excluded = new LinkedHashSet<>(List.of(EXCLUDE_PROPERTIES));
            excluded.addAll(List.of(excludeParamNames));
            removeExcludedFields(value, excluded);
            return JsonUtil.toString(value);
        } catch (RuntimeException ignored) {
            return "[请求体无法解析]";
        }
    }

    private static Type requestBodyType(Context context) {
        if (context.action() == null) {
            return Object.class;
        }
        for (Parameter parameter : context.action().method().getParameters()) {
            if (parameter.isAnnotationPresent(Body.class)) {
                return parameter.getParameterizedType();
            }
        }
        return Object.class;
    }

    @SuppressWarnings("unchecked")
    private static void removeExcludedFields(Object value, Set<String> excluded) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> values = (Map<String, Object>) map;
            values.keySet().removeIf(excluded::contains);
            values.values().forEach(item -> removeExcludedFields(item, excluded));
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(item -> removeExcludedFields(item, excluded));
        }
    }

    /**
     * 参数拼装
     */
    private String argsArrayToString(Collection<Object> paramsArray, String[] excludeParamNames) {
        StringJoiner params = new StringJoiner(" ");
        if (ArrayUtil.isEmpty(paramsArray)) {
            return params.toString();
        }
        for (Object o : paramsArray) {
            if (ObjectUtil.isNotNull(o) && !isFilterObject(o)) {
                String str = JsonUtil.toString(o);
                Dict dict = JsonUtil.toMap(str);
                if (MapUtil.isNotEmpty(dict)) {
                    MapUtil.removeAny(dict, EXCLUDE_PROPERTIES);
                    MapUtil.removeAny(dict, excludeParamNames);
                    str = JsonUtil.toString(dict);
                }
                params.add(str);
            }
        }
        return params.toString();
    }

    /**
     * 判断是否需要过滤的对象。
     *
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            int length = Array.getLength(o);
            for (int i = 0; i < length; i++) {
                if (isFilterValue(Array.get(o, i))) {
                    return true;
                }
            }
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection<?> collection = (Collection<?>) o;
            for (Object value : collection) {
                if (isFilterValue(value)) {
                    return true;
                }
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map<?, ?> map = (Map<?, ?>) o;
            for (Object value : map.values()) {
                if (isFilterValue(value)) {
                    return true;
                }
            }
        }
        return isFilterValue(o);
    }

    private boolean isFilterValue(Object value) {
        return value instanceof UploadedFile || value instanceof Context;
    }
}
