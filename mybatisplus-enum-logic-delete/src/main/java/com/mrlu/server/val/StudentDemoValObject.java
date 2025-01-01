package com.mrlu.server.val;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author 简单de快乐
 * @create 2023-12-04 17:15
 */
public class StudentDemoValObject {


    /**
     * @JsonFormat 将枚举当做一个对象来解析，把枚举的所有属性解析成json
     */
    @JsonFormat(shape= JsonFormat.Shape.OBJECT)
    public enum FinishEnum {
        FINISH(1, "已结束"),
        UN_FINISH(0, "未结束");

        @EnumValue // 标记入库的值
        private Integer value;

        //@JsonValue    //标记响应json值，如果不使用@JsonFormat的话
        private String desc;

        FinishEnum(Integer value, String desc ) {
            this.value = value;
            this.desc = desc;
        }

        public Integer getValue() {
            return value;
        }

        public String getDesc() {
            return desc;
        }

        /**
         * 用于保存所有的枚举值
         */
        private static Map<Integer, FinishEnum> RESOURCE_MAP = Stream
                .of(FinishEnum.values())
                .collect(Collectors.toMap(FinishEnum::getValue, Function.identity()));

        /**
         * 枚举反序列话调用该方法.
         *
         * 以如下格式调用时，会调用desFirst方法
         * {
         *             "name": "desFirst",
         *              "finish": {
         *                 "value"" 0,
         *                 "desc": "未结束"
         *              }
         *  }
         *
         * 当调用add接口的时候，解析finish字段发现对应的实体类型为枚举类型，就会调用对应枚举类型的
         * @JsonCreator标注的方法进行解析。
         * @param jsonNode 枚举字段的json节点
         * @return
         */
        /*@JsonCreator
        public static FinishEnum desFirst(final JsonNode jsonNode) {
            // 获取枚举字段的value子节点的值
            return Optional.ofNullable(RESOURCE_MAP.get(jsonNode.get("value").asInt()))
                    .orElseThrow(() -> new IllegalArgumentException(jsonNode.get("value").asText()));
        }*/

        /**
         * 以如下格式调用时，会调用desSecond方法
         * {
         *             "name": "desSecond",
         *              "finish": 1
         *  }
         * @param jsonNode
         * @return
         */
        @JsonCreator
        public static FinishEnum desSecond(final JsonNode jsonNode) {
            return RESOURCE_MAP.get(jsonNode.asInt());
        }
    }

}
