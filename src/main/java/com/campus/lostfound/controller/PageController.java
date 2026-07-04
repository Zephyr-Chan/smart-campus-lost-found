package com.campus.lostfound.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 页面控制器 - 负责JSP页面路由跳转
 * 所有页面视图由前端JS控制登录状态，API接口仍由LoginInterceptor保护
 */
@Controller
public class PageController {

    /**
     * 首页
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 登录页
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * 注册页
     */
    @GetMapping("/register")
    public String register() {
        return "register";
    }

    /**
     * 丢失物品列表
     */
    @GetMapping("/lost/list")
    public String lostList() {
        return "lost/list";
    }

    /**
     * 发布丢失物品
     */
    @GetMapping("/lost/publish")
    public String lostPublish() {
        return "lost/publish";
    }

    /**
     * 拾到物品列表
     */
    @GetMapping("/found/list")
    public String foundList() {
        return "found/list";
    }

    /**
     * 发布拾到物品
     */
    @GetMapping("/found/publish")
    public String foundPublish() {
        return "found/publish";
    }

    /**
     * 物品详情
     */
    @GetMapping("/item/detail")
    public String itemDetail() {
        return "item/detail";
    }

    /**
     * 匹配结果
     */
    @GetMapping("/match/result")
    public String matchResult() {
        return "match/result";
    }

    /**
     * 我的认领
     */
    @GetMapping("/claim/my")
    public String claimMy() {
        return "claim/my";
    }

    /**
     * 个人中心
     */
    @GetMapping("/user/profile")
    public String userProfile() {
        return "user/profile";
    }

    /**
     * 数据看板
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    // ===== 管理员页面 =====

    /**
     * 公告管理
     */
    @GetMapping("/admin/announcement")
    public String adminAnnouncement() {
        return "admin/announcement";
    }

    /**
     * 用户管理
     */
    @GetMapping("/admin/users")
    public String adminUsers() {
        return "admin/users";
    }

    /**
     * 操作日志
     */
    @GetMapping("/admin/logs")
    public String adminLogs() {
        return "admin/logs";
    }

    // ===== V2 升级新增页面 =====

    /**
     * 消息中心
     */
    @GetMapping("/message/list")
    public String messageList() {
        return "message/list";
    }

    /**
     * 贡献排行榜
     */
    @GetMapping("/rank/list")
    public String rankList() {
        return "rank/list";
    }

    /**
     * 我的收藏
     */
    @GetMapping("/favorite/my")
    public String favoriteMy() {
        return "favorite/my";
    }

    /**
     * 全文搜索结果
     */
    @GetMapping("/search")
    public String searchResult() {
        return "search/result";
    }
}
