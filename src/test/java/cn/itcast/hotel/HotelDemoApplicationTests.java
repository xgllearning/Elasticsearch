package cn.itcast.hotel;

import cn.itcast.hotel.constants.HotelConstants;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class HotelDemoApplicationTests {

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

    @AfterEach
    void tearDown() throws IOException {
        //使用后销毁对象
        client.close();
    }
}
