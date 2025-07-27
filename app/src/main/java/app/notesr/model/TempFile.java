package app.notesr.model;

import android.net.Uri;

import androidx.room.Entity;

import org.jetbrains.annotations.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class TempFile {
    private Long id;

    @NotNull
    private Uri uri;
}
