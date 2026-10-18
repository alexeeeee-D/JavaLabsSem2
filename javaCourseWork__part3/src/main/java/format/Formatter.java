package format;

import model.ApiRecord;
import model.SaveMode;
import java.nio.file.Path;
import java.util.List;

public interface Formatter {
    void create(Path path);
    void save(ApiRecord api, Path path, SaveMode mode);
    void print(Path path);
    void printBySource(Path path, String source);
}
