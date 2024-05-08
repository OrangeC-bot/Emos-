package com.example.emos.api.service.impl;


import cn.hutool.db.Page;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.api.common.util.PageUtils;
import com.example.emos.api.exception.EmosException;
import com.example.emos.api.service.ApprovalService;
import com.example.emos.api.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;

@Service
@Slf4j
public class ApprovalServiceImpl implements ApprovalService {

    @Value("${workflow.url}")
    private String workflow;
    @Autowired
    private UserService userService;

//    @Value("${emos.code}")
//    private String code;
//
//    @Value("${emos.tcode}")
//    private String tcode;

    @Value("${workflow.searchTaskByPage}")
    private String searchTaskByPagePath;

    @Value("${workflow.searchApprovalContent}")
    private String searchApprovalContentPath;

    @Value("${workflow.approvalTaskPath}")
    private String approvalTaskPath;

    @Value("${workflow.archiveTaskPath}")
    private String archiveTaskPath;

    @Value("${workflow.searchApprovalTaskCountPath}")
    private String searchApprovalTaskCountPath;


    //    public PageUtils searchTaskByPage(HashMap param){
//
//        //放入 code 和 tcode，可以用来访问工作流 workflow
//        param.put("code", code);
//        param.put("tcode", tcode);
//        //访问工作流的接口。下面两句代码主要用于远程服务器进行数据交互（如调用接口，获取数据等）
//        //它的作用是向workflow服务器的/workflow/searchTaskByPage接口发送一个POST请求，并将param对象作为请求体发送，然后获取响应结果
//        String url = workflow + "/workflow/searchTaskByPage";
//        HttpResponse resp = HttpRequest.post(url).header("Content-Type", "application/json")
//                .body(JSONUtil.toJsonStr(param)).execute();
//
//        //获取工作流中响应的数据
//        if(resp.getStatus() == 200){
//            //将响应体中的内容提取出来  json {page: {list, totalCount, pageIndex, pageSize}}
//            JSONObject json = JSONUtil.parseObj(resp.body());
//            JSONObject page = json.getJSONObject("page");
//            ArrayList list = page.get("list", ArrayList.class);
//            Long totalCount = page.getLong("totalCount");
//            Integer pageIndex = page.getInt("pageIndex");
//            Integer pageSize = page.getInt("pageSize");
//            //提取出来后存储到PageUtils中
//            PageUtils pageUtils = new PageUtils(list, totalCount, pageIndex, pageSize);
//            //返回 pageUtils
//            return pageUtils;
//
//        }else { //响应失败则发送log日志
//
//            log.error(resp.body());
//            throw new EmosException("获取工作流数据异常");
//
//        }
//
//    }
    @Override
    public PageUtils searchTaskByPage(HashMap param) {
        String url = workflow + searchTaskByPagePath;
        HttpResponse httpResponse = HttpRequest.post(url).header("Content-Type", "application/json").body(JSONUtil.toJsonStr(param)).execute();
        if (httpResponse.getStatus() == 200) {
            JSONObject jsonObject = JSONUtil.parseObj(httpResponse.body());
            JSONObject page = jsonObject.getJSONObject("page");
            ArrayList<JSONObject> list = page.get("list", ArrayList.class);
            ArrayList<HashMap<String, Object>> dataList = new ArrayList<>(list.size());
            for (JSONObject json : list) {
                HashMap<String, Object> map = JSONUtil.parse(json).toBean(HashMap.class);
                dataList.add(map);
            }
            Long totalCount = page.getLong("totalCount");
            Integer pageIndex = page.getInt("pageIndex");
            Integer pageSize = page.getInt("pageSize");
            return new PageUtils(list, totalCount, pageIndex, pageSize);
        } else {
            log.error(httpResponse.body());
            throw new EmosException("获取工作流数据异常");
        }
    }

}
