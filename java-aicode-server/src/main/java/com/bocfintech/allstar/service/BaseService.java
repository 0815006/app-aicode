package com.bocfintech.allstar.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bocfintech.allstar.entity.MyPage;
import org.apache.ibatis.exceptions.TooManyResultsException;

import java.util.List;

public interface BaseService<T> extends IService<T> {

    void save(List<T> var1);

    void deleteById(Long var1);

    void delete(String var1, String var2);

    void deleteByIds(String var1);

    void update(T var1);

    T findById(Long var1);

    T findBy(String var1, Object var2) throws TooManyResultsException;

    List<T> findAllBy(String var1, Object var2);

    List<T> findAll();

    IPage<T> findAll(int var1, int var2);

    IPage<T> findAll(int var1, int var2, String var3);

    IPage<T> findAll(int var1, int var2, String var3, boolean var4);

    IPage<T> findAll(int var1, int var2, String var3, boolean var4, QueryWrapper<T> var5);

    IPage<T> findAll(QueryWrapper<T> var1, int var2, int var3);

    Long keyIsExist(String var1, String var2);

    MyPage<T> myFindAll(int var1, int var2);

    MyPage<T> myFindAll(int var1, int var2, String var3);

    MyPage<T> myFindAll(int var1, int var2, String var3, boolean var4);

    MyPage<T> myFindAll(int var1, int var2, String var3, boolean var4, QueryWrapper<T> var5);

    MyPage<T> myFindAll(QueryWrapper<T> var1, int var2, int var3);
}
