## 功能索引

### 登录模块

**登录模块通过三层架构（Controller → Service → Mapper）实现，核心流程为：**

- 参数校验 → 密码加密 → 数据库验证 → Session 管理 → 结果重定向

------

#### **1. 前端页面渲染**

- **路径**：`GET /admin/login`
- **实现**：`AdminController` 的 `@GetMapping("/login")` 方法返回 `admin/login` 模板，渲染登录页面

------

#### **2. 登录表单提交**

- **路径**：`POST /admin/login`
- **参数**：`userName`、`password`

**流程：**

1. **参数校验**：检查 `userName` 及 `password` 是否为空。若为空，设置错误消息 `errorMsg` 并返回登录页面。
2. **密码加密**：在 `AdminUserServiceImpl.login` 方法中，使用 `MD5Util` 对用户输入的密码进行加密。
3. **数据库验证**：调用 `AdminUserMapper.login`，执行 SQL 查询
4. **Session 管理**：
   - 若验证成功，将用户昵称 (`loginUser`) 和 ID (`loginUserId`) 存入 Session，并设置会话有效期（24 小时）。
   - 若失败，设置错误消息 `errorMsg` 并返回登录页面。
5. **结果处理**：
   - 成功则重定向到首页 (`/admin/index`)，失败则返回登录页。
