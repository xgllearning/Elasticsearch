package cn.itcast.hotel.mq;

import cn.itcast.hotel.constants.MqConstants;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HotelListener {

    @Autowired
    private IHotelService hotelService;
    /**
     * 更新和新增的
     * @param id
     */
    @RabbitListener(queues = MqConstants.HOTEL_INSERT_QUEUE)
    public void listenTopicQueue1(Long id){
        System.out.println("消费者接收到hotel.insert.queue的消息：【" + id + "】");
        hotelService.insertById(id);
    }

    /**
     * 删除业务
     * @param id
     */
    @RabbitListener(queues = MqConstants.HOTEL_DELETE_QUEUE)
    public void listenTopicQueue3(Long id){
        System.out.println("消费者接收到topic.queue2的消息：【" + id + "】");
        hotelService.deleteById(id);
    }
}
