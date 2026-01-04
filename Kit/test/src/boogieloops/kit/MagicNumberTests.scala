package boogieloops.kit

import utest._

object MagicNumberTests extends TestSuite {
  val tests = Tests {

    test("detect PDF from magic bytes") {
      val pdfHeader = Array[Byte](0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x34)
      val result = MagicNumber.detect(pdfHeader)

      assert(result.isDefined)
      assert(result.get.name == "PDF")
      assert(result.get.mimeType == "application/pdf")
      assert(result.get.ext == "pdf")
    }

    test("detect ZIP from magic bytes") {
      val zipHeader = Array[Byte](0x50, 0x4b, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00)
      val result = MagicNumber.detect(zipHeader)

      assert(result.isDefined)
      assert(result.get.name == "ZIP")
      assert(result.get.mimeType == "application/zip")
      assert(result.get.supportedExtensions.contains("docx"))
    }

    test("detect PNG from magic bytes") {
      val pngHeader =
        Array[Byte](0x89.toByte, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
      val result = MagicNumber.detect(pngHeader)

      assert(result.isDefined)
      assert(result.get.name == "PNG")
      assert(result.get.mimeType == "image/png")
    }

    test("detect JPEG from magic bytes") {
      val jpegHeader = Array[Byte](0xff.toByte, 0xd8.toByte, 0xff.toByte, 0xe0.toByte)
      val result = MagicNumber.detect(jpegHeader)

      assert(result.isDefined)
      assert(result.get.name == "JPEG")
      assert(result.get.mimeType == "image/jpeg")
    }

    test("detect GIF from magic bytes") {
      val gifHeader = Array[Byte](0x47, 0x49, 0x46, 0x38, 0x39, 0x61)
      val result = MagicNumber.detect(gifHeader)

      assert(result.isDefined)
      assert(result.get.name == "GIF")
      assert(result.get.mimeType == "image/gif")
    }

    test("detect GZIP from magic bytes") {
      val gzipHeader = Array[Byte](0x1f, 0x8b.toByte, 0x08, 0x00)
      val result = MagicNumber.detect(gzipHeader)

      assert(result.isDefined)
      assert(result.get.name == "GZIP")
      assert(result.get.mimeType == "application/gzip")
    }

    test("detect MP3 from magic bytes") {
      val mp3Header = Array[Byte](0x49, 0x44, 0x33, 0x04, 0x00, 0x00)
      val result = MagicNumber.detect(mp3Header)

      assert(result.isDefined)
      assert(result.get.name == "MP3")
      assert(result.get.mimeType == "audio/mpeg")
    }

    test("detect ELF from magic bytes") {
      val elfHeader = Array[Byte](0x7f, 0x45, 0x4c, 0x46, 0x02, 0x01)
      val result = MagicNumber.detect(elfHeader)

      assert(result.isDefined)
      assert(result.get.name == "ELF")
      assert(result.get.mimeType == "application/x-executable")
    }

    test("return None for unknown magic bytes") {
      // Use bytes that don't match any known signature
      val unknownHeader = Array[Byte](0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
      val result = MagicNumber.detect(unknownHeader)

      assert(result.isEmpty)
    }

    test("return None for empty array") {
      val emptyHeader = Array[Byte]()
      val result = MagicNumber.detect(emptyHeader)

      assert(result.isEmpty)
    }

    test("handle short header gracefully") {
      // PDF magic is 4 bytes, give only 2
      val shortHeader = Array[Byte](0x25, 0x50)
      val result = MagicNumber.detect(shortHeader)

      // Should not match PDF since header is too short
      assert(result.isEmpty)
    }

    test("detect from file path") {
      val tempDir = os.temp.dir()
      val pdfFile = tempDir / "test.pdf"

      // Write PDF magic bytes followed by some content
      os.write(pdfFile, Array[Byte](0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x34))

      val result = MagicNumber.detect(pdfFile)

      assert(result.isDefined)
      assert(result.get.name == "PDF")

      // Cleanup
      os.remove(pdfFile)
      os.remove(tempDir)
    }

    test("detect from java.nio.file.Path") {
      val tempDir = os.temp.dir()
      val pngFile = tempDir / "test.png"

      // Write PNG magic bytes
      os.write(pngFile, Array[Byte](0x89.toByte, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a))

      val javaPath = pngFile.toNIO
      val result = MagicNumber.detect(javaPath)

      assert(result.isDefined)
      assert(result.get.name == "PNG")

      // Cleanup
      os.remove(pngFile)
      os.remove(tempDir)
    }

    test("return None for non-existent file") {
      val nonExistent = os.Path("/tmp/does-not-exist-12345.bin")
      val result = MagicNumber.detect(nonExistent)

      assert(result.isEmpty)
    }

    test("isPdf returns true for PDF") {
      val pdfHeader = Array[Byte](0x25, 0x50, 0x44, 0x46)
      assert(MagicNumber.isPdf(pdfHeader))
    }

    test("isPdf returns false for non-PDF") {
      val pngHeader = Array[Byte](0x89.toByte, 0x50, 0x4e, 0x47)
      assert(!MagicNumber.isPdf(pngHeader))
    }

    test("isZipBased returns true for ZIP") {
      val zipHeader = Array[Byte](0x50, 0x4b, 0x03, 0x04)
      assert(MagicNumber.isZipBased(zipHeader))
    }

    test("isZipBased returns false for non-ZIP") {
      val pdfHeader = Array[Byte](0x25, 0x50, 0x44, 0x46)
      assert(!MagicNumber.isZipBased(pdfHeader))
    }

    test("isAllowed returns true for allowed MIME type") {
      val pdfHeader = Array[Byte](0x25, 0x50, 0x44, 0x46)
      val allowed = Set("application/pdf", "application/zip")

      assert(MagicNumber.isAllowed(pdfHeader, allowed))
    }

    test("isAllowed returns false for disallowed MIME type") {
      val exeHeader = Array[Byte](0x4d, 0x5a, 0x90.toByte, 0x00)
      val allowed = Set("application/pdf", "application/zip")

      assert(!MagicNumber.isAllowed(exeHeader, allowed))
    }

    test("isAllowed returns false for unknown file type") {
      val unknownHeader = Array[Byte](0x00, 0x00, 0x00, 0x00)
      val allowed = Set("application/pdf", "application/zip")

      assert(!MagicNumber.isAllowed(unknownHeader, allowed))
    }

    test("HeaderLength is 24") {
      assert(MagicNumber.HeaderLength == 24)
    }

    test("Signatures is not empty") {
      assert(MagicNumber.Signatures.nonEmpty)
    }

    test("all signatures have required fields") {
      MagicNumber.Signatures.foreach { sig =>
        assert(sig.name.nonEmpty)
        assert(sig.headerSeq.nonEmpty)
        assert(sig.ext.nonEmpty)
        assert(sig.mimeType.nonEmpty)
      }
    }

    // ============================================================================
    // Document format tests (for resume uploads)
    // ============================================================================

    test("detect RTF from magic bytes") {
      val rtfHeader = Array[Byte](0x7b, 0x5c, 0x72, 0x74, 0x66, 0x31) // {\rtf1
      val result = MagicNumber.detect(rtfHeader)

      assert(result.isDefined)
      assert(result.get.name == "Rich Text Format")
      assert(result.get.mimeType == "application/rtf")
      assert(result.get.ext == "rtf")
    }

    test("isRtf returns true for RTF") {
      val rtfHeader = Array[Byte](0x7b, 0x5c, 0x72, 0x74, 0x66, 0x31)
      assert(MagicNumber.isRtf(rtfHeader))
    }

    test("isRtf returns false for non-RTF") {
      val pdfHeader = Array[Byte](0x25, 0x50, 0x44, 0x46)
      assert(!MagicNumber.isRtf(pdfHeader))
    }

    test("detect UTF-8 BOM") {
      val utf8Bom = Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte, 0x48, 0x65, 0x6c, 0x6c, 0x6f)
      val result = MagicNumber.detect(utf8Bom)

      assert(result.isDefined)
      assert(result.get.name == "UTF-8 Text (BOM)")
      assert(result.get.mimeType == "text/plain")
    }

    test("detect UTF-16 LE BOM") {
      val utf16LeBom = Array[Byte](0xff.toByte, 0xfe.toByte, 0x48, 0x00, 0x65, 0x00)
      val result = MagicNumber.detect(utf16LeBom)

      assert(result.isDefined)
      assert(result.get.name == "UTF-16 LE Text (BOM)")
      assert(result.get.mimeType == "text/plain")
    }

    test("detect UTF-16 BE BOM") {
      val utf16BeBom = Array[Byte](0xfe.toByte, 0xff.toByte, 0x00, 0x48, 0x00, 0x65)
      val result = MagicNumber.detect(utf16BeBom)

      assert(result.isDefined)
      assert(result.get.name == "UTF-16 BE Text (BOM)")
      assert(result.get.mimeType == "text/plain")
    }

    test("detect UTF-32 LE BOM") {
      val utf32LeBom = Array[Byte](0xff.toByte, 0xfe.toByte, 0x00, 0x00, 0x48, 0x00, 0x00, 0x00)
      val result = MagicNumber.detect(utf32LeBom)

      assert(result.isDefined)
      assert(result.get.name == "UTF-32 LE Text (BOM)")
      assert(result.get.mimeType == "text/plain")
    }

    test("detect UTF-32 BE BOM") {
      val utf32BeBom = Array[Byte](0x00, 0x00, 0xfe.toByte, 0xff.toByte, 0x00, 0x00, 0x00, 0x48)
      val result = MagicNumber.detect(utf32BeBom)

      assert(result.isDefined)
      assert(result.get.name == "UTF-32 BE Text (BOM)")
      assert(result.get.mimeType == "text/plain")
    }

    test("hasTextBom returns true for UTF-8 BOM") {
      val utf8Bom = Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte, 0x48, 0x65)
      assert(MagicNumber.hasTextBom(utf8Bom))
    }

    test("hasTextBom returns false for PDF") {
      val pdfHeader = Array[Byte](0x25, 0x50, 0x44, 0x46)
      assert(!MagicNumber.hasTextBom(pdfHeader))
    }

    test("detect XML declaration") {
      val xmlHeader = "<?xml version=\"1.0\"?>".getBytes("UTF-8")
      val result = MagicNumber.detect(xmlHeader)

      assert(result.isDefined)
      assert(result.get.name == "XML")
      assert(result.get.mimeType == "application/xml")
    }

    test("detect HTML DOCTYPE") {
      val htmlHeader = "<!DOCTYPE html>".getBytes("UTF-8")
      val result = MagicNumber.detect(htmlHeader)

      assert(result.isDefined)
      assert(result.get.name == "HTML")
      assert(result.get.mimeType == "text/html")
    }

    test("detect HTML tag") {
      val htmlHeader = "<html><head>".getBytes("UTF-8")
      val result = MagicNumber.detect(htmlHeader)

      assert(result.isDefined)
      assert(result.get.name == "HTML")
      assert(result.get.mimeType == "text/html")
    }

    test("detect Microsoft Compound Document (DOC)") {
      val docHeader = Array[Byte](
        0xd0.toByte,
        0xcf.toByte,
        0x11,
        0xe0.toByte,
        0xa1.toByte,
        0xb1.toByte,
        0x1a,
        0xe1.toByte
      )
      val result = MagicNumber.detect(docHeader)

      assert(result.isDefined)
      assert(result.get.name == "Microsoft Compound Document")
      assert(result.get.mimeType == "application/msword")
      assert(result.get.supportedExtensions.contains("doc"))
      assert(result.get.supportedExtensions.contains("xls"))
    }

    // ============================================================================
    // Resume format validation tests
    // ============================================================================

    test("isDocument returns true for PDF") {
      val pdfHeader = Array[Byte](0x25, 0x50, 0x44, 0x46)
      assert(MagicNumber.isDocument(pdfHeader))
    }

    test("isDocument returns true for RTF") {
      val rtfHeader = Array[Byte](0x7b, 0x5c, 0x72, 0x74, 0x66, 0x31)
      assert(MagicNumber.isDocument(rtfHeader))
    }

    test("isDocument returns true for DOC") {
      val docHeader = Array[Byte](
        0xd0.toByte,
        0xcf.toByte,
        0x11,
        0xe0.toByte,
        0xa1.toByte,
        0xb1.toByte,
        0x1a,
        0xe1.toByte
      )
      assert(MagicNumber.isDocument(docHeader))
    }

    test("isDocument returns true for ZIP (DOCX)") {
      val zipHeader = Array[Byte](0x50, 0x4b, 0x03, 0x04)
      assert(MagicNumber.isDocument(zipHeader))
    }

    test("isDocument returns true for text with BOM") {
      val utf8Bom = Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte)
      assert(MagicNumber.isDocument(utf8Bom))
    }

