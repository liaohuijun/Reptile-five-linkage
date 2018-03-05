package com.github.liaohuijun.address.main;

import com.github.liaohuijun.address.bean.Address;
import com.github.liaohuijun.address.dao.AddressDao;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 从中华人民共和国国家统计局的网站爬取省、市、县、镇、村
  * (用一句话描述类的主要功能)
  * @author LIAO  
  * @date 2018年3月5日
 */
public class AddressMain {

    private static Map<Integer, String> cssMap = new HashMap<Integer, String>();
    private static BufferedWriter bufferedWriter = null;
    private static SqlSessionFactory sqlSessionFactory;
    private static SqlSession sqlSession;

    static {
        cssMap.put(1, "provincetr");// 省
        cssMap.put(2, "citytr");// 市
        cssMap.put(3, "countytr");// 县
        cssMap.put(4, "towntr");// 镇
        cssMap.put(5, "villagetr");// 村

        try {
            String resource = "sqlMapConfig.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            sqlSession = sqlSessionFactory.openSession();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        AddressDao addressDao = sqlSession.getMapper(AddressDao.class);
        int level = 1;
        // 获取全国各个省级信息
        Document connect = connect("http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2016/");
        Elements rowProvince = connect.select("tr." + cssMap.get(level));
        for (int i = 0; i < rowProvince.size(); i++) {
            Element element = rowProvince.get(i);
            Elements select = element.select("a");
            for (int j = 0; j < select.size(); j++) {
                Element provinceEle = select.get(j);
                Address province = new Address();
                province.setAreaCode(provinceEle.attr("href").substring(0, 2)+"0000000000");
                province.setName(provinceEle.text());
                province.setFather("0");
                province.setLevel(level);
                province.setShortName(provinceEle.text());
                addressDao.insertOneAddress(province);
                sqlSession.commit();
                System.out.println("===成功插入了省:"+provinceEle.text()+",地区编码:"+provinceEle.attr("href").substring(0, 2)+"0000000000");
                parseNextLevel(provinceEle,level+1,provinceEle.attr("href").substring(0, 2)+"0000000000");
            }
        }
    }

    public static void parseNextLevel(Element element,int level,String parentCode){
        AddressDao addressDao = sqlSession.getMapper(AddressDao.class);
        try {
            Thread.sleep(500);// 睡眠一下，否则可能出现各种错误状态码
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Document doc = connect(element.attr("abs:href"));
        if (doc != null) {
            Elements newsHeadlines = doc.select("tr." + cssMap.get(level));//
            // 获取表格的一行数据
            for (int i = 0; i < newsHeadlines.size(); i++) {
                Element child = newsHeadlines.get(i);
                Address address = new Address();
                address.setAreaCode(child.select("td").first().text());
                address.setLevel(level);
                address.setFather(parentCode);
                address.setName(child.select("td").last().text());
                address.setShortName(formatName(child.select("td").last().text()));
                addressDao.insertOneAddress(address);
                sqlSession.commit();
                System.out.println("===成功插入了地区:"+address.getShortName()+",地区编码:"+address.getAreaCode());
                printInfo(address.getShortName(),address.getAreaCode(),level);
                Elements select = child.select("a");// 在递归调用的时候，这里是判断是否是村一级的数据，村一级的数据没有a标签
                if(select.size()>0){
                    parseNextLevel(select.last(),level+1,child.select("td").first().text());
                }
            }
        }
    }

    private static Document connect(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("The input url('" + url
                    + "') is invalid!");
        }
        try {
            return Jsoup.connect(url).timeout(60 * 1000).get();
        } catch (Exception e) {
            System.out.println("============哎呀呀,连接超时了,URL:"+url);
            e.printStackTrace();
            //连接超时重新连接
            return connect(url);
        }
    }

    /**
     * 写一行数据到数据文件中去
     *
     * @param level
     *            城市级别
     */
    private static void printInfo(String addressName,String areaCode, int level) {
        try {
        	
            File file = new File("/data/www/address/CityInfo.log");  
            if (!file.getParentFile().exists()) {  
                boolean result = file.getParentFile().mkdirs();  
                if (!result) {  
                    System.out.println("创建失败");  
                }  
            }  
        	
            bufferedWriter = new BufferedWriter(new FileWriter(new File(
                    "/data/www/address/CityInfo.log"), true));
            String str = addressName + "{" + level
                    + "}[" + areaCode + "]";
            System.out.println(str);
            bufferedWriter.write(str);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String formatName(String name){
        if (name.indexOf("办事处")>0||name.indexOf("居委会")>0||name.indexOf("村村委会")>0){
            return name.substring(0,name.length()-3);
        }else if(name.indexOf("村村民委员会")>0){
            return name.substring(0,name.length()-5);
        }else if(name.indexOf("村民委员会")>0){
            return name.substring(0,name.length()-4);
        }else if(name.indexOf("村委会")>0){
            return name.substring(0,name.length()-2);
        }else if (name.indexOf("居民委员会")>0){
            return name.substring(0,name.length()-5);
        }else if(name.indexOf("委员会")>0){
            return name.substring(0,name.length()-3);
        }else {
            return name;
        }
    }
}
