package com.peew.notesr.model;

import android.net.Uri;

import java.util.Objects;

public class TempFile {

    private Long id;
    private Uri uri;

    public TempFile(Long id, Uri uri) {
        this.id = id;
        this.uri = uri;
    }

    public TempFile(Uri uri) {
        this.uri = uri;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Uri getUri() {
        return uri;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        TempFile tempFile = (TempFile) object;
        return Objects.equals(id, tempFile.id) && Objects.equals(uri, tempFile.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, uri);
    }
}