    test("isDocument returns false for JPEG") {
      val jpegHeader = Array[Byte](0xff.toByte, 0xd8.toByte, 0xff.toByte)
      assert(!MagicNumber.isDocument(jpegHeader))
    }

    test("isDocument returns false for EXE") {
      val exeHeader = Array[Byte](0x4d, 0x5a)
      assert(!MagicNumber.isDocument(exeHeader))
    }

    test("isResumeFormat returns true for PDF") {
      val pdfHeader = Array[Byte](0x25, 0x50, 0x44, 0x46)
      assert(MagicNumber.isResumeFormat(pdfHeader))
    }

    test("isResumeFormat returns true for DOCX (ZIP)") {
      val zipHeader = Array[Byte](0x50, 0x4b, 0x03, 0x04)
      assert(MagicNumber.isResumeFormat(zipHeader))
    }

    test("isResumeFormat returns true for RTF") {
      val rtfHeader = Array[Byte](0x7b, 0x5c, 0x72, 0x74, 0x66, 0x31)
      assert(MagicNumber.isResumeFormat(rtfHeader))
    }

    test("isResumeFormat returns true for HTML") {
      val htmlHeader = "<!DOCTYPE html>".getBytes("UTF-8")
      assert(MagicNumber.isResumeFormat(htmlHeader))
    }

