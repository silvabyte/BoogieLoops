# BoogieLoops Kit: General Purpose Utilities

Lightweight utilities for Scala 3 applications.

- **DotEnv**: Unified environment variable access from `.env` files and system environment
- **MagicNumber**: File type detection via magic bytes (not file extensions)

## Install

Mill:

```scala
mvn"dev.boogieloop::kit:0.5.6"
```

SBT:

```scala
libraryDependencies += "dev.boogieloop" %% "kit" % "0.5.6"
```

## DotEnv

Load environment variables from `.env` files with automatic fallback to system environment. Fully immutable and thread-safe.

### Quickstart

```scala
import boogieloops.kit.DotEnv

// Load from .env file in current directory
val env = DotEnv.load()

// Access variables (checks .env first, then sys.env)
val dbHost = env.getOrElse("DATABASE_HOST", "localhost")
val apiKey = env.get("API_KEY")  // Option[String]

// Get all variables (unified view: sys.env + .env, file values win)
val allVars = env.all
```

### .env File Format

```bash
# Comments start with #
DATABASE_HOST=localhost
DATABASE_PORT=5432

# Quoted values (quotes are stripped)
API_KEY="sk-1234567890"
SECRET='single-quoted-value'

# Values with equals signs work
CONNECTION_STRING=host=localhost;port=5432;db=myapp
```

### API Reference

```scala
class DotEnv {
  // Get a value, checking .env first, then sys.env
  def get(key: String): Option[String]
  
  // Get with default fallback
  def getOrElse(key: String, default: String): String
  
  // Create new DotEnv with updated value (immutable)
  def withSet(key: String, value: String): DotEnv
  
  // Get all variables (sys.env merged with .env, file values take precedence)
  def all: Map[String, String]
}

object DotEnv {
  // Load from file (defaults to ".env" in current directory)
  def load(filePath: String = ".env", overrideExisting: Boolean = true): DotEnv
}
```

### Immutability

`DotEnv` is fully immutable. The `withSet` method returns a new instance:

```scala
val env1 = DotEnv.load()
val env2 = env1.withSet("NEW_KEY", "value")

env1.get("NEW_KEY")  // None (original unchanged)
env2.get("NEW_KEY")  // Some("value")
```

### Missing Files

If the `.env` file doesn't exist, `DotEnv.load()` returns an instance that falls back entirely to system environment:

```scala
val env = DotEnv.load("non_existent.env")
env.get("PATH")  // Still works - falls back to sys.env
env.all          // Contains all sys.env variables
```

### Override Behavior

The `overrideExisting` parameter controls how duplicate keys in the file are handled:

```scala
// .env contains:
// KEY=first
// KEY=second

DotEnv.load(overrideExisting = true).get("KEY")   // Some("second") - last wins
DotEnv.load(overrideExisting = false).get("KEY")  // Some("first") - first wins
```

## MagicNumber

Detect file types by reading magic bytes from file headers. This is more reliable than checking file extensions, which can be spoofed or incorrect.

### Quickstart

```scala
import boogieloops.kit.MagicNumber

// Detect from raw bytes (e.g., from upload)
val header = fileBytes.take(MagicNumber.HeaderLength)
MagicNumber.detect(header) match {
  case Some(sig) => println(s"File type: ${sig.name}, MIME: ${sig.mimeType}")
  case None      => println("Unknown file type")
}

// Detect from file path
MagicNumber.detect(os.pwd / "document.pdf")  // Some(FileSignature(...))

// Convenience checks
MagicNumber.isPdf(header)       // true/false
MagicNumber.isZipBased(header)  // true for ZIP, DOCX, XLSX, etc.

// Validate against allowed types
val allowed = Set("application/pdf", "image/png", "image/jpeg")
MagicNumber.isAllowed(header, allowed)  // true if file type is in set
```

### Supported File Types

| Category | Formats |
|----------|---------|
| Documents | PDF, RTF, DOC (legacy), WordPerfect |
| Office (ZIP-based) | DOCX, XLSX, PPTX, ODT, ODS, ODP, EPUB |
| Text | UTF-8/16/32 with BOM, XML, HTML |
| Images | PNG, JPEG, GIF, BMP, WEBP, TIFF |
| Compressed | RAR, 7Z, GZIP, BZIP2, XZ |
| Audio | MP3, WAV, FLAC, OGG |
| Video | MKV, FLV, MP4 |
| Executables | EXE/DLL, ELF, Mach-O, Java Class |

### API Reference

```scala
case class FileSignature(
  name: String,              // Human-readable name (e.g., "PDF")
  headerSeq: Array[Byte],    // Magic bytes
  ext: String,               // Primary extension (e.g., "pdf")
  mimeType: String,          // MIME type (e.g., "application/pdf")
  supportedExtensions: Vector[String]  // All valid extensions
)

object MagicNumber {
  val HeaderLength: Int = 24  // Recommended bytes to read
  val Signatures: Vector[FileSignature]  // All known signatures

  // Detect from raw bytes
  def detect(header: Array[Byte]): Option[FileSignature]

  // Detect from file path
  def detect(path: os.Path): Option[FileSignature]
  def detect(path: java.nio.file.Path): Option[FileSignature]

  // Convenience checks
  def isPdf(header: Array[Byte]): Boolean
  def isZipBased(header: Array[Byte]): Boolean
  def isRtf(header: Array[Byte]): Boolean
  def isDocument(header: Array[Byte]): Boolean
  def hasTextBom(header: Array[Byte]): Boolean
  def isLikelyPlainText(header: Array[Byte]): Boolean
  def isAllowed(header: Array[Byte], allowedMimeTypes: Set[String]): Boolean

  // Resume upload validation
  val ResumeMimeTypes: Set[String]  // Common MIME types for resumes
  def isResumeFormat(header: Array[Byte]): Boolean
}
```

### Use Cases

**File upload validation** - Verify uploaded files match their claimed type:

```scala
def validateUpload(bytes: Array[Byte], claimedMime: String): Boolean = {
  val header = bytes.take(MagicNumber.HeaderLength)
  MagicNumber.detect(header).exists(_.mimeType == claimedMime)
}
```

**Security filtering** - Block executable uploads:

```scala
val safeTypes = Set("application/pdf", "image/png", "image/jpeg")
if (!MagicNumber.isAllowed(header, safeTypes)) {
  throw new SecurityException("File type not allowed")
}
```

**Resume upload validation** - Built-in support for common resume formats:

```scala
def handleResumeUpload(bytes: Array[Byte]): Either[String, Unit] = {
  val header = bytes.take(MagicNumber.HeaderLength)
  
  if (!MagicNumber.isResumeFormat(header)) {
    Left("Please upload a PDF, Word document, RTF, or plain text file")
  } else {
    Right(processResume(bytes))
  }
}

// Or check specific document types
if (MagicNumber.isPdf(header)) {
  // Handle PDF
} else if (MagicNumber.isZipBased(header)) {
  // Handle DOCX/ODT (ZIP-based)
} else if (MagicNumber.isRtf(header)) {
  // Handle RTF
} else if (MagicNumber.isLikelyPlainText(header)) {
  // Handle plain text
}
```

## Testing

```bash
./mill Kit.test
# or
make test MODULE=Kit
```
