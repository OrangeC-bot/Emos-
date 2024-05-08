package com.example.emos.api.service;

import cn.hutool.db.Page;
import com.example.emos.api.common.util.PageUtils;

import java.util.HashMap;

public interface ApprovalService {
    /**
     * 查询会议申请分页数据
     * @param param 查询条件
     * @return 会议申请分页数据
     */
    PageUtils searchTaskByPage(HashMap<String, Object> param);
}
