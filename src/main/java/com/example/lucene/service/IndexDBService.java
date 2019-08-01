package com.example.lucene.service;

import com.example.lucene.model.User;
import com.example.lucene.utils.LuceneUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Scorer;
import org.apache.lucene.search.highlight.*;
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
        IndexWriter ramIndexWriter = null;
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

            /**
             * 索引库优化
             */
            //索引库优化
            indexWriter.optimize();
            //设置合并因子为3，每当有3个cfs文件，就合并
            indexWriter.setMergeFactor(3);

            /**
             * 构建内存索引库
             */
            ramIndexWriter = new IndexWriter(LuceneUtils.getRamDirectory(), LuceneUtils.getAnalyzer(), LuceneUtils.getMaxFieldLength());
            ramIndexWriter.addDocument(document);
        } finally {
            //关闭内存IndexWriter对象
            ramIndexWriter.close();
            // 合并所有分段至内存索引库
            indexWriter.addIndexesNoOptimize(LuceneUtils.getRamDirectory());

            //关闭IndexWriter对象
            indexWriter.close();
        }
    }

    /**
     * 搜索
     *
     * @param keyWords 要查询的关键字
     * @param fields   查询的词汇
     * @param count    查询结果数量
     * @param clazz    转换成的javabean
     * @return 返回查询结果
     * @throws Exception
     */
    public <T> List<T> findIndexDB(String keyWords, String[] fields, int count, Class<T> clazz) throws Exception {
        List<T> list = new ArrayList<>();
        //创建IndexSearcher对象
        IndexSearcher indexSearcher = new IndexSearcher(LuceneUtils.getDirectory());
        //创建QueryParser对象
//        QueryParser queryParser = new QueryParser(Version.LUCENE_30, fields[0], LuceneUtils.getAnalyzer());
        // 多条件搜索可以使用我们最大限度匹配对应的数据！
        QueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_30, fields, LuceneUtils.getAnalyzer());
        //创建Query对象来封装关键字
        Query query = queryParser.parse(keyWords);

        //true表示降序
        Sort sort = new Sort(new SortField("id", SortField.INT, true));
        // 多个字段排序：在多字段排序中，只有第一个字段排序结果相同时，第二个字段排序才有作用 提倡用数值型排序
