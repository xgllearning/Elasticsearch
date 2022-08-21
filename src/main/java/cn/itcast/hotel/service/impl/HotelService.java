package cn.itcast.hotel.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
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
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
            //获取排序距离
            Object[] sortValues = hit.getSortValues();
            if (!ArrayUtil.isEmpty(sortValues)) {
                Object value = sortValues[0];
                hotelDoc.setDistance(value);
            }
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
        return new PageResult(total, hotels);//返回pageResult
    }


    private void buildBasicQuery(RequestParams params, SearchRequest request) {


        //注意事项：多个条件之间是AND关系，组合多条件用BooleanQuery,参数存在才需要过滤，做好非空判断
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //全文检索作为BoolQuery中must部分,关键字搜索
        //2.1query，利用match查询，根据参数中的key搜索all字段，查询酒店信息并返回
        String key = params.getKey();//获取参数，并做健壮性判断
        if (StringUtils.isEmpty(key)) {
            boolQuery.must(QueryBuilders.matchAllQuery());//没有查询条件match_all
        } else {
            boolQuery.must(QueryBuilders.matchQuery("all", key));
        }
        //条件过滤,city精确匹配,brand精确匹配,starName精确匹配.price范围过滤
        String city = params.getCity();
        if (!StringUtils.isEmpty(city)) {//此时可以进行term查询,属于过滤查询,不参与算法
            boolQuery.filter(QueryBuilders.termQuery("city", city));
        }
        String brand = params.getBrand();
        if (!StringUtils.isEmpty(brand)) {//此时可以进行term查询,属于过滤查询,不参与算法
            boolQuery.filter(QueryBuilders.termQuery("brand", brand));
        }
        String starName = params.getStarName();
        if (!StringUtils.isEmpty(starName)) {//此时可以进行term查询,属于过滤查询,不参与算法
            boolQuery.filter(QueryBuilders.termQuery("starName", starName));
        }
        //价格使用范围查询
        Integer minPrice = params.getMinPrice();
        Integer maxPrice = params.getMaxPrice();
        if (minPrice != null && maxPrice != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(minPrice).lte(maxPrice));
        }
        //TODO：广告置顶，加权模式，使用function score,此时bool只能为原始查询
        //算分控制,参数一原始查询，参数二算分查询
        FunctionScoreQueryBuilder functionScoreQuery =
                QueryBuilders.functionScoreQuery(
                        //原始查询，相关性算法的查询
                        boolQuery,
                        //算分查询，function score数组层
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                //其中一个functions 元素
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        //参数一过滤条件，参数二，算分函数
                                        QueryBuilders.termQuery("isAD", true),
                                        ScoreFunctionBuilders.weightFactorFunction(5)
                                )
                        });
        //查询，传入查询条件functionScoreQuery
        request.source().query(functionScoreQuery);
    }
    /**
     * 搜索酒店数据，根据关键字搜索并分页
     * 利用match查询，根据参数中的key搜索all字段，查询酒店信息并返回
     * 利用参数中的page、size实现分页
     *
     * @param params
     * @return
     */
    @Override
    public PageResult search(RequestParams params) {

        try {
            //1.准备request,指定索引
            SearchRequest request = new SearchRequest("hotel");
            //2.准备dsl查询条件
            //先构建BoolQuery,最后封装好查询条件,再request.source().query()
            buildBasicQuery(params, request);

            //TODO：广告置顶，加权模式，使用function score,此时bool只能为原始查询

            //2.2分页,参与运算要进行拆箱
            int page = params.getPage();
            int paramsSize = params.getSize();
            request.source().from((page - 1) * paramsSize).size(paramsSize);

            //2.3排序,按照地理坐标排序
            String location = params.getLocation();
            if (!StringUtils.isEmpty(location)) {
                request.source().sort(SortBuilders.geoDistanceSort("location", new GeoPoint(location))
                        .order(SortOrder.ASC).unit(DistanceUnit.KILOMETERS));
            }

            //3.发送请求，得到响应
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //4.解析响应,返回pageResult对象
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 多条件聚合查询，动态显示城市、星级、品牌
     * @return
     */
    @Override
    public Map<String, List<String>> filters(RequestParams params) {
        try {
            //1.准备SearchRequest
            SearchRequest request = new SearchRequest("hotel");
            //2.准备DSL，设置size为0
            //TODO：补充查询信息
            buildBasicQuery(params,request);
            request.source().size(0);
            buildAggregation(request);
            //3.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //4.封装需要返回的对象
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            //5.解析结果
            Aggregations aggregations = response.getAggregations();
            //5.1解析品牌
            List<String> brandList = getAggByName(aggregations, "brandCount");
            //5.2解析城市
            List<String> cityList = getAggByName(aggregations, "cityCount");
            //5.3解析星级
            List<String> starList = getAggByName(aggregations, "starCount");
            //在map中放入list
            map.put("brand", brandList);
            map.put("city", cityList);
            map.put("starName", starList);
            return map;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildAggregation(SearchRequest request) {
        //聚合抽取,对品牌
        request.source().aggregation(AggregationBuilders.terms("brandCount")
                .field("brand").size(100));
        //对城市
        request.source().aggregation(AggregationBuilders.terms("cityCount")
                .field("city").size(100));
        //对星级
        request.source().aggregation(AggregationBuilders.terms("starCount")
                .field("starName").size(100));
    }

    private List<String> getAggByName(Aggregations aggregations,String aggName) {
        //根据聚合名称获取聚合结果
        Terms brandTerms= aggregations.get(aggName);
        //获取buckets并遍历
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        List<String> list = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            //获取key
            String brand = bucket.getKeyAsString();
            list.add(brand);
        }
        return list;
    }

}
