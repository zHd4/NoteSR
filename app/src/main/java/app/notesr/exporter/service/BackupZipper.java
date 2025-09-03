package app.notesr.exporter.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BackupZipper implements AutoCloseable {
    private static final String VERSION_FILE_NAME = "version";
    private static final String NOTES_DIR = "note";
    private static final String FILES_INFOS_DIR = "finfo";
    private static final String DATA_BLOCKS_DIR = "dblock";

    private final ZipOutputStream zipOutputStream;

    public BackupZipper(File outputZipFile) throws IOException {
        this.zipOutputStream = new ZipOutputStream(new FileOutputStream(outputZipFile));
        createStructure();
    }

    private void createStructure() throws IOException {
        createDir(NOTES_DIR);
        createDir(FILES_INFOS_DIR);
        createDir(DATA_BLOCKS_DIR);
    }

    public void addVersionFile(String version) throws IOException {
        addFile(null, VERSION_FILE_NAME, version.getBytes());
    }

    public void addNote(String id, byte[] noteBytes) throws IOException {
        addFile(NOTES_DIR, id, noteBytes);
    }

    public void addFileInfo(String id, byte[] fileInfoBytes) throws IOException {
        addFile(FILES_INFOS_DIR, id, fileInfoBytes);
    }

    public void addDataBlock(String id, byte[] dataBlockBytes) throws IOException {
        addFile(DATA_BLOCKS_DIR, id, dataBlockBytes);
    }

    private void createDir(String dirName) throws IOException {
        ZipEntry dirEntry = new ZipEntry(dirName + "/");

        zipOutputStream.putNextEntry(dirEntry);
        zipOutputStream.closeEntry();
    }

    private void addFile(String dirName, String fileName, byte[] fileBytes) throws IOException {
        ZipEntry entry = dirName != null
                ? new ZipEntry(Paths.get(dirName, fileName).toString())
                : new ZipEntry(fileName);

        zipOutputStream.putNextEntry(entry);
        zipOutputStream.write(fileBytes, 0, fileBytes.length);
        zipOutputStream.closeEntry();
    }

    @Override
    public void close() throws IOException {
        zipOutputStream.close();
    }
}
