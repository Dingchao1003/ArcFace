package com.vishee.arcface.service;

import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.enums.ExtractType;
import com.arcsoft.face.toolkit.ImageFactory;
import com.arcsoft.face.toolkit.ImageInfo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.vishee.arcface.config.ArcFaceEngineRunner;
import com.vishee.arcface.mapper.FaceMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dingchao
 * @date 2022-03-26/16:12
 * @description
 */
@Slf4j
public class RegisterArcThread implements Runnable {

    private List<String> fileNameList;

    private ArcFaceEngineRunner service;

    private FaceEngine engine;

    private FaceMapper faceMapper;

    public RegisterArcThread(List<String> fileNameList, ArcFaceEngineRunner service, FaceMapper faceMapper) {
        this.fileNameList = fileNameList;
        this.service = service;
        this.faceMapper = faceMapper;
    }

    @Override
    public void run() {
        try {
            engine = service.newEngine();
            List<Map<String, Object>> insertSQLParam = new ArrayList<>();
            for (String fileName : fileNameList) {
                File itemFile = new File(fileName);
                List<FaceInfo> faceInfoList = new ArrayList<>();
                ImageInfo imageInfo = ImageFactory.getRGBData(itemFile);
                //分析人脸数据，每次只取一个人脸
                int errorCode = engine.detectFaces(imageInfo, faceInfoList);
                FaceInfo faceInfo = null;
                FaceFeature faceFeature = new FaceFeature();

                if (!faceInfoList.isEmpty()) {
                    faceInfo = faceInfoList.get(0);
                }
                if (faceInfo != null) {
                    errorCode = engine.extractFaceFeature(imageInfo, faceInfo, ExtractType.REGISTER, 0, faceFeature);
                    //获取人脸信息,放入矢量数据库
                    Long id = IdWorker.getId();
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", id);
                    map.put("name", fileName);
                    map.put("data", faceFeature.getFeatureData());
                    insertSQLParam.add(map);
                }
            }
            faceMapper.insertBlob(insertSQLParam);
            log.info("| server| insert sql success");
        } catch (Exception e) {
            log.error("解析人脸出现问题", e);
            e.printStackTrace();
        } finally {
            if (engine != null) {
                engine.unInit();
                engine = null;
                fileNameList = null;

            }
        }


    }


}
