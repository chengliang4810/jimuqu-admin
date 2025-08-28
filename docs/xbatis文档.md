## Save 新增数据

### Mapper 内置方法

- save(T)：实体类保存
- save(T,boolean)：实体类保存(boolean为是否null值也保存)
- save(Model)：Model类保存
- save(Model,boolean)：Model类保存(boolean为是否null值也保存)
- saveOrUpdate(T)：实体类保存或修改
- saveOrUpdate(T,boolean)：实体类保存或修改(boolean为是否null值也保存)
- saveOrUpdate(Model)：Model类保存或修改
- saveOrUpdate(Model,boolean)：Model类保存或修改(boolean为是否null值也保存)
- save(List<T>)：多个实体类保存(非批量操作)
- save(List<T>,boolean)：多个实体类保存(非批量操作)(boolean为是否null值也保存)
- saveModel(List<Model>)：多个Model类保存(非批量操作)
- saveModel(List<Model>,boolean)：多个Model类保存(非批量操作)(boolean为是否null值也保存)
- saveBatch(List<T>)：多个实体类保存(批量操作)
- saveBatch(List<T>,saveFields)：多个实体类，指定列保存(批量操作)
- saveModelBatch(List<Model>)：多个Model类保存(批量操作)
- saveModelBatch(List<Model>,saveFields)：多个Model类，指定列保存(批量操作)

### 单个对象示例
~~~java
 @Autowired
    private SysUserMapper sysUserMapper;
    
    public void save() {
        SysUser sysUser = new SysUser();
        sysUser.setUserName("demo");
        sysUserMapper.save(sysUser);
    }
~~~

### 批量新增示例
~~~java
  @Autowired
private SysUserMapper sysUserMapper;

public void saveBatch() {
    List<SysUser> sysUserList = new ArrayList<>();
    //sysUserList.add() ...
    sysUserMapper.saveBatch(sysUser);
}
~~~


## 修改数据(update)

### Mapper 内置方法

- update(T)：实体类修改
- update(T,boolean)：实体类修改(boolean为是否null值也修改)
- update(Model)：Model类修改
- update(Model,boolean)：Model类修改(boolean为是否null值也修改)
- saveOrUpdate(T)：实体类保存或修改
- saveOrUpdate(T,boolean)：实体类保存或修改(boolean为是否null值也修改)
- saveOrUpdate(Model)：Model类保存或修改
- saveOrUpdate(Model,boolean)：Model类保存或修改(boolean为是否null值也修改)
- update(T,forceUpdateFields)：实体类修改，可强制某些字段修改（null值会被替换成数据库里的NULL）
- update(Model,forceUpdateFields)：Model修改，可强制某些字段修改（null值会被替换成数据库里的NULL）
- update(T,Where)：根据where，实体类批量修改
- update(T,boolean,Where)：根据where，实体类批量修改(boolean为是否null值也修改)
- update(Model,Where)：根据where，Model类批量修改
- update(Model,boolean,Where)：根据where，Model类批量修改(boolean为是否null值也修改)
- update(List<T>)：多个实体类修改
- update(List<T>,boolean)：多个实体类修改(boolean为是否null值也修改)
- updateModel(List<Model>)：多个Model类修改
- updateModel(List<Model>,boolean)：多个Model类修改(boolean为是否null值也修改)
- update(List<T>,forceUpdateFields)：多个实体类修改，可强制某些字段修改（null值会被替换成数据库里的NULL）

### 单个对象根据主键修改示例
~~~java
  @Autowired
private SysUserMapper sysUserMapper;

public void update() {
    SysUser sysUser = new SysUser();
    sysUser.setId(1);
    sysUser.setUserName("demo");
    sysUserMapper.update(sysUser);
}
~~~

### 基于where,（批量）修改
~~~java
 @Autowired
private SysUserMapper sysUserMapper;

public void update() {
    SysUser sysUser=new SysUser();
    sysUser.setUserName("where UPDATE");
    sysUserMapper.update(sysUser,where -> {
        where.gt(SysUser::getId,100);
    });
}
~~~

