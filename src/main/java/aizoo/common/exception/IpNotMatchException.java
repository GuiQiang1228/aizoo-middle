package aizoo.common.exception;

/**
 * slurmAccount取的ip中与jobKey中拿到的ip不同，则抛出该异常
 */
public class IpNotMatchException extends Exception{
    public IpNotMatchException(){
        super();
    }
}
