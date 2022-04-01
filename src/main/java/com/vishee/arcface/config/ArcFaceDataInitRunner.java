package com.vishee.arcface.config;

import com.alibaba.fastjson.JSON;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeatureInfo;
import com.arcsoft.face.FaceSearchCount;
import com.github.pagehelper.PageHelper;
import com.vishee.arcface.mapper.FaceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Slf4j
public class ArcFaceDataInitRunner implements CommandLineRunner {


    @Autowired
    FaceEngine defaultFaceEngine;

    @Autowired
    FaceMapper faceMapper;


    @Override
    public void run(String... args) throws Exception {
        Integer faceCount = faceMapper.count();
        double pageSize = Math.ceil((double) faceCount / 1000);
        System.out.println("分页大小" + pageSize);
        for (int i = 0; i <= pageSize; i++) {
            PageHelper.startPage(i, 1000);
            List<Map<String, Object>> mapList = faceMapper.list();
            List<FaceFeatureInfo> faceFeatureInfos = new ArrayList<>();
            mapList.forEach(x -> {
                FaceFeatureInfo featureInfo = new FaceFeatureInfo();
                featureInfo.setSearchId((Integer) x.get("id"));
                featureInfo.setFaceTag(x.get("tag").toString());
                featureInfo.setFeatureData((byte[]) x.get("data"));
                faceFeatureInfos.add(featureInfo);
            });
            defaultFaceEngine.registerFaceFeature(faceFeatureInfos);
            FaceSearchCount faceSearchCount = new FaceSearchCount();
            defaultFaceEngine.getFaceCount(faceSearchCount);
            System.out.println("注册人脸个数:" + faceSearchCount.getCount());
        }
    }


}