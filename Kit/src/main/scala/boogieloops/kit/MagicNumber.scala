package boogieloops.kit

import scala.util.Try

/**
 * File signature detected from magic bytes.
 *
 * @param name
 *   Human-readable name of the file type
 * @param headerSeq
 *   Magic bytes that identify this file type
 * @param ext
 *   Primary file extension (without dot)
 * @param mimeType
 *   MIME type for the file
 * @param supportedExtensions
 *   All supported extensions for this file type
 */
final case class FileSignature(
    name: String,
    headerSeq: Array[Byte],
    ext: String,
    mimeType: String,
    supportedExtensions: Vector[String] = Vector.empty
)

/**
 * Magic number file type detection.
 *
 * Detects actual file type by reading magic bytes from file header, not relying on file extension
 * or MIME type from HTTP headers which can be spoofed.
 *
 * Based on https://github.com/matsilva/wtft
 */
object MagicNumber {

  /** Number of bytes to read from file header for detection */
  val HeaderLength: Int = 24

  // ============================================================================
  // Image file types
  // ============================================================================

  private val PNG = FileSignature(
    name = "PNG",
    headerSeq = Array(0x89.toByte, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a),
    ext = "png",
    mimeType = "image/png",
    supportedExtensions = Vector("png")
  )

  private val JPEG = FileSignature(
    name = "JPEG",
    headerSeq = Array(0xff.toByte, 0xd8.toByte, 0xff.toByte),
    ext = "jpg",
    mimeType = "image/jpeg",
    supportedExtensions = Vector("jpg", "jpeg")
  )

  private val GIF = FileSignature(
    name = "GIF",
    headerSeq = Array(0x47, 0x49, 0x46, 0x38, 0x39, 0x61),
    ext = "gif",
    mimeType = "image/gif",
    supportedExtensions = Vector("gif")
  )

  private val BMP = FileSignature(
    name = "BMP",
    headerSeq = Array(0x42, 0x4d),
    ext = "bmp",
    mimeType = "image/bmp",
    supportedExtensions = Vector("bmp")
  )

  private val WEBP = FileSignature(
    name = "WEBP",
    headerSeq = Array(0x52, 0x49, 0x46, 0x46),
    ext = "webp",
    mimeType = "image/webp",
    supportedExtensions = Vector("webp")
  )

  private val TIFF = FileSignature(
    name = "TIFF",
    headerSeq = Array(0x4d, 0x4d, 0x00, 0x2a),
    ext = "tiff",
    mimeType = "image/tiff",
    supportedExtensions = Vector("tiff", "tif")
  )

  // ============================================================================
  // Document file types
  // ============================================================================

  private val PDF = FileSignature(
    name = "PDF",
    headerSeq = Array(0x25, 0x50, 0x44, 0x46),
    ext = "pdf",
    mimeType = "application/pdf",
    supportedExtensions = Vector("pdf")
  )

  // Note: DOCX, XLSX, PPTX, ODT, ODS, ODP all share ZIP magic bytes
  // They are all ZIP-based formats. To distinguish them, you'd need to
  // inspect the ZIP contents. For basic detection, we return ZIP.
  private val ZIP = FileSignature(
    name = "ZIP",
    headerSeq = Array(0x50, 0x4b, 0x03, 0x04),
    ext = "zip",
    mimeType = "application/zip",
    supportedExtensions = Vector("zip", "docx", "xlsx", "pptx", "odt", "ods", "odp", "jar", "apk")
  )

  private val DOC = FileSignature(
    name = "Microsoft Word (legacy)",
    headerSeq =
      Array(0xd0.toByte, 0xcf.toByte, 0x11, 0xe0.toByte, 0xa1.toByte, 0xb1.toByte, 0x1a, 0xe1.toByte),
    ext = "doc",
    mimeType = "application/msword",
    supportedExtensions = Vector("doc", "xls", "ppt")
  )

  // ============================================================================
  // Compressed file types
  // ============================================================================

  private val RAR = FileSignature(
    name = "RAR",
    headerSeq = Array(0x52, 0x61, 0x72, 0x21, 0x1a, 0x07, 0x00),
    ext = "rar",
    mimeType = "application/x-rar-compressed",
    supportedExtensions = Vector("rar")
  )

  private val SEVEN_ZIP = FileSignature(
    name = "7Z",
    headerSeq = Array(0x37, 0x7a, 0xbc.toByte, 0xaf.toByte, 0x27, 0x1c),
    ext = "7z",
    mimeType = "application/x-7z-compressed",
    supportedExtensions = Vector("7z")
  )

  private val GZIP = FileSignature(
    name = "GZIP",
    headerSeq = Array(0x1f, 0x8b.toByte, 0x08),
    ext = "gz",
    mimeType = "application/gzip",
    supportedExtensions = Vector("gz", "tgz")
  )

  private val BZIP2 = FileSignature(
    name = "BZIP2",
    headerSeq = Array(0x42, 0x5a, 0x68),
    ext = "bz2",
    mimeType = "application/x-bzip2",
    supportedExtensions = Vector("bz2")
  )

  private val XZ = FileSignature(
    name = "XZ",
    headerSeq = Array(0xfd.toByte, 0x37, 0x7a, 0x58, 0x5a, 0x00),
    ext = "xz",
    mimeType = "application/x-xz",
    supportedExtensions = Vector("xz")
  )

  // ============================================================================
  // Audio file types
  // ============================================================================

  private val MP3 = FileSignature(
    name = "MP3",
    headerSeq = Array(0x49, 0x44, 0x33),
    ext = "mp3",
    mimeType = "audio/mpeg",
    supportedExtensions = Vector("mp3")
  )