    test("isResumeFormat returns false for JPEG") {
      val jpegHeader = Array[Byte](0xff.toByte, 0xd8.toByte, 0xff.toByte)
      assert(!MagicNumber.isResumeFormat(jpegHeader))
    }

    test("isResumeFormat returns false for EXE") {
      val exeHeader = Array[Byte](0x4d, 0x5a, 0x90.toByte)
      assert(!MagicNumber.isResumeFormat(exeHeader))
    }

    test("ResumeMimeTypes contains expected types") {
      assert(MagicNumber.ResumeMimeTypes.contains("application/pdf"))
      assert(MagicNumber.ResumeMimeTypes.contains("application/msword"))
      assert(MagicNumber.ResumeMimeTypes.contains("application/rtf"))
      assert(MagicNumber.ResumeMimeTypes.contains("text/plain"))
      assert(MagicNumber.ResumeMimeTypes.contains("application/zip"))
    }

    // ============================================================================
    // Plain text heuristic tests
    // ============================================================================

    test("isLikelyPlainText returns true for ASCII text") {
      val asciiText = "Hello, this is a plain text resume.".getBytes("UTF-8")
      assert(MagicNumber.isLikelyPlainText(asciiText))
    }

    test("isLikelyPlainText returns true for text with newlines") {
      val textWithNewlines = "Name: John Doe\nEmail: john@example.com\nSkills: Scala, Java".getBytes("UTF-8")
      assert(MagicNumber.isLikelyPlainText(textWithNewlines))
    }

