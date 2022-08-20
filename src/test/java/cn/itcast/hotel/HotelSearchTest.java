package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
class HotelSearchTest {

    @Autowired
    private IHotelService hotelService;

    private RestHighLevelClient client;

    @BeforeEach
    void setUp() {
        //定义成员变量，使用前创建对象，就可以不用每次测试都需要初始化client对象
        client=new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.177.128:9200")//如果是集群的话，可以逗号分割指定多个地址
        ));
    }
    //对解析结果进行一个抽取,ctrl+alt+m
    private void handleResponse(SearchResponse response) {
        //4.解析响应结果
        SearchHits searchHits = response.getHits();
        //5.获取总条数
        long value = searchHits.getTotalHits().value;
        System.out.println("查询到的条数是"+value);
        //6.查询的结果数组
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            //7.得到source，数据
            String json = hit.getSourceAsString();//json字符串，可以转为对象
            //反序列化
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            System.out.println(hotelDoc);
        }
    }

    @Test
    void testMatchAll() throws IOException {
        //1.准备request,准备索引库名称
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL
        request.source().query(QueryBuilders.matchAllQuery());
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.解析响应结果
        handleResponse(response);

    }
    //全文检索的match搜索--单字段搜索，差别是查询条件，也就是query的部分
    @Test
    void testMatch() throws IOException {
        //1.准备request,准备索引库名称
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL，QueryBuilders.matchQuery(字段，内容)
        request.source().query(QueryBuilders.matchQuery("all","希尔顿"));
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //4.解析响应结果
        handleResponse(response);
    }

    //全文检索的multi_match搜索--多字段搜索，差别是查询条件，也就是query的部分
    @Test
    void testMultiMatch() throws IOException {
        //1.准备request,准备索引库名称
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL,QueryBuilders.multiMatchQuery("要查询的内容","字段1","字段2","字段3")
        request.source().query(QueryBuilders.multiMatchQuery("希尔顿","name","address","business"));
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    //词条查询-精确查询term
    @Test
    void testTerm() throws IOException {
        //1.准备request,准备索引库名称
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL,精确查询term,termQuery("字段","精确匹配的值，不分词")，如果用户输入的内容过多，反而搜索不到数据
        request.source().query(QueryBuilders.termQuery("name","希尔顿"));
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    //词条查询-范围查询Range
    @Test
    void testRange() throws IOException {
        //1.准备request,准备索引库名称
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL,精确查询Range,rangeQuery("字段").gte(大于).lte(小于等于)，一般应用在对数值类型做范围过滤的时候。比如做价格范围过滤
        request.source().query(QueryBuilders.rangeQuery("price").gte(1000).lte(2000));
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    //符合查询-bool,布尔查询是用must、must_not、filter等方式组合其它查询
    @Test
    void testBool() throws IOException {
        //1.准备request,准备索引库名称
        SearchRequest request = new SearchRequest("hotel");
        //2.准备DSL,搜索姓名包含如家，价格不高于400，在坐标31.21，121.5周围10km范围内的酒店
        request.source().query(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("name","如家"))
                .mustNot(QueryBuilders.rangeQuery("price").gt(400)));//!filter(QueryBuilders.rangeQuery("price").gt(400));
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    @AfterEach
    void tearDown() throws IOException {
        //使用后销毁对象
        client.close();
    }
}
