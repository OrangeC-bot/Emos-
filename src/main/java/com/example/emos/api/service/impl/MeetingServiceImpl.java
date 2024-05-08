package com.example.emos.api.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import com.example.emos.api.common.util.PageUtils;
import com.example.emos.api.db.dao.TbMeetingDao;
import com.example.emos.api.db.pojo.TbMeeting;
import com.example.emos.api.exception.EmosException;
import com.example.emos.api.service.MeetingService;
import com.example.emos.api.task.MeetingWorkflowTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

@Service
@Slf4j
public class MeetingServiceImpl implements MeetingService {

    @Autowired
    private MeetingWorkflowTask meetingWorkflowTask;

    @Autowired
    private TbMeetingDao meetingDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public PageUtils searchOfflineMeetingByPage(HashMap param) {
        ArrayList<HashMap> list = meetingDao.searchOfflineMeetingByPage(param);
        long count = meetingDao.searchOfflineMeetingCount(param);
        int start = (Integer) param.get("start");
        int length = (Integer) param.get("length");
        for (HashMap map : list) {
            String meeting = (String) map.get("meeting");
            if (meeting != null && meeting.length() > 0) {
                map.replace("meeting", JSONUtil.parseArray(meeting));
            }
        }
        PageUtils pageUtils = new PageUtils(list, count, start, length);
        return pageUtils;
    }

    @Override
    public int insert(TbMeeting meeting) {
        int rows = meetingDao.insert(meeting);
        if (rows != 1) {
            throw  new EmosException("会议添加失败");
        }
        meetingWorkflowTask.startMeetingWorkflow(meeting.getUuid(),meeting.getCreatorId(),meeting.getTitle(),
                meeting.getDate(),meeting.getStart()+":00","线下会议");
        return rows;
    }

    @Override
    public ArrayList<HashMap> searchOfflineMeetingInWeek(HashMap param) {
        ArrayList<HashMap> list = meetingDao.searchOfflineMeetingInWeek(param);
        return list;
    }

    @Override
    public HashMap searchMeetingInfo(short status, long id) {
        HashMap map;
        if (status ==4 || status == 5) {
            map = meetingDao.searchCurrentMeetingInfo(id);
        }else {
            map = meetingDao.searchMeetingInfo(id);
        }
        return map;
    }

    @Override
    public int deleteMeetingApplication(HashMap param) {

        Long id = MapUtil.getLong(param, "id");
        String uuid = MapUtil.getStr(param, "uuid");
        String instanceId = MapUtil.getStr(param, "instanceId");

        //查询会议详情，一会儿要判断是否距离会议开始不足20分钟,web层中的userId传入到这里
        HashMap meeting = meetingDao.searchMeetingById(param);
        String date = MapUtil.getStr(meeting, "date");
        String start = MapUtil.getStr(meeting, "start");
        int status = MapUtil.getInt(meeting, "status");
        boolean isCreator = Boolean.parseBoolean(MapUtil.getStr(meeting, "isCreator"));
        DateTime dateTime = DateUtil.parse(date + " " + start);
        DateTime now = DateUtil.date();

        //距离会议开始不足20分钟，不能删除会议
        if (now.isAfterOrEquals(dateTime.offset(DateField.MINUTE, -20))) {
            throw new EmosException("距离会议开始不足20分钟，不能删除会议");
        }

        //只能申请人删除该会议
        if (!isCreator) {
            throw new EmosException("只能申请人删除该会议");
        }

        //待审批和未开始的会议可以删除
        if (status == 1 || status == 3) {
            //删除会议
            int rows = meetingDao.deleteMeetingApplication(param);
            //判断删除成功,将uuid, instanceId, 和reason传到工作流中才可以真正的删除掉会议。
            if (rows == 1) {
                String reason = param.get("reason").toString();
                meetingWorkflowTask.deleteMeetingApplication(uuid, instanceId, reason);
            }
            return rows;
        } else {
            throw new EmosException("只能删除待审批和未开始的会议");
        }

    }

    /**
     * 通过UUID查询存储在缓存中的roomId,但是只有在会议开始前20分钟，
     * 工作流项目的定时器才会生成这个RoomID
     * 用户在前端页面进入到meeting_video.vue页面之后，需要通过Ajax查询这个会议室的RoomID，
     * 然后让TrtcClient连接到视频会议室
     *
     * @param uuid
     * @return
     */
    @Override
    public Long searchRoomIdByUUID(String uuid) {


        if (redisTemplate.hasKey(uuid)) {
            Object temp = redisTemplate.opsForValue().get(uuid);
            long roomId = Long.parseLong(temp.toString());
            return roomId;
        }
        return null;
    }

    @Override
    public ArrayList<HashMap> searchOnlineMeetingMembers(HashMap param) {
        ArrayList<HashMap> list = meetingDao.searchOnlineMeetingMembers(param);
        return list;
    }

    @Override
    public PageUtils searchOnlineMeetingByPage(HashMap param) {

        //查询结果存储到list和count中
        ArrayList<HashMap> list = meetingDao.searchOnlineMeetingByPage(param);
        long count = meetingDao.searchOnlineMeetingCount(param);

        int start = (Integer) param.get("start");
        int length = (Integer) param.get("length");

        //将数据存储到pageUtils中
        PageUtils pageUtils = new PageUtils(list, count, start, length);

        return pageUtils;
    }

    @Override
    public long searchOnlineMeetingCount(HashMap param) {
        return 0;
    }

    @Override
    public boolean searchCanCheckinMeeting(HashMap param) {
        long count = meetingDao.searchCanCheckinMeeting(param);
        return count == 1 ? true: false;
    }

    @Override
    public int updateMeetingPresent(HashMap param) {
        int rows = meetingDao.updateMeetingPresent(param);
        return rows;
    }


}