//        Sort sort = new Sort(new SortField("count",SortField.INT,true),new SortField("id",SortField.INT,true));
        //用IndexSearcher对象去索引库中查询符合条件的前count条记录，不足count条记录的以实际为准
        TopDocs topDocs = indexSearcher.search(query, null, count, sort);

        //设置关键字高亮
        Formatter formatter = new SimpleHTMLFormatter("<font color='red'>", "</font>");
        Scorer scorer = new QueryScorer(query);
        Highlighter highlighter = new Highlighter(formatter, scorer);

        //设置摘要(搜索结果摘要需要与设置高亮一起使用),每个关键字片段的字符数大小为4
        Fragmenter fragmenter = new SimpleFragmenter(4);
        highlighter.setTextFragmenter(fragmenter);

        //获取符合条件的编号
        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = topDocs.scoreDocs[i];
            int no = scoreDoc.doc;
            //用indexSearcher对象去索引库中查询编号对应的Document对象
            Document document = indexSearcher.doc(no);

            //为结果设置默认相关度,默认1.0
            document.setBoost(i * 1.0F);

            //设置姓名高亮
            String highlighterContent = highlighter.getBestFragment(LuceneUtils.getAnalyzer(), "userName", document.get("userName"));
            document.getField("userName").setValue(highlighterContent);

            //将Document对象中的所有属性取出，再封装回JavaBean对象中去
            T t = (T) LuceneUtils.Document2JavaBean(document, clazz);
            list.add(t);
        }
        return list;
    }

    @Test
    public void testCreateIndexDB() throws Exception {
        //把数据填充到JavaBean对象中
        User user1 = new User("1", "鲁班七号", "不得不承认，有时候肌肉比头脑管用");
        User user2 = new User("2", "成吉思汗", "雄鹰不畏暴风吹 狼群不为长夜畏惧");
        User user3 = new User("3", "公孙离", "一舞剑气动四方");
        User user4 = new User("4", "狄仁杰", "真相只有一个。");
        User user5 = new User("5", "关羽", "把眼光，从二爷的绿帽子上移开");
        User user6 = new User("6", "钟无艳", "俗说说得好，有钱男子汉，无钱汉子难");
        User user7 = new User("7", "杨戬", "刀锋所划之地 便是疆土");
        User user8 = new User("8", "花木兰", "谁说女子不如男");
        User user9 = new User("9", "王昭君", "美貌是种罪孽，暴雪也无法掩埋。");
        User user10 = new User("10", "甄姬", "果然，先爱上的那个人，是输家");
        User user11 = new User("11", "貂蝉", "这么直白的盯着妾身，好羞涩哦");
        User user12 = new User("12", "上官婉儿", "笔落兴亡定三端之妙，墨写清白尽六艺之奥");
        User user13 = new User("13", "孙膑", "失去双脚，得到穿越时间的流量，这就是等价交换。");
        User user14 = new User("14", "牛魔", "牛气冲天，纯爷们");
        User user15 = new User("15", "大乔", "潮水中，沉默着被遗忘的名字，他们隶属于自作多情的泡沫！");
        User user16 = new User("16", "姜子牙", "不刷新世界观怎么可能成长");
        User user17 = new User("17", "白起", "最犀利的剑只为最强大的手所挥动");
        User user18 = new User("18", "东皇太一", "舍弃怜悯，会让你蜕变成冷血的蜈蚣，丑陋而又强大。");
        User user19 = new User("19", "项羽", "天不容我，我必逆天");
        User user20 = new User("20", "庄周", "死亡，美妙的长眠，值得高歌一曲，啦～～～");
        createIndexDB(user1);
        createIndexDB(user2);
        createIndexDB(user3);
        createIndexDB(user4);
        createIndexDB(user5);
        createIndexDB(user6);
        createIndexDB(user7);
        createIndexDB(user8);
        createIndexDB(user9);
        createIndexDB(user10);
        createIndexDB(user11);
        createIndexDB(user12);
        createIndexDB(user13);
        createIndexDB(user14);
        createIndexDB(user15);
        createIndexDB(user16);
        createIndexDB(user17);
        createIndexDB(user18);
        createIndexDB(user19);
        createIndexDB(user20);
    }

    @Test
    public void testFindIndexDB() throws Exception {
//        List<User> list = findIndexDB("1", new String[]{"id"}, 10, User.class);
//        list.forEach(System.out::println);
//        System.out.println("---------------------------------------------------------------------");
        List<User> list1 = findIndexDB("钟", new String[]{"userName"}, 10, User.class);
        list1.forEach(System.out::println);
//        System.out.println("---------------------------------------------------------------------");
//        List<User> list2 = findIndexDB("未来", new String[]{"sal"}, 10, User.class);
//        list2.forEach(System.out::println);
//        System.out.println("---------------------------------------------------------------------");
//        List<User> list3 = findIndexDB("未", new String[]{"sal"}, 10, User.class);
//        list3.forEach(System.out::println);
//        System.out.println("---------------------------------------------------------------------");
//        List<User> list4 = findIndexDB("未", new String[]{"userName", "sal"}, 10, User.class);
//        list4.forEach(System.out::println);
        System.out.println("---------------------------------------------------------------------");
        List<User> list5 = findIndexDB("肌肉", new String[]{"sal"}, 10, User.class);
        list5.forEach(System.out::println);
        System.out.println("---------------------------------------------------------------------");
        List<User> list6 = findIndexDB("一", new String[]{"userName", "sal"}, 10, User.class);
        list6.forEach(System.out::println);
    }
}
