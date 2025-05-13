package com.site.blog.my.core.controller.admin;

import com.site.blog.my.core.service.TagService;
import com.site.blog.my.core.util.PageQueryUtil;
import com.site.blog.my.core.util.Result;
import com.site.blog.my.core.util.ResultGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;


@Controller
@RequestMapping("/admin")
public class TagController {

    @Autowired
    private TagService tagService;


    @GetMapping("/tags")
    public String tagPage(HttpServletRequest request) {
        return "admin/tag";
    }

    @GetMapping("/tags/list")
    @ResponseBody
    public Result list(@RequestParam Map<String, Object> params) {
        if (StringUtils.isEmpty(params.get("page")) || StringUtils.isEmpty(params.get("limit"))) {
            return ResultGenerator.genFailResult("参数异常！");
        }
        PageQueryUtil pageUtil = new PageQueryUtil(params);
        return ResultGenerator.genSuccessResult(tagService.getBlogTagPage(pageUtil));
    }

    @PostMapping("/tags/save")
    @ResponseBody
    public Result save(@RequestParam("tagName") String tagName){
        if (StringUtils.isEmpty(tagName)){
            return ResultGenerator.genFailResult("参数异常");
        }
        if (tagService.saveTag(tagName)){
            return ResultGenerator.genSuccessResult();
        }
        else {
            return ResultGenerator.genFailResult("添加失败");
        }
    }


    @PostMapping("/tags/delete")
    @ResponseBody
    public Result delete(@RequestBody Integer[]tagId){
        if(tagId.length<1){
            return ResultGenerator.genFailResult("非法参数");
        }
        if (tagService.deleteBatch(tagId)){
            return ResultGenerator.genSuccessResult();
        }
        else {
            return ResultGenerator.genFailResult("删除失败");
        }
    }







}
