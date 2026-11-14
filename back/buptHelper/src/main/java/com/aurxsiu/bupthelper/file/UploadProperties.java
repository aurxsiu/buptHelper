package com.aurxsiu.bupthelper.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 附件存储配置，前缀 bupthelper.upload。
 */
@ConfigurationProperties(prefix = "bupthelper.upload")
public class UploadProperties {

    /** 附件存储根目录 */
    private String dir = "./store";

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
