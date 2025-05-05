import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

public class CompileNatives extends DefaultTask {
    @Input
    private String src_path = "默认消息";

    @OutputDirectory
    private File outputDir;

    @TaskAction
    public void execute() throws IOException {

    }

    // Getter 和 Setter（必须提供，Gradle 通过反射访问）
    public String getSrc_path() {
        return src_path;
    }

    public void setSrc_path(String src_path) {
        this.src_path = src_path;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }
}
