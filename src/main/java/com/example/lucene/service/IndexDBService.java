package com.example.lucene.service;

import com.example.lucene.model.User;
import com.example.lucene.utils.LuceneUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class IndexDBService {

    /**
     * 创建索引库
     *
     * @param object 原始的javabean
     * @throws Exception
     */
    public void createIndexDB(Object object) throws Exception {
        IndexWriter indexWriter = null;
        try {
            Document document = LuceneUtils.javaBean2Document(object);
            /**
             * IndexWriter将我们的document对象写到硬盘中
             *
             * 参数一：Directory d,写到硬盘中的目录路径是什么
             * 参数二：Analyzer a, 以何种算法来对document中的原始记录表数据进行拆分成词汇表
             * 参数三：MaxFieldLength mfl 最多将文本拆分出多少个词汇
             */
            indexWriter = new IndexWriter(LuceneUtils.getDirectory(), LuceneUtils.getAnalyzer(), LuceneUtils.getMaxFieldLength());
            //将Document对象通过IndexWriter对象写入索引库中
            indexWriter.addDocument(document);
        } finally {
            //关闭IndexWriter对象
            indexWriter.close();
        }
    }

    /**
     * 搜索
     *
     * @param keyWords 要查询的关键字
     * @param field    查询的词汇
     * @param count    查询结果数量
     * @param clazz    转换成的javabean
     * @return 返回查询结果
     * @throws Exception
     */
    public <T> List<T> findIndexDB(String keyWords, String field, int count, Class<T> clazz) throws Exception {
        List<T> list = new ArrayList<>();
        //创建IndexSearcher对象
        IndexSearcher indexSearcher = new IndexSearcher(LuceneUtils.getDirectory());
        //创建QueryParser对象
        QueryParser queryParser = new QueryParser(Version.LUCENE_30, field, LuceneUtils.getAnalyzer());
        //创建Query对象来封装关键字
        Query query = queryParser.parse(keyWords);
        //用IndexSearcher对象去索引库中查询符合条件的前count条记录，不足count条记录的以实际为准
        TopDocs topDocs = indexSearcher.search(query, count);
        //获取符合条件的编号
        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = topDocs.scoreDocs[i];
            int no = scoreDoc.doc;
            //用indexSearcher对象去索引库中查询编号对应的Document对象
            Document document = indexSearcher.doc(no);
            //将Document对象中的所有属性取出，再封装回JavaBean对象中去
            T t = (T) LuceneUtils.Document2JavaBean(document, clazz);
            list.add(t);
        }
        return list;
    }

    @Test
    public void testCreateIndexDB() throws Exception {
        //把数据填充到JavaBean对象中
        for (int i = 0; i < 1000; i++) {
            User user = new User(i + "", "钟福成" + i, "未来的程序员" + i);
            createIndexDB(user);
        }
    }

    @Test
    public void testFindIndexDB() throws Exception {
        List<User> list = findIndexDB("钟", "userName", 100, User.class);
        list.forEach(System.out::println);
    }
}
