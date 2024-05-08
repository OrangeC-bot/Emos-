package com.example.emos.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.api.common.util.PageUtils;
import com.example.emos.api.common.util.R;
import com.example.emos.api.config.tencent.TrtcUtil;
import com.example.emos.api.controller.form.*;
import com.example.emos.api.db.pojo.TbMeeting;
import com.example.emos.api.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@RestController
@RequestMapping("/meeting")
@Tag(name = "MeetingController", description = "会议web接口")
@Slf4j
public class MeetingController {

    @Autowired
    private MeetingService meetingService;
    @Autowired
    private TrtcUtil trtcUtil;
    @Value("${tencent.trtc.appId}")
    private int appId;

    @PostMapping("/searchOfflineMeetingByPage")
    @Operation(summary = "分页查询线下会议信息")
    @SaCheckLogin
    public R searchOfflineMeetingByPage(@Valid @RequestBody SearchOfflineMeetingByPageForm form) {
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        HashMap param = new HashMap<>() {{
            put("date",form.getDate());
            put("mold",form.getMold());
            put("userId", StpUtil.getLoginId());
            put("start",start);
            put("length",length);
        }};
        PageUtils pageUtils = meetingService.searchOfflineMeetingByPage(param);
        return R.ok().put("page", pageUtils);
    }

    @PostMapping("/insert")
    @Operation(summary = "新增会议信息")
    @SaCheckPermission(value = {"ROOT", "MEETING:INSERT"}, mode = SaMode.OR)
    public R insert(@Valid @RequestBody InsertMeetingForm form) {
        String date = form.getDate();
        DateTime start = DateUtil.parse(date + " " + form.getStart());
        DateTime end = DateUtil.parse(date + " " + form.getEnd());
        if (start.isAfterOrEquals(end)) {
            return R.error("会议结束时间必须大于开始时间");
        } else if (new DateTime().isAfterOrEquals(start)) {
            return R.error("会议开始时间不能早于当前时间");
        }
        TbMeeting meeting = JSONUtil.parse(form).toBean(TbMeeting.class);
        meeting.setUuid(UUID.randomUUID().toString(true));
        meeting.setCreatorId(StpUtil.getLoginIdAsInt());
        meeting.setStatus((short) 1);
        int rows = meetingService.insert(meeting);
        return R.ok().put("rows", rows);
    }

    @PostMapping("/receiveNotify")
    @Operation(summary = "接受工作流通知")
    public R receiveNotify(@Valid @RequestBody RecieveNotifyForm form) {
        if (form.getResult().equals("同意")) {
            log.debug(form.getUuid()+"的会议审批通过");
        }
        else {
            log.debug(form.getUuid()+"的会议审批不通过");
        }
        return R.ok();
    }

    @PostMapping("/searchOfflineMeetingInWeek")
    @Operation(summary = "查询某个会议室一周的会议")
    @SaCheckLogin
    public R searchOfflineMeetingInWeek(@Valid @RequestBody SearchOfflineMeetingInWeekForm form){

        String date = form.getDate();
        DateTime startDate, endDate;
        /**
         * 判断传入的date是否有值，有则从date开始，查询生成七天的日期
         * 否则将查询当前日期，生成七天的日期
         */
        if (date != null && date.length() > 0) {
            //从date开始，生成七天日期
            startDate = DateUtil.parseDate(date);
            endDate = startDate.offsetNew(DateField.DAY_OF_WEEK, 6);

        } else {
            //查询当前日期，生成本周的日期
            startDate = DateUtil.beginOfWeek(new Date());
            endDate = DateUtil.endOfWeek(new Date());
        }

        //将form中的所有信息存储到HashMap中,以便传入到service层中查询
        HashMap param = new HashMap() {{
            put("place", form.getName());
            put("startDate", startDate.toDateStr());
            put("endDate", endDate.toDateStr());
            put("mold", form.getMold());
            put("userId", StpUtil.getLoginIdAsLong());
        }};

        ArrayList list = meetingService.searchOfflineMeetingInWeek(param);

        //生成周日历水平表头的文字标题
        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_WEEK);
        ArrayList days = new ArrayList();
        //输出每个json数据
        range.forEach(one -> {
            JSONObject json = new JSONObject();
            json.set("date", one.toString("MM/dd"));
            json.set("day", one.dayOfWeekEnum().toChinese("周"));
            days.add(json);
        });

