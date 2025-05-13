package com.site.blog.my.core.controller.admin;

import com.site.blog.my.core.entity.AdminUser;
import com.site.blog.my.core.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminUserService adminUserService;


    @GetMapping("/register")
    public String registerPage() {
        return "admin/register";
    }

    @PostMapping("/register")
    public String register(
                @RequestParam("loginUserName") String loginUserName,
                @RequestParam("password") String password,
                @RequestParam("nickName") String nickName,
                HttpSession session){
        if(StringUtils.isEmpty(loginUserName) ||
                StringUtils.isEmpty(password) ||
                StringUtils.isEmpty(nickName)){
            session.setAttribute("errorMsg","所有字段不能为空");
            return "redirect:/admin/register";
        }
        if(adminUserService.isUsernameExists(loginUserName)){
            session.setAttribute("errMsg","用户已存在");
            return "redirect:/admin/register";
        }
        AdminUser newUser = new AdminUser();
        newUser.setLoginUserName(loginUserName);
        newUser.setNickName(nickName);
        newUser.setLoginPassword(password);
        newUser.setLocked((byte) 0);

        if(adminUserService.register(newUser)){
            session.setAttribute("succussMsg","注册成功，请登录");
            return "redirect:/admin/login";
        }
        else {
            session.setAttribute("errorMsg","注册失败");
            return "redirect:/admin/register";
        }
    }



    @GetMapping("/login")//当用户通过浏览器访问时，会映射到Thymeleaf对应模板文件路径，将此模板渲染为HTML返回给客户端（浏览器）
    public String login() {
        return "admin/login";
    }



    //当用户通过浏览器访问 /admin/login（GET请求）时，会触发第一个@GetMapping("/login")方法，
    // 返回admin/login模板，渲染登录页面。
    //当用户提交登录表单（POST请求）时，会触发第二个@PostMapping("/login")方法，
    // 处理登录逻辑（验证用户名、密码等）。
    @PostMapping("/login")
    public String login(@RequestParam("userName") String userName,
                        @RequestParam("password") String password,
                        //@RequestParam("verifyCode") String verifyCode,
                        HttpSession session){
        if(StringUtils.isEmpty(userName) || StringUtils.isEmpty(password)){
            session.setAttribute("errorMsg","用户名或密码不能为空");
            return "admin/login";
        }
        /*if (StringUtils.isEmpty(verifyCode)) {
            session.setAttribute("errorMsg", "验证码不能为空");
            return "admin/login";
        }
        String kaptchaCode = session.getAttribute("verifyCode")+"";
        if (!kaptchaCode.equals(verifyCode)) {
            session.setAttribute("errorMsg","验证码错误");
        }*/

        AdminUser adminUser = adminUserService.login(userName, password);
        if (adminUser != null) {
            session.setAttribute("loginUser",adminUser.getNickName());
            session.setAttribute("loginUserId",adminUser.getAdminUserId());
            session.setMaxInactiveInterval(60*60*24);
            return "redirect:/admin/index";
        }
        else {
            session.setAttribute("errorMsg","登陆失败");
            return "admin/login";
        }

    }
    @GetMapping({"", "/", "/index", "/index.html"})
    public String index() {
        return "admin/index";
    }


    @GetMapping("/profile")
    public String profile(HttpServletRequest request) {
        //获取用户id
        int adminUserId = (int) request.getSession().getAttribute("loginUserId");
        AdminUser adminUser = adminUserService.getUserDetailById(adminUserId);
        if (adminUser == null) {
            return "admin/login";
        }
        request.setAttribute("path","/profile");
        request.setAttribute("loginUserName",adminUser.getLoginUserName());
        request.setAttribute("nickName",adminUser.getNickName());
        return "admin/profile";
    }

    @PostMapping("/profile/password")
    @ResponseBody//看自己写的笔记在平板上
    public String updatepassword(HttpServletRequest request,
                                 @RequestParam("originalPassword") String originalPassword,
                                 @RequestParam("newPassword") String newPassword){

        if (StringUtils.isEmpty(originalPassword)||StringUtils.isEmpty(newPassword)){
            return "参数不能为空";
        }
        Integer adminUserId = (int) request.getSession().getAttribute("loginUserId");
        if (adminUserService.updatePassword(adminUserId,originalPassword,newPassword)){
            request.getSession().removeAttribute("loginUserId");
            request.getSession().removeAttribute("loginUser");
            request.getSession().removeAttribute("errorMsg");
            return "success";
        }
        else {
            return "fail";
        }
    }


    @PostMapping("/profile/name")
    @ResponseBody
    public String nameUpdate(HttpServletRequest request, @RequestParam("loginUserName") String loginUserName,
                             @RequestParam("nickName") String nickName) {
        if (StringUtils.isEmpty(loginUserName) || StringUtils.isEmpty(nickName)) {
            return "参数不能为空";
        }
        Integer loginUserId = (int) request.getSession().getAttribute("loginUserId");
        if (adminUserService.updateName(loginUserId, loginUserName, nickName)) {
            return "success";
        } else {
            return "修改失败";
        }
    }


    @GetMapping("/logout")
    public String logout(HttpServletRequest request){
        request.getSession().removeAttribute("loginUserId");
        request.getSession().removeAttribute("loginUser");
        request.getSession().removeAttribute("errorMsg");
        return "admin/login";
    }




}