    test("isLikelyPlainText returns true for UTF-8 text with BOM") {
      val utf8Bom = Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ "Hello".getBytes("UTF-8")
      assert(MagicNumber.isLikelyPlainText(utf8Bom))
    }

    test("isLikelyPlainText returns false for binary PDF") {
      val pdfHeader = Array[Byte](0x25, 0x50, 0x44, 0x46, 0x00, 0x01, 0x02)
      assert(!MagicNumber.isLikelyPlainText(pdfHeader))
    }

    test("isLikelyPlainText returns false for empty array") {
      assert(!MagicNumber.isLikelyPlainText(Array.empty))
    }

    test("isLikelyPlainText returns true for HTML content") {
      val htmlContent = "<!DOCTYPE html><html><body>Resume</body></html>".getBytes("UTF-8")
      assert(MagicNumber.isLikelyPlainText(htmlContent))
    }

    test("isLikelyPlainText returns true for XML content") {
      val xmlContent = "<?xml version=\"1.0\"?><resume><name>John</name></resume>".getBytes("UTF-8")
      assert(MagicNumber.isLikelyPlainText(xmlContent))
    }

    test("isResumeFormat returns true for plain text without BOM") {
      val plainText = "John Doe\nSoftware Engineer\nSkills: Scala, Java, Python".getBytes("UTF-8")
      assert(MagicNumber.isResumeFormat(plainText))
    }
  }
}
