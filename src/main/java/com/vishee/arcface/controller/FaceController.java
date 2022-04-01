package com.vishee.arcface.controller;

import com.arcsoft.face.*;

import com.arcsoft.face.enums.CompareModel;
import com.arcsoft.face.enums.ExtractType;
import com.arcsoft.face.toolkit.ImageFactory;
import com.arcsoft.face.toolkit.ImageInfo;
import com.vishee.arcface.mapper.FaceMapper;
import com.vishee.arcface.config.ArcFaceEngineRunner;
import com.vishee.arcface.service.RegisterArcThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dingchao
 * @date 2022-03-25/15:50
 * @description
 */
@Controller
@Slf4j
public class FaceController {

    @Autowired
    ArcFaceEngineRunner faceEngineService;

    @Autowired
    FaceMapper faceMapper;

    @Autowired
    ExecutorService crawlExecutorPool;

    @Autowired
    FaceEngine defaultEngine;

    AtomicInteger count = new AtomicInteger(0);

    @RequestMapping("/index")
    public String loadPage() {
        return "index";
    }


    @ResponseBody
    @RequestMapping("/initLocalData")
    public String test() {
        String prefix = "D:\\pic\\person";
        File file = new File(prefix);
        String[] s = file.list();
        List<String> fileList = new ArrayList<>();
        for (int i = 0; i < Objects.requireNonNull(s).length; i++) {
            fileList.add(prefix + File.separator + s[i]);

            if (fileList.size() == 100) {
                log.info("创建线程{}", count);
                count.getAndDecrement();
                RegisterArcThread thread = new RegisterArcThread(fileList, faceEngineService, faceMapper);

                crawlExecutorPool.execute(thread);
                fileList = new ArrayList<>();
            }
            if (i == Objects.requireNonNull(s).length - 1) {
                log.info("创建线程2{}", count);
                count.getAndDecrement();
                RegisterArcThread thread = new RegisterArcThread(fileList, faceEngineService, faceMapper);
                crawlExecutorPool.execute(thread);
                fileList = new ArrayList<>();
            }

        }

        return "init data success";
    }


    @ResponseBody
    @RequestMapping("/postpic")
    public String loadPage(@RequestParam(value = "file") MultipartFile file) throws IOException {
        int errorCode;

        List<FaceFeatureInfo> faceFeatureInfos = new ArrayList<>();
        Instant inst1 = Instant.now();
        List<FaceInfo> faceInfoList = new ArrayList<FaceInfo>();
        ImageInfo imageInfo = ImageFactory.getRGBData(file.getBytes());
        errorCode = defaultEngine.detectFaces(imageInfo, faceInfoList);
        System.out.println("人脸检测errorCode:" + errorCode);
        System.out.println("检测到人脸数:" + faceInfoList.size());
        if (!faceInfoList.isEmpty()) {
            FaceInfo faceInfo = faceInfoList.get(0);
            SearchResult searchResult = new SearchResult();
            FaceFeature faceFeature = new FaceFeature();
            errorCode = defaultEngine.extractFaceFeature(imageInfo, faceInfo, ExtractType.REGISTER, 0, faceFeature);
            float y = 0.6f;
            float i = 0.99f;

            while (i >= y) {
                errorCode = defaultEngine.searchFaceFeature(faceFeature, CompareModel.LIFE_PHOTO, searchResult);
                FaceFeatureInfo featureInfoitem = searchResult.getFaceFeatureInfo();
                faceFeatureInfos.add(featureInfoitem);
                defaultEngine.removeFaceFeature(featureInfoitem.getSearchId());
                i = searchResult.getMaxSimilar();
                System.err.println(i + "相似人脸id" + searchResult.getFaceFeatureInfo().getSearchId() + "匹配分数" + searchResult.getMaxSimilar());
            }
            defaultEngine.registerFaceFeature(faceFeatureInfos);

            Instant inst2 = Instant.now();
            System.err.println("查询人脸数据时间" + Duration.between(inst1, inst2));
            return "最相似人脸Id:" + searchResult.getFaceFeatureInfo().getSearchId();
        }

        return "未找到相似人脸数据";


    }

    public static void main(String[] args) {
        // System.out.println(Math.ceil(11001 / 1000));
    }


}
