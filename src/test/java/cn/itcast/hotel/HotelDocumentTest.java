package cn.itcast.hotel;

import cn.itcast.hotel.constants.HotelConstants;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class HotelDocumentTest {

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


    @Test
    void testInit() {
        System.out.println(client);
    }
    //添加使用index
    @Test
    void testAddDocument() throws IOException {
        //根据id查询酒店数据,数据库是long类型
        Hotel hotel = hotelService.getById("309208L");
        //将查询出来的数据转为hotelDoc
        HotelDoc hotelDoc = new HotelDoc(hotel);
        System.out.println(hotelDoc);
        //1.准备request对象,指定索引库名和id,索引库的字段都是String
        IndexRequest indexRequest = new IndexRequest("hotel").id(hotelDoc.getId().toString());
        //2.准备json文档,source,数据需要从数据库中查询
        indexRequest.source(JSON.toJSONString(hotelDoc),XContentType.JSON);
        //3.发送请求
        client.index(indexRequest,RequestOptions.DEFAULT);
    }

    //查询使用get
    @Test
    void testGetDocumentById() throws IOException {
        //1.准备getRequest对象
        GetRequest getRequest = new GetRequest("hotel").id("309208");
        //2.发送请求，得到响应
        GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
        //3.解析响应结果
        String sourceAsString = response.getSourceAsString();//转为json的字符串
        System.out.println(sourceAsString);
        //4.进行反序列化
        HotelDoc hotelDoc = JSON.parseObject(sourceAsString, HotelDoc.class);
        System.out.println(hotelDoc);


    }
    //修改文档
    @Test
    void testUpdateDocument() throws IOException {
        // 1.准备Request
        UpdateRequest request = new UpdateRequest("hotel", "309208");
        // 2.准备请求参数
        request.doc(
                "price", "2001",
                "starName", "五钻"
        );
        // 3.发送请求
        UpdateResponse update = client.update(request, RequestOptions.DEFAULT);
        System.out.println(update.getGetResult());

    }
    //删除文档
    @Test
    void testDeleteDocument() throws IOException {
        // 1.准备Request
        DeleteRequest request = new DeleteRequest("hotel", "61083");
        // 2.发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }
    @AfterEach
    void tearDown() throws IOException {
        //使用后销毁对象
        client.close();
    }
}
