package com.example.emos.api.task;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.api.db.dao.TbMeetingDao;
import com.example.emos.api.db.dao.TbUserDao;
import com.example.emos.api.db.pojo.TbUser;
import com.example.emos.api.exception.EmosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@Slf4j
public class MeetingWorkflowTask {

    @Autowired
    private TbUserDao userDao;
    @Autowired
    private TbMeetingDao meetingDao;

    @Value("${emos.recieveNotify}")
    private String recieveNotify;

    @Value("${workflow.url}")
    private String workflow;

    @Async("AsyncTaskExecutor")
    public void startMeetingWorkflow(String uuid,
                                     int creatorId,
                                     String title,
                                     String date,
                                     String start,
                                     String meetingType) {
        // 查询会议申请者的基本信息
        HashMap info = userDao.searchUserInfo(creatorId);
        JSONObject json = new JSONObject();
        json.set("uuid", uuid);
        json.set("creatorId", creatorId);
        json.set("creatorName", info.get("name").toString());
        json.set("title", title);
        json.set("notifyUrl", recieveNotify);
        json.set("date", date);
        json.set("start", start);
        json.set("meetingType", meetingType);
        String[] roles = info.get("roles").toString().split(", ");
        if (!ArrayUtil.contains(roles,"总经理")) {
            // 会议申请者不是总经理
            // 查询部门经理id
            Integer managerId = userDao.searchDeptManagerId(creatorId);
            json.set("managerId", managerId);

            // 查询总经理id
            Integer gmId = userDao.searchGmId();
            json.set("gmId", gmId);

            // 查询参会人是否为同一部门
            boolean bool = meetingDao.searchMeetingMembersInSameDept(uuid);
            json.set("sameDept", bool);
        }


        // 向工作流项目发送http请求，请求创建会议工作流实例
        String url = workflow + "/workflow/startMeetingProcess";
        HttpResponse resp = HttpRequest
                .post(url)
                .header("Content-Type", "application/json")
                .body(json.toString()).execute();
        if (resp.getStatus() == 200) {
            json = JSONUtil.parseObj(resp.body());
            String instanceId = json.getStr("instanceId");

            // 更新会议记录的instance_id
            HashMap param = new HashMap();
            param.put("uuid", uuid);
            param.put("instanceId", instanceId);
            int rows = meetingDao.updateMeetingInstanceId(param);
            if (rows != 1) {
                throw new EmosException("保存会议工作流实例ID失败！");
            }
        } else {
            log.error(resp.body());
        }
    }
    @Async("AsyncTaskExecutor")
    public void deleteMeetingApplication(String uuid, String instanceId, String reason){

        JSONObject json = new JSONObject();
        json.set("uuid", uuid);
        json.set("instanceId", instanceId);
        json.set("type", "会议申请");
        json.set("reason", reason);

        //发送http请求响应
        String url = workflow + "/workflow/deleteProcessById";
        HttpResponse resp = HttpRequest.post(url).header("Content-Type", "application/json")
                .body(json.toString()).execute();

        if(resp.getStatus() == 200){
            log.debug("删除了会议申请");
        }else {
            //打印错处消息
            log.error(resp.body());
        }
    }
}
