package com.example.emos.api.service;

import com.example.emos.api.common.util.PageUtils;
import com.example.emos.api.db.pojo.TbMeeting;

import java.util.HashMap;

public interface MeetingService {
    public PageUtils searchOfflineMeetingByPage(HashMap param);

    public int insert(TbMeeting meeting);
}
