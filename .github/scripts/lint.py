"""Checks a RuneLite plugin against the Plugin Hub's published rules before it goes to review.

    python .github/scripts/lint.py

Sources: AGENTS.md (runelite/plugin-hub-tooling templateplugin), the packager's disallowed-apis.txt,
the plugin-hub README, RuneLite's Rejected or Rolled-Back Features wiki page, and review feedback.
Exits 1 on any error and prints GitHub annotations. Excuse one line with: // lint: allow <rule>
"""
import pathlib
import re
import struct
import subprocess
import sys

ERROR = "error"
WARNING = "warning"
BS = chr(92)
NL = chr(10)

START = "(?<![A-Za-z0-9_])"
END = "(?![A-Za-z0-9_])"


def words(*names):
    return START + "(?:" + "|".join(names) + ")" + END


# rule, pattern, message, severity in src/main, severity in src/test (None to skip tests)
CODE_RULES = [
    ("reflection", words("java[.]lang[.]reflect", "getDeclared(?:Field|Method|Constructor)s?", "setAccessible", "MethodHandles") + "|Class[.]forName[(]",
     "No reflection (AGENTS.md)", ERROR, WARNING),
    ("native", "com[.]sun[.]jna|sun[.]misc[.]Unsafe|org[.]lwjgl|System[.]load(?:Library)?[(]|" + START + "native(?=[ ]+[A-Za-z_<])",
     "No JNI, JNA, Unsafe or LWJGL (AGENTS.md)", ERROR, WARNING),
    ("classloading", words("ClassLoader", "URLClassLoader", "defineClass"),
     "No loading classes at runtime (AGENTS.md)", ERROR, WARNING),
    ("code-generation", words("ToolProvider", "JavaCompiler") + "|net[.]bytebuddy|javassist",
     "No generating code at runtime (AGENTS.md)", ERROR, WARNING),
    ("serialisation", words("ObjectInputStream", "ObjectOutputStream"),
     "No Java serialisation (AGENTS.md)", ERROR, WARNING),
    ("processes", words("ProcessBuilder") + "|Runtime[.]getRuntime[(]|System[.](?:exit|getenv)[(]",
     "No processes, environment variables or System.exit (AGENTS.md)", ERROR, WARNING),
    ("sleep", "Thread[.]sleep[(]|[.]awaitTermination[(]",
     "Don't sleep or block on shutdown; use shutdownNow() (AGENTS.md)", ERROR, None),
    ("countdownlatch", words("CountDownLatch"),
     "Batch async work with CompletableFuture.allOf() (AGENTS.md)", WARNING, None),
    ("http-client", words("HttpURLConnection") + "|java[.]net[.]http|org[.]apache[.]http",
     "Use the injected OkHttpClient (AGENTS.md)", ERROR, None),
    ("client-singletons", "new[ ]+(?:OkHttpClient|Gson|GsonBuilder)[ ]*[(.]",
     "Inject the client's OkHttpClient and Gson, don't create them (packager)", ERROR, None),
    ("widget-ids", words("WidgetInfo", "WidgetID"),
     "Use InterfaceID component IDs from gameval (packager, AGENTS.md)", ERROR, None),
    ("getvar", "client[.]getVar[(]",
     "client.getVar is deprecated; use getVarbitValue or getVarpValue", WARNING, None),
    ("browser", words("Desktop") + "|LinkBrowser[.]open[(]",
     "Open links with LinkBrowser.browse (AGENTS.md)", ERROR, None),
    ("file-io", "java[.]io[.]File" + END + "|new[ ]+File[ ]*[(]|java[.]nio[.]file|" + words("Files", "Paths") + "[.]|"
     + words("FileInputStream", "FileOutputStream", "FileReader", "FileWriter", "RandomAccessFile", "RUNELITE_DIR"),
     "File I/O goes through Filepath (AGENTS.md)", ERROR, None),
    ("guava-io", "com[.]google[.]common[.]io|" + words("CharStreams", "ByteStreams", "CharSource", "ByteSource"),
     "Guava's io package counts as file I/O; read resources with getResourceAsStream and readAllBytes (plugin-hub#19123)", ERROR, WARNING),
    ("file-chooser", words("JFileChooser"),
     "Use Filepath.Chooser (AGENTS.md)", ERROR, None),
    ("get-resource", "[.]getResource[(]",
     "Use getResourceAsStream; the plugin isn't unpacked on disk (plugin-hub README)", ERROR, None),
    ("menu-action", "[.]menuAction[(]",
     "No menu actions sent to the server (AGENTS.md)", ERROR, None),
    ("restricted-client", "[.]runScript[(]|hopToWorld[(]",
     "runScript and hopToWorld draw extra review; explain them in the PR", WARNING, None),
    ("window-focus", words("KeyboardFocusManager", "setAlwaysOnTop") + "|[.]toFront[(]|[.]requestFocus[(]",
     "No changing window or keyboard focus (Rejected or Rolled-Back Features)", ERROR, None),
    ("sound", "javax[.]sound",
     "Play sounds through RuneLite's AudioPlayer", ERROR, None),
    ("console", "System[.](?:out|err)[.]|[.]printStackTrace[(]",
     "Log with the plugin's logger, not the console", ERROR, WARNING),
    ("log-info", "log[.]info[(]",
     "info logs end up in players' logs; prefer log.debug (AGENTS.md)", WARNING, None),
    ("runelite-internals", "net[.]runelite[.]client[.](?:account|discord)[.]",
     "RuneLite's account and Discord classes are off limits (packager)", ERROR, None),
    ("runelite-package", "^[ ]*package[ ]+net[.]runelite",
     "Plugin code can't live in the net.runelite package", ERROR, ERROR),
]

