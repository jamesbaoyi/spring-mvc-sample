package com.baoyi.springmvcsample.servlet;

import com.baoyi.springmvcsample.annotation.Autowired;
import com.baoyi.springmvcsample.annotation.RequestMapping;
import com.baoyi.springmvcsample.annotation.RestController;
import com.baoyi.springmvcsample.annotation.Service;

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

    //类集合
    List<String> classNames = new ArrayList<>();
    //类初始化集合
    Map<String, Object> beans = new HashMap<>();
    //地址和方法的集合
    Map<String, Method> mappings = new HashMap<>();

    @Override
    public void init() throws ServletException {

        //1. 加载类 将该包下面的所有类加载到List集合里面。
        scanPackage("com.baoyi.springmvcsample");

        //2. 加载的类进行初始化
        try {
            createInstance(classNames);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        //3. 注入 遍历所有的类的示例，然后获取到类实例中的属性及方法，进行初始化
        try {
            injection();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        //4. 生成调用关系
        createMapping();
    }

    public void scanPackage(String packageName) {
        // com/baoyi/springmvcsample
        String path = packageName.replaceAll("\\.", "/");
        //E:\zwproject\spring-mvc-sample\target\classes\com\baoyi\springmvcsample
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
        if (classNames.isEmpty()) {
            return;
        }
        for (String name : this.classNames) {
            //类在加载的时候根据类名，类名是不带.class。
            String className = name.replaceAll(".class", "");
            Class<?> aClass = Class.forName(className);
            //判断是否是RestController
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
                    //如果在Service中没有写value值，我们默认用类型代替
                    beans.put(aClass.getSimpleName(), aClass.newInstance());
                } else {
                    beans.put(value, aClass.newInstance());
                }
            }
        }
    }

    private void injection() throws IllegalAccessException {
        if (beans.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object instance = entry.getValue();
            Field[] declaredFields = instance.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Autowired autowired = field.getAnnotation(Autowired.class);
                    String value = autowired.value();
                    if (value == null || value.isEmpty()) {
                        //在Controller里面注解过来的类没有value的时候默认取类名
                        value = field.getType().getSimpleName();
                    }
                    //打开属性的保护措施
                    field.setAccessible(true);
                    //实例化属性
                    field.set(instance, beans.get(value));
                }
            }
        }
    }

    private void createMapping() {
        //遍历所有的类，取到他们调用方法的路径和对应的方法
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object instance = entry.getValue();
            if (instance.getClass().isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = instance.getClass().getAnnotation(RequestMapping.class);
                //value=/testController
                String value = requestMapping.value();
                Method[] methods = instance.getClass().getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping rm = method.getAnnotation(RequestMapping.class);
                        //val =/index
                        String val = rm.value();
                        mappings.put(value + val, method);
                    }
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //获取请求的地址
        // /spring_mvc_sample/testController/index
        String uri = req.getRequestURI();
        //context = /spring_mvc_sample
        String context = req.getContextPath();
        // /testController/index
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
