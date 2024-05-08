package com.example.emos.api.db.dao;

import com.example.emos.api.db.pojo.TbAmect;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbAmectDao {

    public ArrayList<HashMap> searchAmectByPage(HashMap param);

    public long searchAmectCount (HashMap param);
    public int insert(TbAmect amect);
    public HashMap searchById(int id);
    public int update(HashMap param);
    public int deleteAmectByIds(Integer[] ids);

}
