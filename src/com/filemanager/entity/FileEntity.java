package com.filemanager.entity;

public class FileEntity {

    private String fileName ;
    private boolean isHighlighted = false;

    public FileEntity(String fileName, boolean isHighlighted) {
       setFileName(fileName);
        setHighlighted(isHighlighted);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setHighlighted(boolean isHighlighted) {
        this.isHighlighted = isHighlighted;
    }
}
