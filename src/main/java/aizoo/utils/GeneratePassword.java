package aizoo.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePassword {

    /**
     * 该方法根据一个输入的password参数对其进行加密
     * @param password 传入一个要加密的password参数
     * @return 返回一个加密后的密文字符串
     * @throws
     */
    public static String generatePassword(String password) {
        return new BCryptPasswordEncoder().encode(password);
    }
    
    public static void main(String[] args) {
        // 打印加密后的password
        System.out.println(generatePassword("pwd"));
    }
}