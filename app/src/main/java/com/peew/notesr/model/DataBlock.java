package com.peew.notesr.model;

public class DataBlock {
    private Long id;
    private Long fileId;
    private Long order;
    private byte[] data;

    public DataBlock(Long id, Long fileId, Long order, byte[] data) {
        this.id = id;
        this.fileId = fileId;
        this.order = order;
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public Long getFileId() {
        return fileId;
    }

    public Long getOrder() {
        return order;
    }

    public byte[] getData() {
        return data;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public void setOrder(Long order) {
        this.order = order;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
