package com.example.lucene.utils;

import com.example.lucene.model.User;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 使用单例模式
 */
public class LuceneUtils {
    private static Directory directory;
    private static Directory ramDirectory;
    private static Analyzer analyzer;
    private static IndexWriter.MaxFieldLength maxFieldLength;

    private LuceneUtils() {
    }

    static {
        try {
            /**如果目录不存在，则会自动创建
             * FSDirectory：表示文件系统目录，即会存储在计算机本地磁盘，继承于
             * org.apache.lucene.store.BaseDirectory
             * 同理还有：org.apache.lucene.store.RAMDirectory：存储在内存中
             * Lucene 7.4.0 版本 open 方法传入的 Path 对象
             * Lucene 4.10.3 版本 open 方法传入的是 File 对象
             */
            directory = FSDirectory.open(new File("E:/createIndexDB"));
            /**
             * 构建内存索引库
             */
            ramDirectory = new RAMDirectory(directory);
            /** 创建分词器
             * StandardAnalyzer：标准分词器，对英文分词效果很好，对中文是单字分词，即一个汉字作为一个词，所以对中文支持不足
             * 市面上有很多好用的中文分词器，如 IKAnalyzer 就是其中一个
             * 现在换成 IKAnalyzer 中文分词器
             */
//            analyzer = new StandardAnalyzer(Version.LUCENE_30);
            analyzer = new IKAnalyzer();
            maxFieldLength = IndexWriter.MaxFieldLength.LIMITED;
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public static Directory getDirectory() {
        return directory;
    }

    public static Directory getRamDirectory() {
        return ramDirectory;
    }

    public static Analyzer getAnalyzer() {
        return analyzer;
    }

    public static IndexWriter.MaxFieldLength getMaxFieldLength() {
        return maxFieldLength;
    }

    /**
     * javaBean2Document
     *
     * @param object 传入的JavaBean类型
     * @return 返回Document对象
     */
    public static Document javaBean2Document(Object object) {
        try {
            Document document = new Document();
            //得到JavaBean的字节码文件对象
            Class<?> aClass = object.getClass();
            //通过字节码文件对象得到对应的属性【全部的属性，不能仅仅调用getFields()】
            Field[] fields = aClass.getDeclaredFields();
            //得到每个属性的名字
            for (Field field : fields) {
                String name = field.getName();
                //得到属性的值【也就是调用getter方法获取对应的值】
                String method = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
                //得到对应的值【就是得到具体的方法，然后调用就行了。因为是get方法，没有参数】
                Method aClassMethod = aClass.getDeclaredMethod(method, null);
                String value = StringUtils.trimToEmpty(aClassMethod.invoke(object) + "");
                //把数据封装到Document对象中。
                document.add(new org.apache.lucene.document.Field(name, value, org.apache.lucene.document.Field.Store.YES, org.apache.lucene.document.Field.Index.ANALYZED));
            }
            return document;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Document2JavaBean
     *
     * @param aClass   要解析的对象类型，要用户传入进来
     * @param document 将Document对象传入进来
     * @return 返回一个JavaBean
     */
    public static Object Document2JavaBean(Document document, Class aClass) {
        try {
            //创建该JavaBean对象
            Object obj = aClass.newInstance();
            //得到该JavaBean所有的成员变量
            Field[] fields = aClass.getDeclaredFields();
            for (Field field : fields) {
                //设置允许暴力访问
                field.setAccessible(true);
                String name = field.getName();
                String value = document.get(name);
                //使用BeanUtils把数据封装到Bean中
                BeanUtils.setProperty(obj, name, value);
            }
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        User user = new User();
        System.out.println(LuceneUtils.javaBean2Document(user));
    }
}