STRING_RULES = [
    ("file-io", "user[.]home|[.]runelite/",
     "No paths into the user's home or .runelite; use the plugin directory (AGENTS.md)", ERROR, None),
]

NETWORK = re.compile("[.]newCall[(]|Request[.]Builder|[.]enqueue[(]")
NETWORK_WARNING = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
IMPORT = re.compile("^import[ ]+(?!static[ ])[A-Za-z0-9_.]*[.]([A-Za-z0-9_]+)[ ]*;")
ALLOW = re.compile("lint:[ ]*allow[ ]+([a-z0-9-]+)")
DEPENDENCY = re.compile("^[ " + chr(9) + "]*(?:implementation|api|runtimeOnly|compileOnly|annotationProcessor|testImplementation)[ (]", re.M)
ALLOWED_DEPENDENCY = re.compile("net[.]runelite|lombok|junit")

found = []


def report(severity, path, line, rule, message):
    found.append(severity)
    where = "file=" + path + ("," + "line=" + str(line) if line else "") + ","
    print("::" + severity + " " + where + "title=lint " + rule + "::" + message)


def blank(src, keep_strings):
    """The source with comments, and unless kept string and char literals, replaced by spaces."""
    out = []
    i = 0
    n = len(src)

    def spaces(text):
        return "".join(c if c == NL else " " for c in text)

    while i < n:
        if src.startswith("/*", i):
            j = src.find("*/", i + 2)
            j = n if j < 0 else j + 2
            out.append(spaces(src[i:j]))
        elif src.startswith("//", i):
            j = src.find(NL, i)
            j = n if j < 0 else j
            out.append(spaces(src[i:j]))
        elif src[i] in "\"'":
            quote = src[i]
            if src.startswith(quote * 3, i):
                j = src.find(quote * 3, i + 3)
                j = n if j < 0 else j + 3
            else:
                j = i + 1
                while j < n and src[j] != quote and src[j] != NL:
                    j += 2 if src[j] == BS else 1
                j = min(j + 1, n)
            out.append(src[i:j] if keep_strings else src[i] + spaces(src[i + 1:j - 1]) + src[j - 1])
        else:
            j = i + 1
            out.append(src[i])
        i = j
    return "".join(out)


def tracked(prefix=""):
    out = subprocess.run(["git", "ls-files", "--", prefix] if prefix else ["git", "ls-files"],
                         capture_output=True, text=True, check=True).stdout
    return [p for p in out.splitlines() if p]


def lint_java(root, shipped):
    for name in tracked(root):
        if not name.endswith(".java"):
            continue
        src = pathlib.Path(name).read_text(encoding="utf-8")
        raw = src.split(NL)
        for keep_strings, rules in ((False, CODE_RULES), (True, STRING_RULES)):
            lines = blank(src, keep_strings).split(NL)
            for rule, pattern, message, main, test in rules:
                severity = main if shipped else test
                if severity is None:
                    continue
                regex = re.compile(pattern)
                hits = [n for n, line in enumerate(lines, 1)
                        if regex.search(line) and not allowed(raw[n - 1], rule)]
                if shipped:
                    for n in hits:
                        report(severity, name, n, rule, message)
                elif hits:
                    report(severity, name, hits[0], rule, message + " (" + str(len(hits)) + " lines; tests aren't shipped but reviewers read them)")
        if shipped:
            code = blank(src, False)
            body = NL.join(line for line in code.split(NL) if not line.lstrip().startswith("import "))
            for n, line in enumerate(code.split(NL), 1):
                m = IMPORT.match(line)
                if m and not re.search(START + m.group(1) + END, body):
                    report(WARNING, name, n, "unused-import", "Unused import " + m.group(1) + " (AGENTS.md)")


