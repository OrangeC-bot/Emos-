package com.example.emos.api.task;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import com.example.emos.api.db.dao.TbReimDao;
import com.example.emos.api.db.dao.TbUserDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReimWorkflowTask {
    @Value("${workflow.url}")
    private String workflowUrl;

    @Value("${workflow.reimWorkflowPath}")
    private String startReimPath;

    @Value("${workflow.deleteProcessPath}")
    private String deleteReimPath;

    @Value("${workflow.reimApprovalReceiveNotifyUrl}")
    private String receiveNotifyUrl;

    @Autowired
    private TbUserDao tbUserDao;

    @Autowired
    private TbReimDao tbReimDao;

    @Async("AsyncTaskExecutor")
    public void deleteLeaveWorkflow(String instanceId, String type, String reason) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("reason", reason);
        jsonObject.set("instanceId", instanceId);
        jsonObject.set("type", type);
        String url = workflowUrl + deleteReimPath;
        HttpResponse httpResponse = HttpRequest
                .post(url)
                .header("Content-Type", "application/json")
                .body(jsonObject.toString()).execute();
        if (httpResponse.getStatus() == 200) {
            log.info("报销申请删除成功");
        } else {
            log.error(httpResponse.body());
        }
    }

}
