package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bocfintech.allstar.entity.MyPage;
import com.bocfintech.allstar.mapper.MyBaseMapper;
import com.bocfintech.allstar.service.BaseService;
import com.bocfintech.allstar.util.MyPageUtil;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BaseServiceImpl<M extends MyBaseMapper<T>, T> extends ServiceImpl<M, T> implements BaseService<T> {
    @Autowired
    protected MyBaseMapper<T> mapper;
    private Class<T> modelClass;

    public BaseServiceImpl() {
        ParameterizedType pt = (ParameterizedType)this.getClass().getGenericSuperclass();
        this.modelClass = (Class)pt.getActualTypeArguments()[0];
    }

    public void save(List<T> models) {
        this.saveBatch(models);
    }

    public void deleteById(Long id) {
        this.mapper.deleteById(id);
    }

    public void deleteByIds(String ids) {
        List<String> list = (List) Arrays.stream(ids.split(",")).collect(Collectors.toList());
        this.mapper.deleteBatchIds(list);
    }

    public void delete(String domainField, String value) {
        QueryWrapper<T> queryWrapper = new QueryWrapper();
        queryWrapper.eq(domainField, value);
        this.mapper.delete(queryWrapper);
    }

    public void update(T model) {
        this.mapper.updateById(model);
    }

    public T findById(Long id) {
        return this.mapper.selectById(id);
    }

    public T findBy(String fieldName, Object value) throws TooManyResultsException {
        QueryWrapper<T> queryWrapper = new QueryWrapper();
        queryWrapper.eq(fieldName, value);
        return this.mapper.selectOne(queryWrapper);
    }

    public List<T> findAllBy(String fieldName, Object value) {
        QueryWrapper<T> queryWrapper = new QueryWrapper();
        queryWrapper.eq(fieldName, value);
        return this.mapper.selectList(queryWrapper);
    }

    public List<T> findAll() {
        return this.mapper.selectList((Wrapper)null);
    }

    public IPage<T> findAll(int page, int size) {
        return this.findAll(page, size, (String)null);
    }

    public IPage<T> findAll(int page, int size, String orderBy) {
        Page<T> paget = new Page((long)page, (long)size);
        OrderItem orderItem = new OrderItem();
        orderItem.setColumn(orderBy);
        orderItem.setAsc(true);
        List<OrderItem> orderItemList = new ArrayList();
        paget.addOrder(orderItemList);
        IPage<T> ipage = this.mapper.selectPage(paget, (Wrapper)null);
        return ipage;
    }

    public IPage<T> findAll(int page, int size, String sortField, boolean asc) {
        return this.findAll(page, size, sortField, asc, (QueryWrapper)null);
    }

    public IPage<T> findAll(int page, int size, String sortField, boolean asc, QueryWrapper<T> query) {
        Page<T> paget = new Page((long)page, (long)size);
        if (asc) {
            paget.addOrder(new OrderItem[]{OrderItem.asc(sortField)});
        } else {
            paget.addOrder(new OrderItem[]{OrderItem.desc(sortField)});
        }

        return this.mapper.selectPage(paget, query);
    }

    public IPage<T> findAll(QueryWrapper<T> query, int page, int size) {
        Page<T> paget = new Page((long)page, (long)size);
        return this.mapper.selectPage(paget, query);
    }

    public Long keyIsExist(String domainField, String value) {
        QueryWrapper<T> queryWrapper = new QueryWrapper();
        queryWrapper.eq(domainField, value);
        return this.mapper.selectCount(queryWrapper);
    }

    public MyPage<T> myFindAll(int page, int size) {
        MyPage myPage = MyPageUtil.getMypage(this.findAll(page, size));
        return myPage;
    }

    public MyPage<T> myFindAll(int page, int size, String orderBy) {
        MyPage myPage = MyPageUtil.getMypage(this.findAll(page, size, orderBy));
        return myPage;
    }

    public MyPage<T> myFindAll(int page, int size, String sortField, boolean asc) {
        MyPage myPage = MyPageUtil.getMypage(this.findAll(page, size, sortField, asc));
        return myPage;
    }

    public MyPage<T> myFindAll(int page, int size, String sortField, boolean asc, QueryWrapper<T> query) {
        MyPage myPage = MyPageUtil.getMypage(this.findAll(page, size, sortField, asc, query));
        return myPage;
    }

    public MyPage<T> myFindAll(QueryWrapper<T> query, int page, int size) {
        MyPage myPage = MyPageUtil.getMypage(this.findAll(query, page, size));
        return myPage;
    }
}
