package app.notesr.exporter.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import app.notesr.file.model.DataBlock;
import app.notesr.file.service.FileService;
import app.notesr.util.FilesUtils;

class FilesDataExporter extends Exporter {
    private final File outputDir;
    private final FileService fileService;
    private final Runnable checkCancelled;

    FilesDataExporter(File outputDir,
                      FileService fileService,
                      Runnable checkCancelled,
                      Runnable notifyProgress) {

        super(notifyProgress);

        this.outputDir = outputDir;
        this.fileService = fileService;
        this.checkCancelled = checkCancelled;
    }

    @Override
    public void export() throws IOException {
        if (!outputDir.exists()) {
            if (!outputDir.mkdir()) {
                throw new IOException("Failed to create temporary directory to export data blocks");
            }
        }

        FilesUtils filesUtils = new FilesUtils();
        List<DataBlock> dataBlocksWithoutData = fileService.getAllDataBlocksWithoutData();

        for (DataBlock blockWithoutData : dataBlocksWithoutData) {
            DataBlock dataBlock = fileService.getDataBlock(blockWithoutData.getId());
            filesUtils.writeFileBytes(new File(outputDir, dataBlock.getId()), dataBlock.getData());

            increaseProgress();
            checkCancelled.run();
        }
    }

    @Override
    public long getTotal() {
        return fileService.getDataBlocksCount();
    }
}
