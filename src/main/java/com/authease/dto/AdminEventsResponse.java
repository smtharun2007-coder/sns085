package com.authease.dto;

import com.authease.model.LoginEvent;

import java.util.List;

public class AdminEventsResponse {

    private List<LoginEvent> items;
    private long total;
    private int page;
    private int pageSize;

    public AdminEventsResponse() {}

    public AdminEventsResponse(List<LoginEvent> items, long total, int page, int pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<LoginEvent> getItems() {
        return items;
    }

    public void setItems(List<LoginEvent> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
