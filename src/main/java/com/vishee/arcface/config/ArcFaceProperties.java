package com.vishee.arcface.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author dingchao
 * @date 2022-03-25/10:54
 * @description
 */

@Configuration
@ConfigurationProperties(prefix = "arc.ai.face")
@Data
public class ArcFaceProperties {


    private String appid;

    private String sdkKey;

    private String activeKey;

    /**
     * 虹软运行库路径
     */
    private String libPath;
    /**
     * 虹软激活引擎路径
     */
    private String activeFilePath;

    private String os;

    private Integer arc;
}
