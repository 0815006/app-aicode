package com.bocfintech.allstar.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonFormat(
        shape = Shape.OBJECT
)
public class MyPage<E> extends ArrayList<E> {
    private long size;
    private long offset;
    private long number;
    private long totalPages;
    private long totalElements;

    public long getNumberOfElements() {
        return (long)this.size();
    }

    public MyPage() {
    }

    public void setContent(List<E> content) {
        this.clear();
        this.addAll(content);
    }

    public List<E> getContent() {
        return (List<E>) Arrays.asList(this.toArray());
    }

    public MyPage(List<E> content, MyPageInfo pageInfo, long total) {
        super(content);
        this.number = (long)pageInfo.getPage();
        this.size = (long)pageInfo.getSize();
        this.totalElements = total;
        this.totalPages = (long)pageInfo.getSize() == 0L ? 1L : (total - 1L) / this.size + 1L;
    }

    public MyPage(long offset, List<E> content, long pagesSize, long total) {
        super(content);
        this.offset = offset;
        this.size = pagesSize;
        this.totalElements = total;
        this.totalPages = pagesSize == 0L ? 1L : (total - 1L) / this.size + 1L;
    }

    public MyPage(List<E> content, long page, long pagesSize, long total) {
        super(content);
        this.number = page;
        this.size = pagesSize;
        this.totalElements = total;
        this.totalPages = pagesSize == 0L ? 1L : (total - 1L) / this.size + 1L;
    }

    public void mySetPageInfo(MyPage myPage) {
        this.size = myPage.size;
        this.number = myPage.number;
        this.totalPages = myPage.totalPages;
        this.totalElements = myPage.totalElements;
    }

    public static <E> MyPageBuilder<E> builder() {
        return new MyPageBuilder();
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public long getSize() {
        return this.size;
    }

    public long getOffset() {
        return this.offset;
    }

    public long getNumber() {
        return this.number;
    }

    public long getTotalPages() {
        return this.totalPages;
    }

    public long getTotalElements() {
        return this.totalElements;
    }

    public MyPage(long size, long offset, long number, long totalPages, long totalElements) {
        this.size = size;
        this.offset = offset;
        this.number = number;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    public static class MyPageBuilder<E> {
        private long size;
        private long offset;
        private long number;
        private long totalPages;
        private long totalElements;

        MyPageBuilder() {
        }

        public MyPageBuilder<E> size(long size) {
            this.size = size;
            return this;
        }

        public MyPageBuilder<E> offset(long offset) {
            this.offset = offset;
            return this;
        }

        public MyPageBuilder<E> number(long number) {
            this.number = number;
            return this;
        }

        public MyPageBuilder<E> totalPages(long totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public MyPageBuilder<E> totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }

        public MyPage<E> build() {
            return new MyPage(this.size, this.offset, this.number, this.totalPages, this.totalElements);
        }

        public String toString() {
            return "MyPage.MyPageBuilder(size=" + this.size + ", offset=" + this.offset + ", number=" + this.number + ", totalPages=" + this.totalPages + ", totalElements=" + this.totalElements + ")";
        }
    }
}
