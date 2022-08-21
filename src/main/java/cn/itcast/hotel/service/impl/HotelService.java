package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Autowired
    private RestHighLevelClient client;

    //    @Autowired
//    private RestHighLevelClient restHighLevelClient;
//对解析结果进行一个抽取,ctrl+alt+m
    private PageResult handleResponse(SearchResponse response) {
        //4.解析响应结果
        SearchHits searchHits = response.getHits();
        //5.获取总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("查询到的条数是" + total);
        //6.查询的结果数组
        SearchHit[] hits = searchHits.getHits();
        //每一个元素都要放到集合中封装进pageResult对象中
        List<HotelDoc> hotels = new ArrayList<>();
        for (SearchHit hit : hits) {
            //7. 获取文档source
            String json = hit.getSourceAsString();
            //8. 反序列化
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            //9. 获取高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields)) {//健壮性判断
                // 根据字段名获取高亮结果
                HighlightField highlightField = highlightFields.get("name");//根据字段取
                if (highlightField != null) {
                    // 获取高亮值
                    String name = highlightField.getFragments()[0].string();
                    // 覆盖非高亮结果---展示的就为高亮
                    hotelDoc.setName(name);
                }
                System.out.println(hotelDoc);
            }
            hotels.add(hotelDoc);//将解析到的hotelDoc对象封装进集合
        }
        return new PageResult(total,hotels);//返回pageResult
    }

    /**
     * 搜索酒店数据，根据关键字搜索并分页
     * 利用match查询，根据参数中的key搜索all字段，查询酒店信息并返回
     * 利用参数中的page、size实现分页
     * @param params
     * @return
     */
    @Override
    public PageResult search(RequestParams params)  {

        try {
            //1.准备request,指定索引
            SearchRequest request = new SearchRequest("hotel");
            //2.准备dsl查询条件
            //2.1query，利用match查询，根据参数中的key搜索all字段，查询酒店信息并返回
            String key = params.getKey();//获取参数，并做健壮性判断
            if(StringUtils.isEmpty(key)){
                request.source().query(QueryBuilders.matchAllQuery());//没有查询条件match_all
            }else {
                request.source().query(QueryBuilders.matchQuery("all",key));
            }
            //2.2分页,参与运算要进行拆箱
            int page = params.getPage();
            int paramsSize = params.getSize();
            request.source().from((page-1)*paramsSize).size(paramsSize);
            //3.发送请求，得到响应
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //4.解析响应,返回pageResult对象
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
