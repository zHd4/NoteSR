package com.peew.notesr.model;

import android.net.Uri;

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

}
