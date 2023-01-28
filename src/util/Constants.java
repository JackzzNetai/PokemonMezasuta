package util;

import java.nio.file.Path;

public class Constants {
    private static final String DATA_DIRECTORY_NAME = "data";
    private static final String IMAGE_DIRECTORY_NAME = "/img";
    private static final String DAN_FILE_NAME = "/Dan.csv";
    private static final String TAG_FILE_NAME = "/Tag.csv";

    public static final Path IMAGE_DIRECTORY_PATH = Path.of(DATA_DIRECTORY_NAME + IMAGE_DIRECTORY_NAME);
    public static final Path DAN_FILE_PATH = Path.of(DATA_DIRECTORY_NAME + DAN_FILE_NAME);
    public static final Path TAG_FILE_PATH = Path.of(DATA_DIRECTORY_NAME + TAG_FILE_NAME);
    public static final String DAN_FILE_HEADER = "name,url\n";
    public static final String TAG_FILE_HEADER = "serialNumber,name,isNew,dan,starTitle,starNumber\n";
}
