package aizoo.common.exception;

/**
 * fork组件时抛出的自定义异常
 */
public class ForkException extends Exception{
    public ForkException() {
        super();
    }

    public ForkException(String message) {
        super(message);
    }
}