        return R.ok().put("list",list).put("days",days);
    }

    @PostMapping("/searchMeetingInfo")
    @Operation(summary = "查询会议信息")
    @SaCheckLogin
    public R searchMeetingInfo(@Valid @RequestBody SearchMeetingInfoForm form){

        //直接将值传入到后端中
        HashMap map = meetingService.searchMeetingInfo(form.getStatus(), form.getId());
        return R.ok(map);
    }

    @PostMapping("/deleteMeetingApplication")
    @Operation(summary = "删除会议申请")
    @SaCheckLogin
    public R deleteMeetingApplication(@Valid @RequestBody DeleteMeetingApplicationForm form){

        //将form表单信息存储到 param中
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        //获取creatorId，用于判断是否为创建会议者id
        param.put("creatorId", StpUtil.getLoginIdAsLong());
        param.put("userId",StpUtil.getLoginIdAsLong());

        int rows = meetingService.deleteMeetingApplication(param);
        return R.ok().put("rows", rows);
    }

    @PostMapping("/searchOnlineMeetingByPage")
    @Operation(summary = "查询线上会议分页数据")
    @SaCheckLogin
    public R searchOnlineMeetingByPage(@Valid @RequestBody SearchOnlineMeetingByPageForm form){
        //form{date, mold, page, length}

        //算出start
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;

        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        param.put("start", start);
        param.put("userId", StpUtil.getLoginId());
        PageUtils pageUtils= meetingService.searchOnlineMeetingByPage(param);
        return R.ok().put("page",pageUtils);
    }

    @GetMapping("/searchMyUserSig")
    @Operation(summary = "获取用户签名")
    @SaCheckLogin
    public R searchMyUserSig() {

        int userId = StpUtil.getLoginIdAsInt();
        String userSig = trtcUtil.genUserSig(userId + "");
        return R.ok().put("userSig", userSig).put("userId", userId).put("appId", appId);

    }

    @PostMapping("/searchRoomIdByUUID")
    @Operation(summary = "查询会议房间RoomId")
    @SaCheckLogin
    public R searchRoomIdByUUID(@Valid @RequestBody SearchRoomIdByUUIDForm form){

        Long roomId = meetingService.searchRoomIdByUUID(form.getUuid());
        return R.ok().put("roomId", roomId);
    }

    @PostMapping("/searchOnlineMeetingMembers")
    @Operation(summary = "查询线上会议成员")
    public R searchOnlineMeetingMembers(@Valid @RequestBody SearchOnlineMeetingMembersForm form){

        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        //获取登录用户的userId
        param.put("userId", StpUtil.getLoginIdAsInt());
        //将条件信息meetingId和userId传到后端进行查询
        ArrayList<HashMap> list = meetingService.searchOnlineMeetingMembers(param);
        return R.ok().put("list", list);

    }

    @PostMapping("/updateMeetingPresent")
    @Operation(summary = "执行会议签到")
    @SaCheckLogin
    public R updateMeetingPresent(@Valid @RequestBody UpdateMeetingPresentForm form){

        HashMap param = new HashMap(){{

            //给哈希变量param赋值meetingId和userId
            put("meetingId", form.getMeetingId());
            put("userId", StpUtil.getLoginIdAsInt());

        }};

        //判断查询的会议是否存在（会议状态为未进行或已开始）
        boolean bool = meetingService.searchCanCheckinMeeting(param);
        //如果会议状态符合未进行和已开始,则返回true，执行以下判断条件,返回更新的参会签到条数
        if(bool){

            int rows = meetingService.updateMeetingPresent(param);
            return R.ok().put("rows", rows);

        }
        //否则返回0,表示更新人数为0，更新失败
        return R.ok().put("rows", 0);


    }
}
