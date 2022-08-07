## 命名规范
1. java采用驼峰式命名规则，原则上不允许出现任何带下划线的类名、变量名、方法名
2. 类名首字母大写
3. 变量名首字母小写

## 确定请求类型、请求格式
具体参考Restful API 设计规范： http://www.ruanyifeng.com/blog/2014/05/restful_api.html 
1. 查询类请求使用get，参数放在url中
2. 需要提交数据，更新数据库的请求使用post，参数放在body中（由于有些浏览器不支持PUT DELETE PATCH，所以Restful API规范中提到的这些，也用post实现）
3. web端如果需要往前端传输数据，使用的数据格式，应与domain中的实体对应，以Map或List的格式把各个实体组织起来返回
4. 前端给web端传输的数据（post请求），要么与domain中的实体对应，要么新建对应的Form对象


## Controller 开发
1. controller不直接与DAO层接触，除非非常简单的操作，如findAll等
2. 每一个controller的返回值，可以是domain中的对象或者对应的form，或者由list或者map组装的domain或form中的对象，也可以是string、boolean、int等等，或者直接返回void
3. 尽量不返回自己定义的其他格式
4. controller方法的参数，如果来自url，可以是@requestvariable（url采用问号参数形式），或者@pathvariable（url采用rest形式），对应的数据类型，也是基本数据类型，或者来自form

## service 开发
1. service直接与dao接触，可以调用一些工具类的方法辅助
2. 返回值要求与controller相同
3. 如果一个service方法需要多次修改数据库，则需在方法头部注解@tranactional，表示这个操作是事务性的，如果失败则直接回滚
4. 注意在保存、修改、删除对象时，其外键关联对应的对象是否需要修改，比如删除时需要将相应的对象set为null


## dao 开发
1. 尽量通过使用jpa直接支持的方法名规范写接口
2. 多表联查时，可以使用@query注解手写sql（在一般dao查询次数过多，效率低下时启用此种模式）
3. findall，findone，getall,getone，save，delete，这些都是jpa原生提供的，不需要写接口硬性支持
4. 分页查询使用pagable对象，不需手动实现分页
5. dao中的方法参数，除了传string、int等基本类型，也可以直接传对象（这也可以避免一部分多表联查）

## 枚举类型
1. string-->enum : Enumtype.valueof(str)
2. enum-->string: Enumtype.xxx.getName() Enumtype.xxx.getValue() 
3. enum是否相等，可以直接用 “==”
4. 判断是否存在某个string的enum，使用  EnumUtils.isValidEnum(MyEnum.class, myValue)




