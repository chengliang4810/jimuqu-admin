package com.jimuqu.common.mybatis.expression;

import cn.hutool.v7.core.text.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Solon兼容的表达式解析器
 * <p>
 * 替代Spring SpEL，提供简单的表达式解析功能
 * 支持变量替换和方法调用
 *
 * @author chengliang4810
 * @version 1.0
 */
@Slf4j
public class SolonExpressionParser {

    /**
     * 表达式模式：#{variable} 或 #{@bean.method()}
     */
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("#\\{([^}]+)\\}");

    /**
     * 方法调用模式：@bean.method(args)
     */
    private static final Pattern METHOD_CALL_PATTERN = Pattern.compile("@([^.]+)\\.([^\\(]+)\\(([^)]*)\\)");

    /**
     * 变量模式：#variable
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("#([a-zA-Z0-9_]+)");

    /**
     * 解析表达式
     *
     * @param template  表达式模板
     * @param context  上下文变量
     * @param beanResolver Bean解析器
     * @return 解析后的字符串
     */
    public static String parse(String template, Map<String, Object> context, BeanResolver beanResolver) {
        if (StrUtil.isBlank(template)) {
            return template;
        }

        Matcher matcher = EXPRESSION_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String expression = matcher.group(1);
            String replacement = evaluateExpression(expression, context, beanResolver);
            matcher.appendReplacement(result, replacement);
        }

        matcher.appendTail(result);
        return result.toString().trim();
    }

    /**
     * 评估单个表达式
     *
     * @param expression 表达式
     * @param context 上下文变量
     * @param beanResolver Bean解析器
     * @return 评估结果
     */
    private static String evaluateExpression(String expression, Map<String, Object> context, BeanResolver beanResolver) {
        expression = expression.trim();

        // 处理方法调用：@bean.method(args)
        Matcher methodMatcher = METHOD_CALL_PATTERN.matcher(expression);
        if (methodMatcher.matches()) {
            return evaluateMethodCall(methodMatcher, beanResolver);
        }

        // 处理变量：#variable
        Matcher variableMatcher = VARIABLE_PATTERN.matcher(expression);
        if (variableMatcher.matches()) {
            return evaluateVariable(variableMatcher.group(1), context);
        }

        // 直接返回表达式（可能是字面量）
        return expression;
    }

    /**
     * 评估方法调用
     *
     * @param matcher 方法匹配器
     * @param beanResolver Bean解析器
     * @return 方法调用结果
     */
    private static String evaluateMethodCall(Matcher matcher, BeanResolver beanResolver) {
        String beanName = matcher.group(1);
        String methodName = matcher.group(2);
        String argsStr = matcher.group(3);

        try {
            // 获取Bean
            Object bean = beanResolver.getBean(beanName);
            if (bean == null) {
                log.warn("未找到Bean: {}", beanName);
                return "";
            }

            // 解析参数
            Object[] args = parseArguments(argsStr, beanResolver);

            // 调用方法
            Object result = invokeMethod(bean, methodName, args);
            return convertToString(result);

        } catch (Exception e) {
            log.error("调用方法失败: {}.{}({})", beanName, methodName, argsStr, e);
            return "";
        }
    }

    /**
     * 评估变量
     *
     * @param variableName 变量名
     * @param context 上下文
     * @return 变量值
     */
    private static String evaluateVariable(String variableName, Map<String, Object> context) {
        Object value = context.get(variableName);
        return convertToString(value);
    }

    /**
     * 解析方法参数
     *
     * @param argsStr 参数字符串
     * @param beanResolver Bean解析器
     * @return 参数数组
     */
    private static Object[] parseArguments(String argsStr, BeanResolver beanResolver) {
        if (StrUtil.isBlank(argsStr)) {
            return new Object[0];
        }

        String[] argStrs = argsStr.split(",");
        Object[] args = new Object[argStrs.length];

        for (int i = 0; i < argStrs.length; i++) {
            String arg = argStrs[i].trim();
            args[i] = parseArgument(arg, beanResolver);
        }

        return args;
    }

    /**
     * 解析单个参数
     *
     * @param argStr 参数字符串
     * @param beanResolver Bean解析器
     * @return 参数值
     */
    private static Object parseArgument(String argStr, BeanResolver beanResolver) {
        argStr = argStr.trim();

        // 处理空字符串
        if (argStr.isEmpty()) {
            return null;
        }

        // 处理字符串
        if (argStr.startsWith("\"") && argStr.endsWith("\"")) {
            return argStr.substring(1, argStr.length() - 1);
        }

        // 处理数字
        try {
            return Long.parseLong(argStr);
        } catch (NumberFormatException e) {
            // 不是数字，继续处理
        }

        // 处理变量
        if (argStr.startsWith("#")) {
            // 这里简化处理，实际应该从上下文中获取
            // 目前返回null，在方法调用时需要处理
            return null;
        }

        // 处理布尔值
        if ("true".equalsIgnoreCase(argStr)) {
            return true;
        }
        if ("false".equalsIgnoreCase(argStr)) {
            return false;
        }

        // 其他情况保持原样
        return argStr;
    }

    /**
     * 调用方法
     *
     * @param bean 目标对象
     * @param methodName 方法名
     * @param args 参数
     * @return 方法结果
     */
    private static Object invokeMethod(Object bean, String methodName, Object[] args) throws Exception {
        java.lang.reflect.Method[] methods = bean.getClass().getMethods();

        for (java.lang.reflect.Method method : methods) {
            if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                return method.invoke(bean, args);
            }
        }

        throw new NoSuchMethodException("未找到方法: " + methodName);
    }

    /**
     * 转换为字符串
     *
     * @param value 值
     * @return 字符串
     */
    private static String convertToString(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof Iterable) {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(item);
                first = false;
            }
            sb.append(")");
            return sb.toString();
        }

        return String.valueOf(value);
    }

    /**
     * Bean解析器接口
     */
    public interface BeanResolver {
        Object getBean(String name);
    }

}