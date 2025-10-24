package com.lora.mianshihou.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 用户角色枚举
 *
 * @author lora
 *
 */
public enum UserRoleEnum {

    USER("用户", "user"),
    ADMIN("管理员", "admin"),
    BAN("被封号", "ban");

    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        //假设枚举类很多？？？
        // private static final Map<string : UserRoleEnum> VALUE_MAP = new HashMap<>();
        // static {
        // // 构建映射
        // for ( UserRoleEnum role : values()){
         //    VALUE_MAP.put(role.value,role)
         //}
        //  根据value获取枚举
        // private static UserRoleEnum getEnumByValue(String value){
        // if(ObjectUtils.isEmpty(value)){  return null; }
        // }
        // return VALUE_MAP.get(value);
        //
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
