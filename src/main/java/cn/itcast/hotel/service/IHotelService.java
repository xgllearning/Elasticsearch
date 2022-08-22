
package cn.itcast.hotel.service;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IHotelService extends IService<Hotel> {
    // 搜索酒店数据
    PageResult search(RequestParams params);
    //多条件聚合查询，动态显示城市、星级、品牌
    Map<String, List<String>> filters(RequestParams params);
    //实现搜索框自动补全
    List<String> getSuggestions(String prefix);

    void insertById(Long id);

    void deleteById(Long id);
}