  private val WAV = FileSignature(
    name = "WAV",
    headerSeq = Array(0x52, 0x49, 0x46, 0x46),
    ext = "wav",
    mimeType = "audio/wav",
    supportedExtensions = Vector("wav")
  )

  private val FLAC = FileSignature(
    name = "FLAC",
    headerSeq = Array(0x66, 0x4c, 0x61, 0x43),
    ext = "flac",
    mimeType = "audio/flac",
    supportedExtensions = Vector("flac")
  )

  private val OGG = FileSignature(
    name = "OGG",
    headerSeq = Array(0x4f, 0x67, 0x67, 0x53),
    ext = "ogg",
    mimeType = "audio/ogg",
    supportedExtensions = Vector("ogg", "oga", "ogv", "ogx")
  )

  // ============================================================================
  // Video file types
  // ============================================================================

  private val MKV = FileSignature(
    name = "MKV",
    headerSeq = Array(0x1a, 0x45, 0xdf.toByte, 0xa3.toByte),
    ext = "mkv",
    mimeType = "video/x-matroska",
    supportedExtensions = Vector("mkv", "webm")
  )

  private val FLV = FileSignature(
    name = "FLV",
    headerSeq = Array(0x46, 0x4c, 0x56, 0x01),
    ext = "flv",
    mimeType = "video/x-flv",
    supportedExtensions = Vector("flv")
  )

  private val MP4 = FileSignature(
    name = "MP4",
    headerSeq = Array(0x00, 0x00, 0x00),
    ext = "mp4",
    mimeType = "video/mp4",
    supportedExtensions = Vector("mp4", "m4v", "m4a", "mov")
  )

  // ============================================================================
  // Executable file types
  // ============================================================================

  private val EXE = FileSignature(
    name = "Windows Executable",
    headerSeq = Array(0x4d, 0x5a),
    ext = "exe",
    mimeType = "application/x-msdownload",
    supportedExtensions = Vector("exe", "dll", "sys")
  )

  private val ELF = FileSignature(
    name = "ELF",
    headerSeq = Array(0x7f, 0x45, 0x4c, 0x46),
    ext = "elf",
    mimeType = "application/x-executable",
    supportedExtensions = Vector("elf", "so", "bin")
  )

  private val MACHO = FileSignature(
    name = "Mach-O",
    headerSeq = Array(0xcf.toByte, 0xfa.toByte, 0xed.toByte, 0xfe.toByte),
    ext = "macho",
    mimeType = "application/x-mach-binary",
    supportedExtensions = Vector("macho", "dylib")
  )

  private val JAVA_CLASS = FileSignature(
    name = "Java Class",
    headerSeq = Array(0xca.toByte, 0xfe.toByte, 0xba.toByte, 0xbe.toByte),
    ext = "class",
    mimeType = "application/java-vm",
    supportedExtensions = Vector("class")
  )

  /**
   * All known file signatures.
   *
   * Order matters - more specific signatures should come before less specific ones. For example,
   * PDF should come before generic signatures.
   */
  val Signatures: Vector[FileSignature] = Vector(
    // Documents (most common for resume import)
    PDF,
    ZIP,
    DOC,
    // Images
    PNG,
    JPEG,
    GIF,
    BMP,
    WEBP,
    TIFF,
    // Audio
    MP3,
    FLAC,
    OGG,
    WAV,
    // Video
    MKV,
    FLV,
    MP4,
    // Compressed
    RAR,
    SEVEN_ZIP,
    GZIP,
    BZIP2,
    XZ,
    // Executables
    ELF,
    MACHO,
    JAVA_CLASS,
    EXE
  )

  /**
   * Detect file type from raw bytes.
   *
   * @param header
   *   First bytes of the file (at least HeaderLength bytes recommended)
   * @return
   *   Some(FileSignature) if detected, None if unknown
   */
  def detect(header: Array[Byte]): Option[FileSignature] = {
    Signatures.find { sig =>
      sig.headerSeq.zipWithIndex.forall { case (b, i) =>
        i < header.length && header(i) == b
      }
    }
  }

  /**
   * Detect file type from file path.
   *
   * @param path
   *   Path to the file
   * @return
   *   Some(FileSignature) if detected, None if unknown or file read fails
   */
  def detect(path: os.Path): Option[FileSignature] = {
    Try {
      val bytes = os.read.bytes(path)
      val header = bytes.take(HeaderLength)
      detect(header)
    }.toOption.flatten
  }

  /**
   * Detect file type from java.nio.file.Path.
   *
   * @param path
   *   Path to the file
   * @return
   *   Some(FileSignature) if detected, None if unknown or file read fails
   */
  def detect(path: java.nio.file.Path): Option[FileSignature] = {
    detect(os.Path(path))
  }

  /**
   * Check if the detected file type matches any of the allowed MIME types.
   *
   * @param header
   *   First bytes of the file
   * @param allowedMimeTypes
   *   Set of allowed MIME types
   * @return
   *   true if file type is allowed, false otherwise
   */
  def isAllowed(header: Array[Byte], allowedMimeTypes: Set[String]): Boolean = {
    detect(header).exists(sig => allowedMimeTypes.contains(sig.mimeType))
  }

  /**
   * Check if the file is a PDF.
   */
  def isPdf(header: Array[Byte]): Boolean = {
    detect(header).exists(_.mimeType == "application/pdf")
  }

  /**
   * Check if the file is a ZIP-based format (includes DOCX, XLSX, etc).
   */
  def isZipBased(header: Array[Byte]): Boolean = {
    detect(header).exists(_.mimeType == "application/zip")
  }
}