def allowed(line, rule):
    m = ALLOW.search(line)
    return m is not None and m.group(1) == rule


def properties():
    props = {}
    for line in pathlib.Path("runelite-plugin.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props


def lint_repo():
    props = properties()
    for key in ("displayName", "author", "description", "tags", "plugins"):
        if not props.get(key):
            report(ERROR, "runelite-plugin.properties", None, "properties", key + " must be set")
    if props.get("build", "standard") != "standard":
        report(ERROR, "runelite-plugin.properties", None, "properties", "build must be standard (plugin-hub README)")

    gradle = pathlib.Path("build.gradle").read_text(encoding="utf-8")
    m = re.search("^version[ ]*=[ ]*['\"]([^'\"]+)['\"]", gradle, re.M)
    if not m or m.group(1) != props.get("version"):
        report(ERROR, "build.gradle", None, "version",
               "build.gradle version " + (m.group(1) if m else "missing") + " must match runelite-plugin.properties " + str(props.get("version")))
    if not re.search("release[.]set[(]11[)]|release[ ]*=[ ]*11", gradle):
        report(ERROR, "build.gradle", None, "java-11", "Compile with options.release.set(11) (AGENTS.md)")
    for dep in DEPENDENCY.finditer(gradle):
        line_end = gradle.find(NL, dep.start())
        line = gradle[dep.start():line_end if line_end >= 0 else len(gradle)]
        if not ALLOWED_DEPENDENCY.search(line):
            report(ERROR, "build.gradle", gradle[:dep.start()].count(NL) + 1, "dependency",
                   "Only RuneLite, Lombok and JUnit; extra dependencies need manual verification (plugin-hub README, AGENTS.md)")

    if not pathlib.Path("LICENSE").exists():
        report(ERROR, "LICENSE", None, "license", "Keep a permissive licence such as BSD-2 (AGENTS.md)")

    for name in tracked():
        if name.endswith("META-INF/services/net.runelite.client.plugins.Plugin"):
            report(ERROR, name, None, "services-file", "Don't ship a Plugin services file (AGENTS.md)")
        if name.endswith(".class") or (name.endswith(".jar") and name != "gradle/wrapper/gradle-wrapper.jar") \
                or name.startswith(("build/", "out/")):
            report(ERROR, name, None, "build-artifact", "Don't commit build artifacts (AGENTS.md)")

    icon = pathlib.Path("icon.png")
    if icon.exists():
        data = icon.read_bytes()
        if data[:8] != bytes([137, 80, 78, 71, 13, 10, 26, 10]):
            report(ERROR, "icon.png", None, "icon", "icon.png must be a real PNG (AGENTS.md)")
        else:
            width, height = struct.unpack(">II", data[16:24])
            if width > 48 or height > 72:
                report(ERROR, "icon.png", None, "icon", "icon.png is " + str(width) + "x" + str(height) + "; at most 48x72 (plugin-hub README)")

    main = NL.join(pathlib.Path(p).read_text(encoding="utf-8") for p in tracked("src/main/java") if p.endswith(".java"))
    code = blank(main, False)
    if re.search("getPluginDirectory|" + words("Filepath"), code) and "internalName" not in code:
        report(ERROR, None, None, "internal-name", "Using Filepath needs internalName in @PluginDescriptor (AGENTS.md)")
    if NETWORK.search(code) and NETWORK_WARNING not in main.replace("\" + \"", ""):
        report(ERROR, None, None, "network",
               "Network features must be opt-in with a warning= containing the hub's wording (AGENTS.md)")


def main():
    lint_java("src/main/java", True)
    if pathlib.Path("src/test/java").exists():
        lint_java("src/test/java", False)
    lint_repo()
    print("lint: " + str(found.count(ERROR)) + " error(s), " + str(found.count(WARNING)) + " warning(s)")
    return 1 if ERROR in found else 0


if __name__ == "__main__":
    sys.exit(main())
