package com.site.blog.my.core.service;

import com.site.blog.my.core.controller.vo.BlogDetailVO;
import com.site.blog.my.core.controller.vo.SimpleBlogListVO;
import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.util.PageQueryUtil;
import com.site.blog.my.core.util.PageResult;

import java.util.List;

public interface BlogService {
    String saveBlog(Blog blog);

    Blog getBlogById(Long blogId);

    String updateBlog(Blog blog);

    PageResult getBlogsPage(PageQueryUtil pageUtil);

    List<SimpleBlogListVO> getBlogListForIndexPage(int type);


    PageResult getBlogsPageBySearch(String keyword, int page);

    boolean deleteBatch(Integer[] ids);

    PageResult getBlogsForIndexPage(int pageNum);


    PageResult getBlogsPageByCategory(String categoryName, Integer page);

    PageResult getBlogsPageByTag(String tagName, int page);

    BlogDetailVO getBlogDetail(Long blogId);
}
