package com.vishee.arcface.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * @author dingchao
 * @date 2022-03-25/15:47
 * @description
 */
@Mapper
public interface FaceMapper {

    void insert(List<Map<String, Object>> param);

    void insertBlob(List<Map<String, Object>> param);

    List<Map<String, Object>> list();

    Integer count();
}
