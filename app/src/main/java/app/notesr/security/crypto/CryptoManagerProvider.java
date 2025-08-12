package app.notesr.security.crypto;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;

import app.notesr.App;
import app.notesr.util.FilesUtils;
import app.notesr.util.Wiper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CryptoManagerProvider {
    private static final String PREF_NAME = "crypto_prefs";
    private static volatile CryptoManager instance;

    public static CryptoManager getInstance(){
        return getInstance(App.getContext());
    }

    public static CryptoManager getInstance(Context context) {
        if (instance == null) {
            synchronized (CryptoManagerProvider.class) {
                if (instance == null) {
                    SharedPreferences prefs = context.getSharedPreferences(PREF_NAME,
                            Context.MODE_PRIVATE);

                    instance = new CryptoManager(
                            prefs,
                            new FilesUtils(),
                            new Wiper(),
                            new SecureRandom(),
                            new AesCryptorFactory()
                    );
                }
            }
        }
        return instance;
    }
}
