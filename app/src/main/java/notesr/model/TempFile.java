package notesr.model;

import android.net.Uri;

import org.jetbrains.annotations.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class TempFile {
    private Long id;

    @NotNull
    private Uri uri;
}
