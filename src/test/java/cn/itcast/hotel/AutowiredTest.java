package cn.itcast.hotel;

import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AutowiredTest {
    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Test
    public void test(){

        System.out.println(client);
        System.out.println("==================");
        System.out.println(restHighLevelClient);
        System.out.println("==================");
    }
}
