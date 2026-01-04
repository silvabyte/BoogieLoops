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
  }
}
