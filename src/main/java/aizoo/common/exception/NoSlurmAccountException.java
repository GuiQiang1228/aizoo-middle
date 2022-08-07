package aizoo.common.exception;

/**
 * 执行实验、服务、应用等方法时，若用户无slurm账号，则跑出该异常
 */
public class NoSlurmAccountException extends Exception{
    public NoSlurmAccountException() {
        super();
    }
}
