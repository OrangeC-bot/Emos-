package com.example.emos.api.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.example.emos.api.common.util.PageUtils;
import com.example.emos.api.common.util.R;
import com.example.emos.api.controller.form.SearchTaskByPageForm;
import com.example.emos.api.service.ApprovalService;
import com.example.emos.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;

@RestController
@RequestMapping("/approval")
@Tag(name = "ApprovalController", description = "任务审批Web接口")
@Slf4j
public class ApprovalController {
    @Value("${workflow.url}")
    private String workflow;

    @Value("${emos.code}")
    private String code;

    @Value("${emos.tcode}")
    private String tcode;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private UserService userService;


    @PostMapping("/searchTaskByPage")
    @Operation(summary = "查询分页任务列表")
    @SaCheckPermission(value = {"WORKFLOW:APPROVAL", "FILE:ARCHIVE"}, mode = SaMode.OR)
    public R searchTaskByPage(@Valid @RequestBody SearchTaskByPageForm form) {

        //获取表单form传送的param，将form表单传输过去
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        //获取操作的用户信息，将用户信息传到userId
        int userId = StpUtil.getLoginIdAsInt();
        param.put("userId", userId);
        //将从TbUser数据库中查询到的角色权限值封装成role,存储到HashMap中,用于HttpRequest请求发送给工作流项目中的角色权限
        param.put("role", userService.searchUserRoles(userId));
        //将从工作流中查询的结果存储封装到PageUtils工具类中
        PageUtils pageUtils = approvalService.searchTaskByPage(param);
        return R.ok().put("page", pageUtils);
    }
//    @PostMapping("/searchTaskByPage")
//    @Operation(summary = "查询分页任务列表")
//    @SaCheckPermission(value = {"ROOT", "WORKFLOW:APPROVAL", "FILE:ARCHIVE"}, mode = SaMode.OR)
//    public R searchTaskByPage(@Valid @RequestBody SearchTaskByPageForm searchTaskByPageForm) {
//        HashMap<String, Object> param = JSONUtil.parse(searchTaskByPageForm).toBean(HashMap.class);
//        int userId = StpUtil.getLoginIdAsInt();
//        param.put("userId", userId);
//        param.put("role", userService.searchUserRoles(userId));
//        return R.ok().put("page", approvalService.searchTaskByPage(param));
//    }
}
