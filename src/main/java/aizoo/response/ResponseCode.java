package aizoo.response;

public enum ResponseCode {
    /**
     * 成功返回的状态码
     */
    SUCCESS(200, "成功"),
    /**
     * Job执行不成功
     */
    RUN_ERROR(10005, "执行不成功，请重试"),
    /**
     * 无slurm账号，无法执行实验、服务、应用
     */
    NO_SLURM_ACCOUNT_ERROR(10006, "无slurm账号，无法执行！"),
    /**
     * 新建图时图名称已存在的状态码
     */
    GRAPH_NAME_EXISTS_ERROR(10002, "同名图已存在"),
    /**
     * 截图上传失败
     */
    GRAPH_PICTURE_UPLOAD_ERROR(10003, "截图上传失败"),
    /**
     * 该组件已被当前用户fork过
     */
    COMPONENT_HAS_FORKED_ERROR(10004, "该组件已被当前用户fork过"),
    /**
     * 用户未登录
     */
    NOT_LOGIN(401, "用户未登录"),
    /**
     * 用户无权限
     */
    PERMISSION_ERROR(403, "用户无权限"),
    /**
     * 用户名或密码不正确
     */
    LOGIN_ERROR(20003, "用户名或密码不正确"),
    /**
     * 终止任务失败
     */
    CANCEL_JOB_ERROR(12000,"终止失败请重试"),
    /**
     * 所有无法识别的异常默认的状态码
     */
    SERVICE_ERROR(500, "服务器异常"),
    /**
     * 组件上传失败
     */
    COMPONENT_UPLOAD_ERROR(10010, "组件上传失败"),
    /**
     * 组件上传失败
     */
    MIRROR_UPLOAD_ERROR(10011, "镜像上传失败"),
    /**
     * 数据资源上传失败
     */
    DATASOURCE_UPLOAD_ERROR(10012, "数据资源上传失败，请重试"),
    /**
     * 解析json文件失败
     */
    JSON_FILE_PARSE_ERROR(10013, "解析json文件失败:文件数据有异常！"),
    /**
     * 任务运行结果下载失败
     */
    JOB_DOWNLOAD_ERROR(10021,"任务运行结果下载失败"),

    /**
     * fork目录中存在同名文件
     */
    File_NAME_EXISTS(10022,"fork目录中存在同名文件"),

    /**
     * fork目录中存在同名文件
     */
    File_NOT_DIR(10024,"不是文件目录"),

    /**
     * 连线失败
     */
    CAN_LINK_ERROR(10023,"连线失败"),
    /**
     * 命名空间注册失败,已经存在该命名空间
     */
    NAMESPACE_REGISTER_ERROR(10031,"命名空间注册失败，已经存在该命名空间"),
    /**
     * 修改权限失败
     */
    PRIVACY_MODIFY_ERROR(10032,"权限修改失败"),

    /**
     * 切片验证失败
     */
    CHUNK_CHECK_ERROR(10033,"切片验证失败"),

    /**
     * 组件fork失败
     */
    FORK_COMPONENT_ERROR(10034,"该命名空间下已经有相同名字的组件"),

    /**
     * 验证资源容量不通过
     */
    RESOURCE_CHECK_ERROR(10035,"所需资源已超出限制！"),

    /**
     * 密码不符合规范，注册失败
     */
    REGISTER_ERROR(10041,"注册失败！"),

    /**
     * 用户名已存在
     */
    USERNAME_CHECK_ERROR(20001,"用户名已存在！"),
    /**
     * 用户名不符合规范
     */
    USERNAME_RULE_ERROR(20004,"用户名不符合规范！"),

    /**
     * 图类型不存在
     */
    GRAPH_TYPE_ERROR(30003, "图类型不存在"),

    /**
     * 该组件版本已存在
     */
    COMPONENT_VERSION_ERROR(30004, "该组件版本已存在"),
    /**
     * 枚举类型不存在
     */
    ENUMTYPE_ERROR(30005, "该枚举类型不存在"),
    /**
     * 用户升级审核中
     */
    LEVEL_CHANGE_ERROR(30006, "仍有升级请求审核中"),

    /**
     * 新建组件失败
     */
    COMPONENT_BUILD_ERROR(30007, "新建组件失败！"),

    /**
     * 修改组件失败
     */
    COMPONENT_MODIFY_ERROR(30008, "修改组件失败！"),

    /**
     * 发布图失败
     */
    RELEASE_GRAPH_ERROR(30009, "发布图失败！"),
    /**
     * fork的组件或者其子组件有确实
     */

    FORK_NOT_EXIST(30010, "fork的组件或者其子组件有缺失"),

    /**
     * 邮箱已被注册
     */
    EMAIL_CHECK_ERROR(20002, "该邮箱已被注册！"),

