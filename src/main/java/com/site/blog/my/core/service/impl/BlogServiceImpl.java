package com.site.blog.my.core.service.impl;

import com.site.blog.my.core.controller.vo.BlogDetailVO;
import com.site.blog.my.core.controller.vo.BlogListVO;
import com.site.blog.my.core.controller.vo.SimpleBlogListVO;
import com.site.blog.my.core.dao.BlogCategoryMapper;
import com.site.blog.my.core.dao.BlogMapper;
import com.site.blog.my.core.dao.BlogTagMapper;
import com.site.blog.my.core.dao.BlogTagRelationMapper;
import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.entity.BlogCategory;
import com.site.blog.my.core.entity.BlogTag;
import com.site.blog.my.core.entity.BlogTagRelation;
import com.site.blog.my.core.service.BlogService;
import com.site.blog.my.core.util.MarkDownUtil;
import com.site.blog.my.core.util.PageQueryUtil;
import com.site.blog.my.core.util.PageResult;
import com.site.blog.my.core.util.PatternUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl implements BlogService {

    @Autowired
    private BlogMapper blogMapper;
    @Autowired
    private BlogCategoryMapper categoryMapper;
    @Autowired
    private BlogTagMapper tagMapper;
    @Autowired
    private BlogTagRelationMapper blogTagRelationMapper;

    @Override
    @Transactional
    //前端提交
    //  → Controller参数校验
    //  → 构建Blog对象
    //  → Service层处理：
    //    1. 分类检查 → 更新分类排序
    //    2. 标签处理 → 插入新标签
    //    3. 插入博客
    //    4. 建立标签关系
    //  → 返回结果
    public String saveBlog(Blog blog) {
        //根据传入的Id查询分类
        BlogCategory blogCategory = categoryMapper.selectByPrimaryKey(blog.getBlogCategoryId());
        if (blogCategory == null) {
            //若分类不存在，设置为默认分类（ID=0，名称="默认分类"）。
            //若分类存在，更新该分类的排序值（categoryRank + 1）。
            blog.setBlogCategoryId(0);
            blog.setBlogCategoryName("默认分类");
        } else {
            //设置博客分类名称
            blog.setBlogCategoryName(blogCategory.getCategoryName());
            //分类的排序值加1
            blogCategory.setCategoryRank(blogCategory.getCategoryRank() + 1);
        }
        //处理标签数据
        String[] tags = blog.getBlogTags().split(",");
        if (tags.length > 6) {
            return "标签数量限制为6";
        }
        //保存文章
        if (blogMapper.insertSelective(blog) > 0) {
            //新增的tag对象
            List<BlogTag> tagListForInsert = new ArrayList<>();
            //所有的tag对象，用于建立关系数据
            List<BlogTag> allTagsList = new ArrayList<>();
            for (int i = 0; i < tags.length; i++) {
                BlogTag tag = tagMapper.selectByTagName(tags[i]);
                if (tag == null) {
                    //不存在就新增
                    BlogTag tempTag = new BlogTag();
                    tempTag.setTagName(tags[i]);
                    tagListForInsert.add(tempTag);
                } else {
                    allTagsList.add(tag);
                }
            }
            //新增标签数据并修改分类排序值
            if (!CollectionUtils.isEmpty(tagListForInsert)) {
                tagMapper.batchInsertBlogTag(tagListForInsert);
            }
            categoryMapper.updateByPrimaryKeySelective(blogCategory);
            List<BlogTagRelation> blogTagRelations = new ArrayList<>();
            //新增关系数据
            allTagsList.addAll(tagListForInsert);
            for (BlogTag tag : allTagsList) {
                BlogTagRelation blogTagRelation = new BlogTagRelation();
                blogTagRelation.setBlogId(blog.getBlogId());
                blogTagRelation.setTagId(tag.getTagId());
                blogTagRelations.add(blogTagRelation);
            }
            if (blogTagRelationMapper.batchInsert(blogTagRelations) > 0) {
                return "success";
            }
        }
        return "保存失败";
    }

    @Override
    public Blog getBlogById(Long blogId) {
        return blogMapper.selectByPrimaryKey(blogId);
    }

    @Override
    public String updateBlog(Blog blog) {
        Blog blogForUpdate = blogMapper.selectByPrimaryKey(blog.getBlogId());
        if (blogForUpdate == null) {
            return "数据不存在";
        }
        blogForUpdate.setBlogTitle(blog.getBlogTitle());
        blogForUpdate.setBlogSubUrl(blog.getBlogSubUrl());
        blogForUpdate.setBlogContent(blog.getBlogContent());
        blogForUpdate.setBlogCoverImage(blog.getBlogCoverImage());
        blogForUpdate.setBlogStatus(blog.getBlogStatus());
        blogForUpdate.setEnableComment(blog.getEnableComment());
        BlogCategory blogCategory = categoryMapper.selectByPrimaryKey(blog.getBlogCategoryId());
        if (blogCategory == null) {
            blogForUpdate.setBlogCategoryId(0);
            blogForUpdate.setBlogCategoryName("默认分类");
        }else {
            //设置博客分类名称

            blogForUpdate.setBlogCategoryName(blogCategory.getCategoryName());
            blogCategory.setCategoryId(blog.getBlogCategoryId());
            //分类的排序值加1
            blogCategory.setCategoryRank(blogCategory.getCategoryRank() + 1);
        }

        //处理标签数据

        //将标签字符串分割成数组，检查标签数量是否超过 6。
        //遍历标签数组，检查每个标签是否存在：
        //如果标签不存在，新增标签。
        //如果标签存在，获取标签的 ID。
        String[] tags = blog.getBlogTags().split(",");
        if (tags.length > 6) {
            return "标签数量限制为6";
        }
        blogForUpdate.setBlogTags(blog.getBlogTags());
        //新增的tag对象
        List<BlogTag> tagListForInsert = new ArrayList<>();
        List<BlogTag> allTagsList = new ArrayList<>();
        for (int i = 0; i < tags.length; i++) {
            BlogTag tag = tagMapper.selectByTagName(tags[i]);
            if (tag == null) {
                //不存在就新增
                BlogTag tempTag = new BlogTag();
                tempTag.setTagName(tags[i]);
                tagListForInsert.add(tempTag);
            }
            else {
                allTagsList.add(tag);
            }
        }
        //如果有新标签，批量插入新标签。
        if(!CollectionUtils.isEmpty(tagListForInsert)) {
            tagMapper.batchInsertBlogTag(tagListForInsert);
        }
        List<BlogTagRelation> blogTagRelations = new ArrayList<>();
        //新增关系数据
        allTagsList.addAll(tagListForInsert);
        for (BlogTag tag : allTagsList) {
            BlogTagRelation blogTagRelation = new BlogTagRelation();
            blogTagRelation.setBlogId(blog.getBlogId());
            blogTagRelation.setTagId(tag.getTagId());
            blogTagRelations.add(blogTagRelation);
        }
        //修改blog信息->修改分类排序值->删除原关系数据->保存新的关系数据
        categoryMapper.updateByPrimaryKeySelective(blogCategory);
        blogTagRelationMapper.deleteByBlogId(blog.getBlogId());
        blogTagRelationMapper.batchInsert(blogTagRelations);
        if (blogMapper.updateByPrimaryKeySelective(blogForUpdate)>0){
            return "success";
        }
        return "修改失败";



    }






    @Override
    public PageResult getBlogsPage(PageQueryUtil pageUtil) {
        List<Blog> blogList = blogMapper.findBlogList(pageUtil);
        int total = blogMapper.getTotalBlogs(pageUtil);
        PageResult pageResult = new PageResult(blogList,total,pageUtil.getLimit(),pageUtil.getPage());
        return pageResult;
    }

    @Override
    public List<SimpleBlogListVO> getBlogListForIndexPage(int type) {
        List<SimpleBlogListVO> simpleBlogListVOS = new ArrayList<>();
        List<Blog> blogs = blogMapper.findBlogListByType(type,9);
        if (!CollectionUtils.isEmpty(blogs)) {
            for (Blog blog : blogs) {
                SimpleBlogListVO simpleBlogListVO = new SimpleBlogListVO();
                BeanUtils.copyProperties(blog,simpleBlogListVO);
                simpleBlogListVOS.add(simpleBlogListVO);
            }
        }
        return simpleBlogListVOS;
    }

    @Override
    public PageResult getBlogsPageBySearch(String keyword, int page) {
        if (page > 0 && PatternUtil.validKeyword(keyword)) {
            Map param = new HashMap();
            param.put("page", page);
            param.put("limit", 9);
            param.put("keyword", keyword);
            param.put("blogStatus", 1);//过滤发布状态下的数据
            PageQueryUtil pageUtil = new PageQueryUtil(param);
            List<Blog> blogList = blogMapper.findBlogList(pageUtil);
            List<BlogListVO> blogListVOS = getBlogListVOsByBlogs(blogList);
            int total = blogMapper.getTotalBlogs(pageUtil);
            PageResult pageResult = new PageResult(blogListVOS, total, pageUtil.getLimit(), pageUtil.getPage());
            return pageResult;
        }
        return null;
    }


    //获取首页文章列表
    public PageResult getBlogsForIndexPage(int page){
        Map params = new HashMap();
        params.put("page",page);
        params.put("limit",10);
        params.put("blogStatus",1);
        PageQueryUtil pageUtil = new PageQueryUtil(params);
        List<Blog> blogs = blogMapper.findBlogList(pageUtil);
        List<BlogListVO> blogListVOS = getBlogListVOsByBlogs(blogs);
        int total = blogMapper.getTotalBlogs(pageUtil);
        PageResult pageResult = new PageResult(blogListVOS,total,pageUtil.getLimit(),pageUtil.getPage());
        return pageResult;
    }

    @Override
    public PageResult getBlogsPageByCategory(String categoryName, Integer page) {
        if (PatternUtil.validKeyword(categoryName)) {
            BlogCategory blogCategory = categoryMapper.selectByCategoryName(categoryName);
            if ("默认分类".equals(categoryName) && blogCategory == null) {
                blogCategory = new BlogCategory();
                blogCategory.setCategoryId(0);
            }
            if (blogCategory != null && page > 0) {
                Map param = new HashMap();
                param.put("page", page);
                param.put("limit", 9);
                param.put("blogCategoryId", blogCategory.getCategoryId());
                param.put("blogStatus", 1);//过滤发布状态下的数据
                PageQueryUtil pageUtil = new PageQueryUtil(param);
                List<Blog> blogList = blogMapper.findBlogList(pageUtil);
                List<BlogListVO> blogListVOS = getBlogListVOsByBlogs(blogList);
                int total = blogMapper.getTotalBlogs(pageUtil);
                PageResult pageResult = new PageResult(blogListVOS, total, pageUtil.getLimit(), pageUtil.getPage());
                return pageResult;
            }
        }
        return null;
    }

    @Override
    public PageResult getBlogsPageByTag(String tagName, int page) {
        if (PatternUtil.validKeyword(tagName)) {
            BlogTag tag = tagMapper.selectByTagName(tagName);
            if (tag != null && page > 0) {
                Map param = new HashMap();
                param.put("page", page);
                param.put("limit", 9);
                param.put("tagId", tag.getTagId());
                PageQueryUtil pageUtil = new PageQueryUtil(param);
                List<Blog> blogList = blogMapper.getBlogsPageByTagId(pageUtil);
                List<BlogListVO> blogListVOS = getBlogListVOsByBlogs(blogList);
                int total = blogMapper.getTotalBlogsByTagId(pageUtil);
                PageResult pageResult = new PageResult(blogListVOS, total, pageUtil.getLimit(), pageUtil.getPage());
                return pageResult;
            }
        }
        return null;
    }

    @Override
    public BlogDetailVO getBlogDetail(Long blogId) {
        Blog blog = blogMapper.selectByPrimaryKey(blogId);
        //不为空且状态为已发布
        BlogDetailVO blogDetailVO = getBlogDetailVO(blog);
        if (blogDetailVO != null) {
            return blogDetailVO;
        }
        return null;
    }

    private BlogDetailVO getBlogDetailVO(Blog blog) {

        if (blog != null && blog.getBlogStatus() == 1) {
            //增加浏览量
            blog.setBlogViews(blog.getBlogViews() + 1);
            blogMapper.updateByPrimaryKey(blog);
            BlogDetailVO blogDetailVO = new BlogDetailVO();
            BeanUtils.copyProperties(blog, blogDetailVO);
            //md格式转换
            blogDetailVO.setBlogContent(MarkDownUtil.mdToHtml(blogDetailVO.getBlogContent()));
            BlogCategory blogCategory = categoryMapper.selectByPrimaryKey(blog.getBlogCategoryId());
            if (blogCategory == null) {
                blogCategory = new BlogCategory();
                blogCategory.setCategoryId(0);
                blogCategory.setCategoryName("默认分类");
                blogCategory.setCategoryIcon("/admin/dist/img/category/00.png");
            }
            //分类信息
            blogDetailVO.setBlogCategoryIcon(blogCategory.getCategoryIcon());
            if (!StringUtils.isEmpty(blog.getBlogTags())) {
                //标签设置
                List<String> tags = Arrays.asList(blog.getBlogTags().split(","));
                blogDetailVO.setBlogTags(tags);
            }
            return blogDetailVO;
        }
        return null;
    }


    //数据填充
    private List<BlogListVO> getBlogListVOsByBlogs(List<Blog> blogList) {
        List<BlogListVO> blogListVOS = new ArrayList<>();
        if (!CollectionUtils.isEmpty(blogList)) {
            List<Integer> categoryIds = blogList.stream().map(Blog::getBlogCategoryId).collect(Collectors.toList());
            Map<Integer, String> blogCategoryMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(categoryIds)) {
                List<BlogCategory> blogCategories = categoryMapper.selectByCategoryIds(categoryIds);
                if (!CollectionUtils.isEmpty(blogCategories)) {
                    blogCategoryMap = blogCategories.stream().collect(Collectors.toMap(BlogCategory::getCategoryId, BlogCategory::getCategoryIcon, (key1, key2) -> key2));
                }
            }
            for (Blog blog : blogList) {
                BlogListVO blogListVO = new BlogListVO();
                BeanUtils.copyProperties(blog, blogListVO);
                if (blogCategoryMap.containsKey(blog.getBlogCategoryId())) {
                    blogListVO.setBlogCategoryIcon(blogCategoryMap.get(blog.getBlogCategoryId()));
                } else {
                    blogListVO.setBlogCategoryId(0);
                    blogListVO.setBlogCategoryName("默认分类");
                    blogListVO.setBlogCategoryIcon("/admin/dist/img/category/1.png");
                }
                blogListVOS.add(blogListVO);
            }
        }
        return blogListVOS;
    }



    @Override
    public boolean deleteBatch(Integer[] ids) {
        if (ids.length < 1) {
            return false;
        }
        return blogMapper.deleteBatch(ids) > 0;
    }
}
