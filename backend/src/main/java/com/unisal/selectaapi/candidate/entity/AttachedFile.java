package com.unisal.selectaapi.candidate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AttachedFile {

    @Column(name = "file_key", length = 512)
    private String key;

    @Column(name = "file_name", length = 255)
    private String name;

    @Column(name = "file_content_type", length = 100)
    private String contentType;

    public AttachedFile() {
    }

    public AttachedFile(String key, String name, String contentType) {
        this.key = key;
        this.name = name;
        this.contentType = contentType;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }
}
