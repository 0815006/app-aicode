package com.bocfintech.allstar.entity;

import java.io.Serializable;

public class MyPageInfo implements Serializable {
    private int page;
    private int size;
    private int begin;
    private int end;
    private long total;
    private int pages;
    private boolean count;

    public MyPageInfo(int page, int size) {
        this(page, size, true);
    }

    public MyPageInfo(int page, int size, boolean count) {
        this.count = true;
        this.page = page;
        this.size = size;
        this.begin = page * size;
        this.end = this.begin + size;
        this.count = count;
    }

    public int getBegin() {
        return this.begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return this.end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getPage() {
        return this.page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isCount() {
        return this.count;
    }

    public void setCount(boolean count) {
        this.count = count;
    }

    public long getTotal() {
        return this.total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPages() {
        return this.pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }
}
