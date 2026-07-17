package com.jimuqu.common.log.aspect;

import cn.dev33.satoken.router.SaHttpMethod;
import com.jimuqu.common.core.constant.HttpStatus;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessStatus;
import com.jimuqu.common.log.event.OperLogEvent;
import com.jimuqu.common.satoken.utils.LoginHelper;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.array.ArrayUtil;
import cn.hutool.v7.core.date.StopWatch;
import cn.hutool.v7.core.map.Dict;
import cn.hutool.v7.core.map.MapUtil;
import cn.hutool.v7.core.util.ObjUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.handle.*;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

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
            operLog.setOperUrl(StringUtil.substring(joinPoint.url(), 0, 255));
            LoginUser loginUser = LoginHelper.getLoginUser();
            operLog.setOperName(loginUser.getUsername());
            operLog.setDeptName(loginUser.getDeptName());

            if (e != null) {
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                operLog.setErrorMsg(StringUtil.substring(e.getMessage(), 0, 2000));
            }
            // 设置方法名称
            String className = joinPoint.action().getClass().getName();
            String methodName = joinPoint.action().fullName();
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
            log.error("异常信息:{}", exp.getMessage());
            exp.printStackTrace();
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
                && ObjUtil.isNotNull(jsonResult)) {
            operLog.setJsonResult(StringUtil.substring(JsonUtil.toString(jsonResult), 0, 2000));
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
            String params = sanitizeRequestBody(joinPoint.body(), excludeParamNames);
            operLog.setOperParam(StringUtil.substring(params, 0, 2000));
        } else {
            MapUtil.removeAny(paramsMap, EXCLUDE_PROPERTIES);
            MapUtil.removeAny(paramsMap, excludeParamNames);
            operLog.setOperParam(StringUtil.substring(JsonUtil.toString(paramsMap), 0, 2000));
        }
    }

    static String sanitizeRequestBody(String body, String[] excludeParamNames) {
        if (StringUtil.isBlank(body)) {
            return body;
        }
        try {
            Object value = JsonUtil.toObject(body, Object.class);
            Set<String> excluded = new LinkedHashSet<>(List.of(EXCLUDE_PROPERTIES));
            excluded.addAll(List.of(excludeParamNames));
            removeExcludedFields(value, excluded);
            return JsonUtil.toString(value);
        } catch (RuntimeException ignored) {
            return body;
        }
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
            if (ObjUtil.isNotNull(o) && !isFilterObject(o)) {
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
    @SuppressWarnings("rawtypes" )
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(UploadedFile.class);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection collection = (Collection) o;
            for (Object value : collection) {
                return value instanceof UploadedFile;
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map map = (Map) o;
            for (Object value : map.values()) {
                return value instanceof UploadedFile;
            }
        }
        return o instanceof UploadedFile;
    }
}
