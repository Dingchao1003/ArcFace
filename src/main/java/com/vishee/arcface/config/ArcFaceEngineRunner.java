package com.vishee.arcface.config;

import com.alibaba.fastjson.JSON;
import com.arcsoft.face.ActiveDeviceInfo;
import com.arcsoft.face.EngineConfiguration;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FunctionConfiguration;
import com.arcsoft.face.enums.DetectMode;
import com.arcsoft.face.enums.DetectOrient;
import com.vishee.arcface.constant.FaceConstants;
import com.vishee.arcface.mapper.FaceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author dingchao
 * @date 2022-03-25/10:49
 * @description
 */
@Service
@Slf4j
@EnableConfigurationProperties(ArcFaceProperties.class)
@Configuration
public class ArcFaceEngineRunner implements InitializingBean, DisposableBean {


    @Autowired
    private ArcFaceProperties arcFaceProperties;

    @Autowired
    FaceMapper faceMapper;

    //人脸引擎
    private static volatile FaceEngine defaultEngine;

    private volatile boolean isActive = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        int errorCode;
        FaceEngine engine = null;
        try {
            File libFile = new File(arcFaceProperties.getLibPath());
            if (!libFile.exists()) {
                throw new RuntimeException("|face engine| lib path is not exist");
            }

            engine = new FaceEngine(arcFaceProperties.getLibPath());
            //收集当前设备信息
            ActiveDeviceInfo activeDeviceInfo = new ActiveDeviceInfo();
            errorCode = engine.getActiveDeviceInfo(activeDeviceInfo);
            log.info("|face engine| collecting device info, operateCode is:{}", errorCode);
            log.info("|face engine| device info is :{}", activeDeviceInfo.getDeviceInfo());
            //获取当前用户路径写入设备信息文件
            String deviceFileName = System.getProperties().getProperty("user.home") + File.separator + FaceConstants.OFFLINE_ACTIVE_DEVICE_INFO_FILENAME;
            File deviceInfoFile = new File(deviceFileName);
            log.info("|face engine| generate device file");
            generateFile(deviceInfoFile, activeDeviceInfo.getDeviceInfo());

        } catch (Exception e) {
            throw new RuntimeException("|face engine| error on generate device file");
        }
        //尝试激活引擎，激活失败不会影响程序启动
        try {
            File activeFile = new File(arcFaceProperties.getActiveFilePath());
            if (activeFile.exists() && activeFile.isFile()) {
                active(arcFaceProperties.getActiveFilePath(), engine);
                log.info("|face engine| engine offline active success");
            }
        } catch (Exception e) {
            log.error("|face engine| error on active engine with file", e);
            throw new RuntimeException("激活人脸引擎失败");
        }
        defaultEngine = engine;
    }

    /**
     * 激活人脸引擎方法
     *
     * @param activeFilePath 激活文件路径
     * @throws Exception
     */
    public FaceEngine active(String activeFilePath, FaceEngine engine) {

        if (isActive) {
            log.info("|face engine| engine has been active");
            return init(engine);

        }
        if (StringUtils.isEmpty(activeFilePath)) {
            activeFilePath = arcFaceProperties.getActiveFilePath();
        }
        File activeFile = new File(activeFilePath);
        if (!activeFile.exists() || !activeFile.isFile()) {
            log.error("|face engine| file {} not exist can't active engine", activeFilePath);
            throw new RuntimeException("激活文件不存在，无法激活人脸引擎");
        }
        log.info("|face engine| begin active engine with file :{}", activeFilePath);
        engine.activeOffline(activeFilePath);
        isActive = true;
        return init(engine);
    }

    /**
     * 卸载人脸引擎
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        log.info("|face engine| uninstall face engine");
        defaultEngine.unInit();
    }

    private void generateFile(File file, String context) throws IOException {
        file.deleteOnExit();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(context.getBytes(StandardCharsets.UTF_8));
        fileOutputStream.close();
    }

    /**
     * 初始化引擎
     */
    private FaceEngine init(FaceEngine engine) {
        if (!isActive) {
            log.error("|face engine| can't init,engine is not active");
            throw new RuntimeException("初始化引擎配置失败,引擎未激活");
        }
        if (engine == null) {
            log.error("|face engine| can't init,engine is null");
            throw new RuntimeException("初始化引擎配置失败,引擎未声明");
        }
        //引擎配置
        EngineConfiguration engineConfiguration = new EngineConfiguration();
        engineConfiguration.setDetectMode(DetectMode.ASF_DETECT_MODE_IMAGE);
        engineConfiguration.setDetectFaceOrientPriority(DetectOrient.ASF_OP_ALL_OUT);
        engineConfiguration.setDetectFaceMaxNum(10);
        //功能配置
        FunctionConfiguration functionConfiguration = new FunctionConfiguration();
        functionConfiguration.setSupportAge(true);
        functionConfiguration.setSupportFaceDetect(true);
        functionConfiguration.setSupportFaceRecognition(true);
        functionConfiguration.setSupportGender(true);
        functionConfiguration.setSupportLiveness(true);
        functionConfiguration.setSupportIRLiveness(true);
        functionConfiguration.setSupportImageQuality(true);
        functionConfiguration.setSupportMaskDetect(true);
        functionConfiguration.setSupportUpdateFaceData(true);
        engineConfiguration.setFunctionConfiguration(functionConfiguration);
        int errorCode = engine.init(engineConfiguration);
        if (errorCode != 0) {
            log.error("|face engine| engine init error, errorCode is {}", errorCode);
            throw new RuntimeException("初始化引擎配置失败，错误码" + errorCode);
        }
        log.info("|face engine| engine init success version is {}", engine.getVersion().getVersion());
        return engine;
    }


    public FaceEngine newEngine() {
        if (!isActive) {
            throw new RuntimeException("引擎未激活，请激活后再获取");
        }
        FaceEngine engine = new FaceEngine(arcFaceProperties.getLibPath());
        engine = active(arcFaceProperties.getActiveFilePath(), engine);
        return engine;
    }


    @Bean(name = "defaultEngine")
    public FaceEngine defaultEngine() {
        return defaultEngine;
    }


}