###效果如下：
![image](https://github.com/zktlkhjp/My_Project/blob/master/pic/57f55b81cd8e0109dfb090f0a4c6678.png)
------

### 注册模块

**流程**：页面渲染 → 参数校验 → 用户名唯一性检查 → 密码加密 → 数据插入 → 结果反馈。

------

#### **1. 前端页面渲染**

- **路径**：`GET /admin/register`
- **实现**：`AdminController` 的 `@GetMapping("/register")` 方法返回 `admin/register` 模板，渲染注册页面。

------

#### **2. 注册表单提交**

- **路径**：`POST /admin/register`
- **参数**：`loginUserName`（用户名）、`password`（密码）、`nickName`（昵称）

**流程：**

1. **参数校验**：检查 `loginUserName`、`password`、`nickName` 是否为空。若为空，设置错误消息 `errorMsg` 并重定向回注册页面。
2. **用户名唯一性检查**：调用 `adminUserService.isUsernameExists(loginUserName)`，检查用户名是否已存在。若存在，设置错误消息 `errMsg` 并重定向回注册页面。
3. **密码加密**：在 `AdminUserServiceImpl.register` 方法中，使用 `MD5Util` 对用户输入的密码进行加密。
4. **数据持久化**：创建 `AdminUser` 对象，设置字段值（包括加密后的密码），调用 `adminUserMapper.insertSelective` 插入数据库。
5. **结果反馈**：
   - 注册成功：设置成功消息 `successMsg`，重定向到登录页面 (`/admin/login`)。
   - 注册失败：设置错误消息 `errorMsg`，重定向回注册页面。
![image](https://github.com/zktlkhjp/My_Project/blob/master/pic/1747150011219.png)
------

### 用户个人资料管理模块

#### **1. 查看个人资料**

- **路径**：`GET /admin/profile`

**流程：**

1. **会话验证**：从 `HttpSession` 中获取当前登录用户的 ID (`loginUserId`)。
2. **查询用户信息**：调用 `adminUserService.getUserDetailById(adminUserId)`，通过 `AdminUserMapper.selectByPrimaryKey` 查询用户详细信息。
3. **数据传递**：将用户登录名 (`loginUserName`)、昵称 (`nickName`) 和路径标识 (`path`) 存入请求属性，渲染 `admin/profile` 模板。

------

#### **2. 修改密码**

- **路径**：`POST /admin/profile/password`
- **参数**：`originalPassword`（原密码）、`newPassword`（新密码）

**流程：**

1. **参数校验**：检查原密码和新密码是否为空。若为空，返回 `参数不能为空`。
2. **会话验证**：从 `HttpSession` 中获取用户 ID (`loginUserId`)。
3. **密码验证与更新**：
   - 调用 `adminUserService.updatePassword`，验证原密码的 MD5 加密值是否与数据库一致。
   - 若一致，对新密码进行 MD5 加密并更新数据库。
4. **结果处理**：
   - 成功：清除会话中的用户信息（强制登出），返回 `success`。
   - 失败：返回 `fail`。

------

#### **3. 修改用户名和昵称**

- **路径**：`POST /admin/profile/name`
- **参数**：`loginUserName`（新用户名）、`nickName`（新昵称）

**流程：**

1. **参数校验**：检查用户名和昵称是否为空。若为空，返回 `参数不能为空`。
2. **会话验证**：从 `HttpSession` 中获取用户 ID (`loginUserId`)。
3. **更新用户信息**：调用 `adminUserService.updateName`，直接更新数据库中的用户名和昵称。
4. **结果处理**：
   - 成功：返回 `success`。
   - 失败：返回 `修改失败`。
![image](https://github.com/zktlkhjp/My_Project/blob/master/pic/666bc40ffb7099cdbceaba09a54020a.png)
------

### 分类管理模块

#### **1. 分类列表分页查询**

##### 前端页面请求分页数据

- 在 `category.html` 页面中，通过 jqGrid 插件初始化表格，并设置其 `url` 属性为 `/admin/categories/list`，这是获取分页数据的接口地址。
- 用户在前端页面通过操作（如点击分页按钮等）触发 jqGrid 加载数据，此时会向后端发送包含分页参数（页码 `page`、每页条数 `limit` 等）的 HTTP GET 请求。

##### 后端接收分页请求参数

- 在 `CategoryController` 控制器的 `/categories/list` 请求处理方法中，使用 `@RequestParam` 注解接收前端传来的请求参数，并将这些参数存储在 `Map` 中。
- 判断 `page` 和 `limit` 参数是否为空，若为空则返回参数异常的错误结果。

##### 构造分页查询条件

- 将接收到的参数封装成 `PageQueryUtil` 对象，该对象对分页参数进行了一些处理，如计算起始位置等，为后续的数据库分页查询做准备。

##### 调用服务层进行分页查询

- 控制器调用 `CategoryService` 服务层的 `getBlogCategoryPage` 方法，并将 `PageQueryUtil` 对象作为参数传递，由服务层负责具体的业务逻辑处理和数据查询操作。

##### 数据库分页查询操作

- 在 `CategoryServiceImpl` 服务实现类的 `getBlogCategoryPage` 方法中，调用 `BlogCategoryMapper` 数据访问层的 `findCategoryList` 方法和 `getTotalCategories` 方法。
- `findCategoryList` 方法根据传入的 `PageQueryUtil` 对象中的分页参数，执行数据库查询操作获取分页后的分类数据列表。
- `getTotalCategories` 方法查询满足条件的分类数据的总记录数。

##### 返回分页结果给前端

- 根据查询到的分类数据列表和总记录数，构造 `PageResult` 对象，该对象包含了分页数据、总记录数、当前页码、每页条数等信息。
- 将 `PageResult` 对象作为响应结果返回给前端，前端接收到数据后，jqGrid 会根据返回的数据更新表格的显示内容，呈现出分页后的分类列表。
![image](https://github.com/zktlkhjp/My_Project/blob/master/pic/f59dedf23ff02691e375fa06b00f4af.png)
------

#### **2. 分类列表新增功能**
![image](https://github.com/zktlkhjp/My_Project/blob/master/pic/0a1effc069cd4c7cea16fa1716aec70.png)

##### 前端页面请求新增分类

- 在 `category.html` 页面中，用户点击 “新增” 按钮，触发 `categoryAdd()` 函数。
- `categoryAdd()` 函数重置表单数据，设置模态框标题为 “分类添加”，并显示模态框 `#categoryModal`。
- 用户在模态框中填写分类名称，点击 “图标切换” 按钮随机选择图标。
- 用户点击模态框中的 “确认” 按钮（`#saveButton`）时，触发表单验证，若验证通过，则将表单数据通过 `$("#categoryForm").serialize()` 序列化，并发送 POST 请求到 `/admin/categories/save` 接口。

![image](https://github.com/zktlkhjp/My_Project/blob/master/pic/6c22e8570572ecefb33c37f9c54bfc3.png)

**后端接收新增请求**
- 在 `CategoryController` 控制器的 `/categories/save` 请求映射方法中，使用 `@RequestParam` 注解接收前端传来的分类名称 `categoryName` 和分类图标 `categoryIcon` 参数。
- 检查 `categoryName` 和 `categoryIcon` 是否为空，若为空则返回相应的错误结果。

##### 构造分类保存逻辑

- 将接收到的参数封装成 `BlogCategory` 对象，为后续的数据库保存操作做准备。

##### 调用服务层进行分类保存

- 控制器调用 `CategoryService` 服务层的 `saveCategory` 方法，并将分类名称和图标作为参数传递，由服务层负责具体的业务逻辑处理和数据保存操作。

##### 数据库分类保存操作

- 在 `CategoryServiceImpl` 服务实现类的 `saveCategory` 方法中，调用 `BlogCategoryMapper` 数据访问层的 `insertSelective` 方法。
- `insertSelective` 方法将新的分类数据插入到数据库中。

##### 返回保存结果给前端

- 根据分类保存操作是否成功，构造相应的结果对象，若成功则返回成功结果，前端会关闭模态框、显示成功提示并刷新列表；若失败则返回失败结果，前端显示相应的错误提示。

------

#### **3. 分类列表更新功能**
![image](https://github.com/zktlkhjp/My_Project/blob/master/pic/0a1effc069cd4c7cea16fa1716aec70.png)

##### 前端页面请求更新分类

- 在 `category.html` 页面中，用户点击 “修改” 按钮，触发 `categoryEdit()` 函数。
- `categoryEdit()` 函数通过 `getSelectedRow()` 获取选中的分类 ID，并发送 GET 请求到 `/admin/categories/info/{id}` 接口获取分类详情。
- 将获取到的分类详情数据填充到模态框 `#categoryModal` 中，用户可以在模态框中修改分类名称和图标。
- 用户点击模态框中的 “确认” 按钮（`#saveButton`）时，触发表单验证，若验证通过，则将表单数据通过 `$("#categoryForm").serialize()` 序列化，并发送 POST 请求到 `/admin/categories/update` 接口。

![image](https://github.com/zktlkhjp/My_Project/blob/master/pic/0b8bc6719d3f45dd8e07de4ac3f039c.png)

##### 后端接收更新请求
- 在 `CategoryController` 控制器的 `/categories/update` 请求映射方法中，使用 `@RequestParam` 注解接收前端传来的分类 ID `categoryId`、分类名称 `categoryName` 和分类图标 `categoryIcon` 参数。
- 检查 `categoryId` 是否合法（不为 `null` 且大于 0），并检查 `categoryName` 和 `categoryIcon` 是否为空，若为空则返回相应的错误结果。

##### 构造分类更新逻辑

- 将接收到的参数封装成 `BlogCategory` 对象，为后续的数据库更新操作做准备。

##### 调用服务层进行分类更新

- 控制器调用 `CategoryService` 服务层的 `updateCategory` 方法，并将分类 ID、分类名称和图标作为参数传递，由服务层负责具体的业务逻辑处理和数据更新操作。

##### 数据库分类更新操作

- 在 `CategoryServiceImpl` 服务实现类的 `updateCategory` 方法中，调用 `BlogCategoryMapper` 数据访问层的 `updateByPrimaryKeySelective` 方法。
- `updateByPrimaryKeySelective` 方法根据分类 ID 更新分类数据。

##### 返回更新结果给前端

- 根据分类更新操作是否成功，构造相应的结果对象，若成功则返回成功结果，前端会关闭模态框、显示成功提示并刷新列表；若失败则返回失败结果，前端显示相应的错误提示。

------

#### **4. 分类详情功能**

##### 前端页面请求分类详情
![image](https://github.com/zktlkhjp/My_Project/blob/master/pic/0b8bc6719d3f45dd8e07de4ac3f039c.png)
- 在 `category.html` 页面中，用户点击 “修改” 按钮，触发 `categoryEdit()` 函数。
- `categoryEdit()` 函数通过 `getSelectedRow()` 获取选中的分类 ID，并发送 GET 请求到 `/admin/categories/info/{id}` 接口获取分类详情。
- 将获取到的分类详情数据填充到模态框 `#categoryModal` 中，用户可以在模态框中查看和修改分类信息。

##### 后端接收详情请求

- 在 `CategoryController` 控制器的 `/categories/info/{id}` 请求映射方法中，接收前端传来的分类 ID 参数。
- 检查分类 ID 是否合法（不为 `null` 且大于 0），若不合法则返回相应的错误结果。

##### 调用服务层查询分类详情

- 控制器调用 `CategoryService` 服务层的 `selectById` 方法，并将分类 ID 作为参数传递，由服务层负责具体的业务逻辑处理和数据查询操作。

##### 数据库查询操作

- 在 `CategoryServiceImpl` 服务实现类的 `selectById` 方法中，调用 `BlogCategoryMapper` 数据访问层的 `selectByPrimaryKey` 方法。
- `selectByPrimaryKey` 方法根据分类 ID 查询分类详情。

##### 返回查询结果给前端

- 根据查询操作是否成功，构造相应的结果对象，若成功则返回成功结果，前端会将查询到的分类详情数据填充到模态框中；若失败则返回失败结果，前端显示相应的错误提示。

------

#### **5. 删除分类功能**

##### 前端页面请求删除分类

- 在 `category.html` 页面中，用户点击 “删除” 按钮，触发 `deleteCagegory()` 函数。
- `deleteCagegory()` 函数通过 `getSelectedRows()` 获取选中的分类 ID 数组。
- 使用 SweetAlert 弹出确认对话框，询问用户是否确认删除。
- 如果用户确认删除，则将选中的分类 ID 数组通过 POST 请求发送到 `/admin/categories/delete` 接口。

##### 后端接收删除请求

- 在 `CategoryController` 控制器的 `/categories/delete` 请求映射方法中，接收前端传来的分类 ID 数组。
- 检查分类 ID 数组是否为空或长度小于 1，若为空则返回相应的错误结果。

##### 调用服务层进行分类删除

- 控制器调用 `CategoryService` 服务层的 `deleteBatch` 方法，并将分类 ID 数组作为参数传递，由服务层负责具体的业务逻辑处理和数据删除操作。

##### 数据库删除操作

- 在 `CategoryServiceImpl` 服务实现类的 `deleteBatch` 方法中，调用 `BlogCategoryMapper` 数据访问层的 `deleteBatch` 方法。
- `deleteBatch` 方法根据分类 ID 数组批量删除分类数据。

##### 返回删除结果给前端

- 根据删除操作是否成功，构造相应的结果对象，若成功则返回成功结果，前端会显示删除成功提示并刷新列表；若失败则返回失败结果，前端显示相应的错误提示。

------

### 标签模块

#### **1. 标签分页列表模块**

##### 前端页面请求分页数据

- 在 `tag.html` 页面中，通过 jqGrid 插件初始化表格，并设置其 `url` 属性为 `/admin/tags/list`，这是获取分页数据的接口地址。
- 用户在前端页面通过操作（如点击分页按钮等）触发 jqGrid 加载数据，此时会向后端发送包含分页参数（页码 `page`、每页条数 `limit` 等）的 HTTP GET 请求。

##### 后端接收分页请求参数

- 在 `TagController` 控制器的 `/tags/list` 请求映射方法中，使用 `@RequestParam` 注解接收前端传来的请求参数，并将这些参数存储在 `Map` 中。
- 判断 `page` 和 `limit` 参数是否为空，若为空则返回参数异常的错误结果。

##### 构造分页查询条件

- 将接收到的参数封装成 `PageQueryUtil` 对象，该对象对分页参数进行了一些处理，如计算起始位置等，为后续的数据库分页查询做准备。

##### 调用服务层进行分页查询

- 控制器调用 `TagService` 服务层的 `getBlogTagPage` 方法，并将 `PageQueryUtil` 对象作为参数传递，由服务层负责具体的业务逻辑处理和数据查询操作。

##### 数据库分页查询操作

- 在 `TagServiceImpl` 服务实现类的 `getBlogTagPage` 方法中，调用 `BlogTagMapper` 数据访问层的 `findTagList` 方法和 `getTotalTags` 方法。
- `findTagList` 方法根据传入的 `PageQueryUtil` 对象中的分页参数，执行数据库查询操作获取分页后的标签数据列表。
- `getTotalTags` 方法查询满足条件的标签数据的总记录数。

##### 返回分页结果给前端

- 根据查询到的标签数据列表和总记录数，构造 `PageResult` 对象，该对象包含了分页数据、总记录数、当前页码、每页条数等信息。
- 将 `PageResult` 对象作为响应结果返回给前端，前端接收到数据后，jqGrid 会根据返回的数据更新表格的显示内容，呈现出分页后的标签列表。

------

#### **2. 新增标签模块**

##### 前端页面请求新增标签

- 在 `tag.html` 页面中，用户点击 “新增” 按钮，触发 `tagAdd()` 函数。
- `tagAdd()` 函数重置表单数据，设置模态框标题为 “标签添加”，并显示模态框 `#tagModal`。
- 用户在模态框中填写标签名称。
- 用户点击模态框中的 “确认” 按钮（`#saveButton`）时，触发表单验证，若验证通过，则将表单数据通过 `$("#tagForm").serialize()` 序列化，并发送 POST 请求到 `/admin/tags/save` 接口。

##### 后端接收新增请求

- 在 `TagController` 控制器的 `/tags/save` 请求映射方法中，使用 `@RequestParam` 注解接收前端传来的标签名称 `tagName` 参数。
- 检查 `tagName` 是否为空，若为空则返回相应的错误结果。

##### 构造标签保存逻辑

- 将接收到的参数封装成 `BlogTag` 对象，为后续的数据库保存操作做准备。

##### 调用服务层进行标签保存

- 控制器调用 `TagService` 服务层的 `saveTag` 方法，并将标签名称作为参数传递，由服务层负责具体的业务逻辑处理和数据保存操作。

##### 数据库标签保存操作

- 在 `TagServiceImpl` 服务实现类的 `saveTag` 方法中，调用 `BlogTagMapper` 数据访问层的 `insertSelective` 方法。
- `insertSelective` 方法将新的标签数据插入到数据库中。

##### 返回保存结果给前端

- 根据标签保存操作是否成功，构造相应的结果对象，若成功则返回成功结果，前端会关闭模态框、显示成功提示并刷新列表；若失败则返回失败结果，前端显示相应的错误提示。

------

#### **3. 删除标签功能**

##### 前端页面请求删除标签

- 在 `tag.html` 页面中，用户点击 “删除” 按钮，触发 `deleteTag()` 函数。
- `deleteTag()` 函数通过 `getSelectedRows()` 获取选中的标签 ID 数组。
- 使用 SweetAlert 弹出确认对话框，询问用户是否确认删除。
- 如果用户确认删除，则将选中的标签 ID 数组通过 POST 请求发送到 `/admin/tags/delete` 接口。

##### 后端接收删除请求

- 在 `TagController` 控制器的 `/tags/delete` 请求映射方法中，接收前端传来的标签 ID 数组。
- 检查标签 ID 数组是否为空或长度小于 1，若为空则返回相应的错误结果。

##### 调用服务层进行标签删除

- 控制器调用 `TagService` 服务层的 `deleteBatch` 方法，并将标签 ID 数组作为参数传递，由服务层负责具体的业务逻辑处理和数据删除操作。

##### 数据库删除操作

- 在 `TagServiceImpl` 服务实现类的 `deleteBatch` 方法中，调用 `BlogTagMapper` 数据访问层的 `deleteBatch` 方法。
- `deleteBatch` 方法根据标签 ID 数组批量删除标签数据。

##### 返回删除结果给前端

- 根据删除操作是否成功，构造相应的结果对象，若成功则返回成功结果，前端会显示删除成功提示并刷新列表；若失败则返回失败结果，前端显示相应的错误提示。

------

### 博客模块

#### **博客分页展示功能**

##### 前端请求分页数据

- 在 `blog.html` 页面中，通过 jqGrid 插件初始化表格，设置 `url` 为 `/admin/blogs/list`。
- 用户操作触发 jqGrid 加载数据，向后端发送包含分页参数（页码 `page`、每页条数 `limit`）的 HTTP GET 请求。

------

##### 后端接收请求参数

- `BlogController` 的 `/blogs/list` 方法接收参数，使用 `@RequestParam` 注解获取前端传来的请求参数并存储在 `Map` 中。
- 判断 `page` 和 `limit` 参数是否为空，若为空返回参数异常的错误结果。

------

##### 构造分页查询条件

- 将接收的参数封装成 `PageQueryUtil` 对象，为后续数据库分页查询做准备。

------

##### 调用服务层进行分页查询

- 控制器调用 `BlogService` 的 `getBlogsPage` 方法，传递 `PageQueryUtil` 对象。
- 服务层实现类 `BlogServiceImpl` 的 `getBlogsPage` 方法调用 `BlogMapper` 的 `findBlogList` 和 `getTotalBlogs` 方法，获取分页后的博客数据列表及总记录数。

------

##### 返回分页结果给前端

- 根据查询结果构造 `PageResult` 对象，包含分页数据、总记录数等信息，作为响应结果返回给前端。
- 前端接收到数据后，jqGrid 更新表格显示内容，呈现分页后的博客列表。

------

#### **新增博客功能**

------

#### **1. Controller层（接收请求与参数校验）**

- **接口定义**：`@PostMapping("/blogs/save")` 接收前端提交的博客数据，包括标题、分类 ID、标签、内容、封面图等参数。
- **参数校验**：
  - 标题必填，且长度不超过 150 字符。
  - 标签必填，且数量不超过 6 个，总长度不超过 10 万字符。
  - 封面图路径必填，防止空链接。
  - 子 URL、分类 ID 等字段做非空或格式校验。
- **请求处理**：
  - 校验通过后，将参数封装为 `Blog` 对象，调用 `BlogService.saveBlog()` 处理业务逻辑。
  - 若校验失败，直接返回错误提示。

------

#### **2. Service层（业务逻辑处理）**

------

##### **1. 分类处理逻辑**

- **目标**：确保博客关联的分类有效，并维护分类的热度排序。

**步骤：**

1. **查询分类**：根据传入的 `blogCategoryId` 从数据库中查找分类。
2. **分类不存在的处理**：
   - 将博客的 `blogCategoryId` 设为 `0`（默认分类 ID）。
   - 设置分类名称为 “默认分类”。
3. **分类存在的处理**：
   - 更新分类的 `categoryRank`（排序值）加 1（用于统计分类下文章数量或热度）。
   - 将分类名称同步到博客对象中（便于后续展示）。

------

##### **2. 标签处理逻辑**

- **目标**：确保标签合规（数量 ≤ 6），并维护标签库的唯一性。

**步骤：**

1. **分割标签**：将 `blogTags` 字符串按逗号分割为数组。
2. **标签数量校验**：若超过 6 个，直接返回错误（防止滥用）。
3. **标签去重与新增**：
   - 遍历每个标签，检查是否已存在于 `tb_blog_tag` 表。
   - **不存在的标签**：插入新记录到 `tb_blog_tag` 表。
   - **已存在的标签**：收集到列表中，用于后续关联。

------

##### **3. 博客主表写入**

- **目标**：将博客内容持久化到数据库。

**操作：**

- 调用 `blogMapper.insertSelective(blog)` 插入博客数据。
- **关键点**：`insertSelective` 仅插入非空字段，避免覆盖默认值。

------

##### **4. 标签关联关系建立**

- **目标**：将博客与所有标签（新旧）建立多对多关联。

**步骤：**

1. **合并标签列表**：将新插入的标签和已存在的标签合并为一个列表。
2. **构建关联关系**：
   - 遍历合并后的标签列表，为每个标签创建 `BlogTagRelation` 对象。
   - 关联关系包含 `blogId`（当前博客 ID）和 `tagId`（标签 ID）。
3. **批量插入关联关系**：
   - 调用 `blogTagRelationMapper.batchInsert()` 一次性插入所有关联关系。

------

#### **博客修改功能**

------

##### **1. 访问编辑页面（GET /blogs/edit/{blogId}）**

- **目标**：根据博客 ID 加载详情，渲染编辑页面。

**步骤：**

1. **路由匹配**：用户访问 `/blogs/edit/{blogId}`（如 `/blogs/edit/123`），触发 `BlogController.edit` 方法。
2. **查询博客**：调用 `blogService.getBlogById(blogId)`，从数据库获取指定 ID 的博客。
3. **数据校验**：若博客不存在，跳转到错误页面（如 404）。
4. **传递数据**：
   - 将 `blog` 对象存入请求作用域（`request.setAttribute("blog", blog)`）。
   - 获取所有分类列表（`categoryService.getAllCategories()`），用于渲染分类选择下拉框。
5. **渲染页面**：返回 `admin/edit.html` 模板，Thymeleaf 将数据填充到表单字段。

------

##### **2. 页面数据回显（编辑页面渲染 resources/templates/admin/edit.html）**

- **目标**：将查询到的博客详情显示在表单中。

**关键实现：**

- **隐藏域**：`<input type="hidden" th:value="${blog.blogId}">` 存储博客 ID。
- **标题输入**：`<input th:value="${blog.blogTitle}">` 预填标题。
- **分类选择**：`<select>` 通过 Thymeleaf 遍历分类列表，选中当前博客的分类。
- **标签输入**：`<input th:value="${blog.blogTags}">` 预填标签（逗号分隔）。
- **内容编辑器**：`<textarea th:utext="${blog.blogContent}">` 加载 Markdown 内容。
- **封面图**：`<img th:src="${blog.blogCoverImage}">` 显示当前封面图。

------

##### **3. 表单提交与验证（前端 JS 逻辑）**

- **目标**：收集用户修改后的数据，发送到后端。

**关键步骤：**

1. **数据收集**：
   - 获取表单字段值（标题、分类 ID、标签、内容、封面图等）。
   - 通过 `blogEditor.getMarkdown()` 获取 Markdown 编辑器的内容。
2. **验证规则**：
   - 标题、分类、标签、内容、封面图必填。
   - 标题和路径长度限制（≤150 字符），标签数量限制（≤6 个）。
   - 封面图路径必须有效（非默认占位图）。
3. **请求构建**：
   - 若 `blogId > 0`，调用 `/blogs/update` 接口（修改操作）。
   - 否则调用 `/blogs/save`（新增操作）。
4. **Ajax 提交**：
   - 发送 POST 请求，携带 JSON 数据。
   - 成功时提示 “修改成功”，失败时显示错误信息。

------

##### **4. 后端接口处理（POST /blogs/update）**

- **目标**：接收前端数据，更新数据库。

**关键实现：**

1. **参数校验**：
   - 检查必填字段（标题、分类、标签等）。
   - 校验标题、路径、标签长度。
2. **构建 Blog 对象**：
   - 将前端参数封装为 `Blog` 对象（如 `blog.setBlogTitle(title)`）。
3. **调用 Service**：`blogService.updateBlog(blog)`，执行业务逻辑。

------

##### **5. 业务层处理（Service 层）**

- **目标**：处理分类、标签、博客的更新逻辑。

**关键步骤：**

1. **查询原博客**：`blogMapper.selectByPrimaryKey(blogId)`，确保数据存在。
2. **更新博客主表**：
   - 直接覆盖标题、内容、状态等字段（`blogMapper.updateByPrimaryKeySelective(blog)`）。
3. **处理分类**：
   - 若分类变更，原分类排序值减 1，新分类排序值加 1。
4. **处理标签**：
   - **删除旧关联**：`blogTagRelationMapper.deleteByBlogId(blogId)`，移除原有标签关系。
   - **新增新标签**：
     - 分割标签字符串，过滤重复标签（最多 6 个）。
     - 不存在的标签批量插入到 `tb_blog_tag` 表。
   - **重建关联**：将新标签与博客 ID 关联，批量插入到 `tb_blog_tag_relation` 表。

------

#### **博客删除功能**

------

##### **1. 前端触发删除操作**

- **用户行为**：在博客管理页面（如后台列表页）勾选一篇或多篇待删除文章，点击 “批量删除” 按钮。

**交互方式：**

- 前端通过 JavaScript 收集选中的文章 ID（如 `[1, 2, 3]`）。
- 发送 HTTP 请求到后端接口（如 `/blogs/delete`），通常使用 Ajax 实现异步操作。

------

##### **2. 控制器接收请求**

- **路由定义**：后端定义接口路径和请求方法



复制

```
@PostMapping("/blogs/delete")
@ResponseBody
public Result delete(@RequestBody Integer[] ids) {
    // 处理逻辑
}
```

**核心逻辑：**

1. **参数校验**：检查传入的 ID 数组是否为空或无效。
2. **调用 Service 层**：将 ID 数组传递给业务逻辑层处理（如 `blogService.deleteBatch(ids)`）。
3. **返回结果**：根据操作结果返回成功或失败信息（如 JSON 格式 `{code: 200, message: "删除成功"}`）。

------

##### **3. 业务层处理**

- **事务控制**：使用 `@Transactional` 确保批量删除操作的原子性。

**核心逻辑：**

1. **参数有效性检查**：若 ID 数组为空，直接返回失败。
2. **调用 Mapper 层**：执行数据库删除操作（如 `blogMapper.deleteBatch(ids)`）。
3. **返回结果**：若数据库操作成功，返回 `true`；否则回滚事务并返回 `false`。