### update 列自增,例如version=version +1
~~~java
  @Autowired
private SysUserMapper sysUserMapper;

public void update() {
    UpdateChain.of(sysUserMapper)
            .set(SysUser::getVersion, c -> c.plus(1))
            .eq(SysUser::getId, 1)
            .execute();
}
~~~

### 基于UpdateChain(强大)
~~~java
   @Autowired
private SysUserMapper sysUserMapper;

public void update() {
    UpdateChain.of(sysUserMapper)
            .update(SysUser.class)
            .set(SysUser::getUserName, "new userName")
            .eq(SysUser::getId, 1)
            .execute();
}
~~~

## 删除(delete)数据

### Mapper 内置方法

- deleteAll()：删除所有
- deleteById(id)：根据ID删除
- deleteById(ids)：多个ID删除
- delete(T)：一个实体类删除
- delete(List<T>)：多个实体类删除
- delete(Where)：动态条件删除

### 根据ID 删除

~~~java
  @Autowired
    private SysUserMapper sysUserMapper;
    
    public void delete() {
        sysUserMapper.deleteById(1);
    }
~~~


### 根据实体类删除


~~~java
    @Autowired
private SysUserMapper sysUserMapper;

public void delete() {
    SysUserModel sysUser = sysUserMapper.getById(1);
    sysUserMapper.delete(sysUser);
}
~~~

### 基于DeleteChain(强大)

~~~java
   @Autowired
private SysUserMapper sysUserMapper;

public void delete() {
    DeleteChain.of(sysUserMapper)
            .eq(SysUser::getId, 1)
            .execute();
}
~~~

### 基于where,（批量）删除

~~~java
@Autowired
    private SysUserMapper sysUserMapper;

    public void delete() {
        sysUserMapper.delete(where -> {
            where.gt(SysUser::getId,100);
        });
    }
~~~


## 查询

### Mapper 内置方法

- getById(id)：根据ID查询
- getById(id,selectFields)：根据ID查询,可选择部分列
- get(Where)：动态条件，单个查询

### ID 查询
~~~ java
public class Demo {
    @Autowired
    private SysUserMapper sysUserMapper;
    
    public void getById() {
       SysUser sysUser= sysUserMapper.getById(1);
    }
}
~~~

### 根据ID查询,可选择部分列
~~~ java
public class Demo {
    @Autowired
    private SysUserMapper sysUserMapper;
    
    public void getById() {
       SysUser sysUser= sysUserMapper.getById(1,SysUser::getId,SysUser::getUserName);
    }
}
~~~

### 动态where查询
~~~ java
public class Demo {
    @Autowired
    private SysUserMapper sysUserMapper;
    
    public void getById() {
       SysUser sysUser= sysUserMapper.get(where->{
           where.eq(SysUser::getId,1);
       });
    }
}
~~~

### 链路查询
~~~ java
SysUserRoleVo sysUserRoleVo = QueryChain.of(sysUserMapper)
    .select(SysUser.class,SysRole.class)
    .from(SysUser.class)
    .join(SysUser.class, SysRole.class,on->on.eq(SysUser::getRole_id,SysRole::getId))
    .eq(SysUser::getId,1)
    .like(SysUser::getUserName,"abc")
    .groupBy(SysUser::getId)
    .having(SysUser::getId,c->c.count().gt(0))
    .orderBy(SysUser::getId)
    .returnType(SysUserRoleVo.class)
    .get();
~~~

### 忽略null值，忽略空字符串，自动对字符串trim操作

~~~java
SysUser sysUser = QueryChain.of(sysUserMapper)
     // 忽略 null 条件参数    
     // 忽略 空字符串 条件参数 
     //  对字符串进行trim 去空格操作    
    .forSearch(true);
~~~

~~~java
SysUser sysUser = QueryChain.of(sysUserMapper)
    // 忽略 null 条件参数    
    .ignoreNullValueInCondition(true)
    // 忽略 空字符串 条件参数    
    .ignoreEmptyInCondition(true)
    //  对字符串进行trim 去空格操作    
    .trimStringInCondition(true);
~~~