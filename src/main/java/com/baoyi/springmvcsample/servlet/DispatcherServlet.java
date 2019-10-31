package com.baoyi.springmvcsample.servlet;

import com.baoyi.springmvcsample.annotation.Autowired;
import com.baoyi.springmvcsample.annotation.RequestMapping;
import com.baoyi.springmvcsample.annotation.RestController;
import com.baoyi.springmvcsample.annotation.Service;
import com.sun.xml.internal.ws.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: qijigui
 * @CreateDate: 2019/10/30 15:26
 * @Description:
 */
public class DispatcherServlet extends HttpServlet {

    List<String> classNames = new ArrayList<>();
    Map<String, Object> beans = new HashMap<>();
    Map<String, Method> mappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        //1. 加载类
        scanPackage("com.baoyi.springmvcsample");
        //2. 初始化类
        try {
            createInstance(classNames);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        //3. 注入
        try {
            injection();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        createMapping();
    }

    public void scanPackage(String packageName) {
        String path = packageName.replaceAll("\\.", "/");
        URL url = getClass().getClassLoader().getResource("/" + path);
        File[] files = new File(url.getFile()).listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                scanPackage(packageName + "." + file.getName());
            } else {
                classNames.add(packageName + "." + file.getName());
            }
        }
    }

    public void createInstance(List<String> classNames) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        for (String name : this.classNames) {
            String className = name.replaceAll(".class", "");
            Class<?> aClass = Class.forName(className);
            //判断是否是Controller
            if (aClass.isAnnotationPresent(RestController.class)) {
                RequestMapping requestMapping = aClass.getAnnotation(RequestMapping.class);
                String value = requestMapping.value();
                beans.put(value, aClass.newInstance());
            }
            //判断是否是Service注解
            if (aClass.isAnnotationPresent(Service.class)) {
                Service service = aClass.getAnnotation(Service.class);
                String value = service.value();
                if (value == null || value.isEmpty()) {
                    beans.put(aClass.getSimpleName(), aClass.newInstance());
                } else {
                    beans.put(value, aClass.newInstance());
                }
            }
        }
    }

    private void injection() throws IllegalAccessException {
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object instance = entry.getValue();
            Field[] declaredFields = instance.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Autowired autowired = field.getAnnotation(Autowired.class);
                    String value = autowired.value();
                    if (value == null || value.isEmpty()) {
                        value = field.getType().getSimpleName();
                    }
                    field.setAccessible(true);
                    field.set(instance, beans.get(value));
                }
            }
        }
    }

    private void createMapping() {
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object instance = entry.getValue();
            if (instance.getClass().isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = instance.getClass().getAnnotation(RequestMapping.class);
                String value = requestMapping.value();
                Method[] methods = instance.getClass().getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping rm = method.getAnnotation(RequestMapping.class);
                        String val = rm.value();
                        mappings.put(value + val, method);
                    }
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();

        String context = req.getContextPath();

        String path = uri.replaceAll(context, "");
        Method m = mappings.get(path);
        Object instance = beans.get("/" + path.split("/")[1]);
        try {
            m.invoke(instance, null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
