import com.sun.source.util.JavacTask;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.xml.parsers.DocumentBuilderFactory;

// Parses project sources without resolving dependencies; this is not a full build.
public class SourceSyntaxCheck {
    public static void main(String[] args) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = Files.walk(Path.of("src"));
             var manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            var sources = files.filter(path -> path.toString().endsWith(".java")).toList();
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("--release", "21", "-proc:none"), null, manager.getJavaFileObjectsFromPaths(sources));
            task.parse();
            var errors = diagnostics.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR).toList();
            if (!errors.isEmpty()) throw new AssertionError(errors.toString());
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.newDocumentBuilder().parse(Path.of("pom.xml").toFile());
            System.out.println("PASS: Java 21 syntax for " + sources.size() + " sources; pom.xml is well-formed");
        }
    }
}