    /**
     * 邮箱不存在
     */
    EMAIL_NOT_EXITING_ERROR(10071, "该邮箱不存在！"),

    /**
     * 编译下载图失败
     */
    GRAPH_COMPILE_ERROR(10051, "编译下载图失败!"),

    /**
     * 获取会话列表失败
     */
    GET_CONVERSATION_LIST_ERROR(10061, "获取会话列表失败!"),

    /**
     * 获取私信的消息列表失败
     */
    GET_MESSAGE_LIST_ERROR(10062, "获取私信的消息列表失败!"),

    /**
     * 发送消息失败
     */
    SEND_MESSAGE_ERROR(10063, "发送消息失败!"),

    /**
     * 拉取公告失败
     */
    PULL_ANNOUNCE_FAILED(10064, "拉取公告失败!"),

    /**
     * 删除分享记录失败
     */
    DELETE_SHARE_RECORD_FAILED(10065, "删除分享记录失败!"),

    /**
     * 资源不可重复分享
     */
    RESOURCE_ALREADY_SHARED(10066, "资源已经被分享过了，请勿重复分享"),

    /**
     * 资源接受分享失败
     */
    SHARE_ACCEPT_ERROR(10067, "接受分享失败"),

    /**
     * 文件已经不存在了
     */
    FILE_NOT_EXIT_ERROR(10068, "文件已经不存在了"),

    /**
     * 保存图失败
     */
    GRAPH_SAVA_ERROR(10069, "保存图失败"),

    /*
    资源为私有资源，无法拷贝
     */
    PRIVACY_ERROR(10070, "私有资源无法fork"),

    /**
     * 非本人资源，无法删除
     */
    DELETE_OUT_OF_BOUNDS(10072,"非本人资源，无法删除"),
    /**
     * 非本人资源，无法修改
     */
    CHANGE_OUT_OF_BOUNDS(10073, "非本人资源，无法修改"),

    /**
     * 非本人JOB，无法停止
     */
    JOBSTOP_OUT_OF_BOUNDS(10074, "非本人JOB，无法停止"),

    /**
     * 无分享权限
     */
    SHARE_OUT_OF_BOUNDS(10075, "非本人资源，无分享权限"),

    /**
     * 空图禁止分享
     */
    SHARE_NULL_ERROR(10076, "空图禁止分享！"),
    /**
     * 无添加权限
     */
    ADD_OUT_OF_BOUNDS(10077, "非本人资源，无法添加！"),
    /**
     * 项目文件上传失败
     */
    PROJECT_FILE_UPLOAD_ERROR(10078, "项目文件上传失败，请重试"),
    /**
     * 项目文件下载失败
     */
    PROJECT_FILE_DOWNLOAD_ERROR(10079, "项目文件下载失败!"),
    /**
     * 非本人资源，无法下载
     */
    DOWNLOAD_OUT_OF_BOUNDS(10080,"非本人资源，无法下载"),
    /**
     * SlurmAccount账号为空时的提示信息
     */
    SLURM_ACCOUNT_NULL(10081,"当前用户无slurm账号,无法查看集群资源使用情况"),
    /**
     *
     */
    IP_NOT_MATCH(10082,"slurmAccount中ip信息为空或者jobKey中ip与slurmAccount的ip不一致"),
    /**
     *
     */
    APPLICATION_DELETE_FAILED(10083,"删除应用失败"),
    /**
     *删除图失败
     */
    GRAPH_DELETE_FAILED(10084,"请先删除实验管理中关联的实验后再删除该图！"),
    /**
     * 代码上传失败
     */
    CODE_UPLOAD_FAILED(10085,"代码上传失败"),
    /**
     * 已存在相同code文件名
     */
    HAVE_SAME_CODENAME(20005,"已存在相同的code文件名"),
    /**
     * 已存在相同镜像文件名
     */
    HAVE_SAME_MIRRORNAME(20006,"已存在相同的镜像文件名"),
    /**
     * 硬盘资源超限
     */
    DISK_CHECK_ERROR(10086,"硬盘资源超限！请申请更高等级权限或删除部分其他资源后重试！"),
    /**
     * checkPoint被其他进程占用
     */
    CHECKPOINT_DELETE_ERROR(10087,"checkPoint被其他进程占用！请稍后重试！"),
    /**
     * 图不存在
     */
    GRAPH_NOT_EXIST_ERROR(10088,"所需图不存在！"),
    /**
     * 代码上传失败
     */
    SINGLE_FILE_UPLOAD_FAILED(10089,"代码单个文件上传失败"),
    /**
     * 图中有节点缺失
     */
    NODE_NOT_EXIST_ERROR(10090,"图中有节点缺失"),
    ;


    /**
     * 状态码
     */
    private int code;
    /**
     * 返回信息
     */
    private String msg;

    ResponseCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
