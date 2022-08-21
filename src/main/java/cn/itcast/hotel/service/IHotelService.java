package cn.itcast.hotel.service;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;

public interface IHotelService extends IService<Hotel> {
    // 搜索酒店数据
    PageResult search(RequestParams params);
}
