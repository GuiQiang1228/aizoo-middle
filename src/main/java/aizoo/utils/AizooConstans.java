package aizoo.utils;

public class AizooConstans {

    // 为output增加的self输出
    public static final String SELF_NAME = "self";
    public static final String SELF_TITLE = "self";
    public static final String SELF_DESCRIPTION = "";
    public static final String SELF_ORIGIN_NAME = "self";

    // aizoo unknown类型的type
    public static final String AIZOO_UNKNOWN = "aizoo.unknown";

    // aizoo unknown类型的type的id
    public static final Long AIZOO_UNKNOWN_ID = 8l;

    // slurm运行时的Python版本
    public static final String SLURM_PYTHON_VERSION = "python3/3.7.2";

    // slurm 运行时的参数名
    public static final String SLURM_RUN_ARGUMENTS_NAME = "slurmKwargs";



    // slurm运行的JSON模板
    public static final String SLURM_JSON = "{\n" +
            "\t\"job_name\": \"job_test\",\n" +
            "\t\"slurm_kwargs\": {\n" +
            "\t\t\"partition\": \"debug\"\n" +
            "\t},\n" +
            "\t\"scripts_dir\": \"test_script_dir\",\n" +
            "\t\"log_dir\": \"testlog_dir\",\n" +
            "\t\"environment\": \"python3/3.7.2\",\n" +
            "\t\"file_path\": \"test.py\"\n" +
            "}";

    // 编译下载的ZIP内文件夹的名字
    public static final String COMPILE_ZIP_FILE_FOLDER_NAME = "graph_codes";
}
