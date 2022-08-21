package cn.itcast.hotel;

import cn.itcast.hotel.constants.HotelConstants;
import cn.itcast.hotel.service.IHotelService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootTest
class HotelDemoApplicationTests {

    private RestHighLevelClient client;

    @Autowired
    private IHotelService iHotelService;

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

    /**
     * 创建索引库
     */
    @Test
    void createHotelIndex() throws IOException {
        //1.创建Request对象
        CreateIndexRequest request = new CreateIndexRequest("hotel");
        //2.准备请求的参数：DSL语句
        CreateIndexRequest source = request.source(HotelConstants.MAPPING_TEMPLATE, XContentType.JSON);
        //3.发送请求
        client.indices().create(request, RequestOptions.DEFAULT);
    }

    /**
     * 删除索引库
     * @throws IOException
     */
    @Test
    void testDeleteHotelIndex() throws IOException {
        //1.要删除的索引Request对象
        DeleteIndexRequest request = new DeleteIndexRequest("hotel");
        //2.发送请求
        client.indices().delete(request, RequestOptions.DEFAULT);
    }
    /**
     * 查看索引库是否存在
     * @throws IOException
     */
    @Test
    void testExistsHotelIndex() throws IOException {
        //1.要查询的索引库
        GetIndexRequest request = new GetIndexRequest("hotel");
        //2.发起请求
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println(exists);
    }


    @Test
    void contextLoads() {
        Map<String, List<String>> filters = iHotelService.filters();
        System.out.println(filters);
    }

    @AfterEach
    void tearDown() throws IOException {
        //使用后销毁对象
        client.close();
    }
}
