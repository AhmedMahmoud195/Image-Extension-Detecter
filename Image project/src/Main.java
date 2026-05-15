import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Scanner;
import java.util.zip.CRC32;
class ImageSignatures {
    // عشان اعرف ال BMP
    public static final byte[] BMP_SIGNATURE = {0x42, 0x4D};
    // PNG عشان أعرف
    public static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };
}

public class Main {

    public static void main(String[] args) {
        System.out.print("Enter your image file path please : ");
        Scanner input = new Scanner(System.in);
        String filePath = input.nextLine();

        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("File not found!");
            input.close();
            return;
        }

        // Detect format
        String format = detectFormat(file);
        System.out.println("\n========================================");
        System.out.println("DETECTION RESULT");
        System.out.println("========================================");
        System.out.println("Detected Format: " + format);

        if (format.equals("Unknown")) {
            System.out.println("Could not identify any image signatures.");
            input.close();
            return;
        }

        // Validate integrity
        boolean isValid = format.equals("BMP") ? validateBMP(file) : validatePNG(file);

        // Print metadata
        printImageMetadata(file, format, isValid);

        input.close();
    }
    public static String detectFormat(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[8];
            int bytesRead = fis.read(header);
            if (bytesRead < 2) {
                return "Unknown";
            }
            // بتأكد من BMP عن طريق مقارنة الفايل بالsignature من الclass اللي انا عامله
            if (header[0] == ImageSignatures.BMP_SIGNATURE[0] &&
                    header[1] == ImageSignatures.BMP_SIGNATURE[1]) {
                return "BMP";
            }
            // بتأكد من PNG عن طريق مقارنة الفايل بالsignature من الclass اللي انا عامله
            if (bytesRead >= 8) {
                boolean isPNG = true;
                for (int i = 0; i < 8; i++) {
                    if (header[i] != ImageSignatures.PNG_SIGNATURE[i]) {
                        isPNG = false;
                        break;
                    }
                }
                if (isPNG) {
                    return "PNG";
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        return "Unknown";
    }
    //function عشان اتأكد هو ال BMP ده شغال فعلا ولا لا
    public static boolean validateBMP(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[54]; // BMP header + BITMAPINFOHEADER
            int bytesRead = fis.read(header);

            if (bytesRead < 14) {
                return false; // في مشكلة في حجم الفايل فهيكون مش صح
            }
            long fileSizeInHeader = ((header[5] & 0xFF) << 24) |
                    ((header[4] & 0xFF) << 16) |
                    ((header[3] & 0xFF) << 8) |
                    (header[2] & 0xFF);
            long actualFileSize = file.length();
            if (fileSizeInHeader != actualFileSize && fileSizeInHeader != 0) {
                return false;
            }
            int pixelDataOffset = ((header[13] & 0xFF) << 24) |
                    ((header[12] & 0xFF) << 16) |
                    ((header[11] & 0xFF) << 8) |
                    (header[10] & 0xFF);
            if (pixelDataOffset < 14 || pixelDataOffset > actualFileSize) {
                return false;
            }
            if (bytesRead >= 18) {
                int dibHeaderSize = ((header[17] & 0xFF) << 24) |
                        ((header[16] & 0xFF) << 16) |
                        ((header[15] & 0xFF) << 8) |
                        (header[14] & 0xFF);
                if (dibHeaderSize != 12 && dibHeaderSize != 40 &&
                        dibHeaderSize != 52 && dibHeaderSize != 56 &&
                        dibHeaderSize != 108 && dibHeaderSize != 124) {
                    return false;
                }
            }

            return true;

        } catch (IOException e) {
            return false;
        }
    }
    //بتأكد من هنا من فايل ال PNG ولا لا
    public static boolean validatePNG(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] signature = new byte[8];
            fis.read(signature);
            for (int i = 0; i < 8; i++) {
                if (signature[i] != ImageSignatures.PNG_SIGNATURE[i]) {
                    return false;
                }
            }
            byte[] chunkHeader = new byte[8];
            fis.read(chunkHeader);
            int ihdrLength = ((chunkHeader[0] & 0xFF) << 24) |
                    ((chunkHeader[1] & 0xFF) << 16) |
                    ((chunkHeader[2] & 0xFF) << 8) |
                    (chunkHeader[3] & 0xFF);

            if (ihdrLength != 13) {
                return false;
            }
            if (chunkHeader[4] != 'I' || chunkHeader[5] != 'H' ||
                    chunkHeader[6] != 'D' || chunkHeader[7] != 'R') {
                return false;
            }
            byte[] ihdrData = new byte[13];
            byte[] crcBytes = new byte[4];
            fis.read(ihdrData);
            fis.read(crcBytes);

            CRC32 crc = new CRC32();
            crc.update(chunkHeader, 4, 4); // Chunk type
            crc.update(ihdrData);
            long calculatedCRC = crc.getValue();
            long fileCRC = ((crcBytes[0] & 0xFFL) << 24) |
                    ((crcBytes[1] & 0xFFL) << 16) |
                    ((crcBytes[2] & 0xFFL) << 8) |
                    (crcBytes[3] & 0xFFL);

            if (calculatedCRC != fileCRC) {
                return false;
            }
            // Verify color type and bit depth are valid
            int bitDepth = ihdrData[8] & 0xFF;
            int colorType = ihdrData[9] & 0xFF;
            // Valid bit depths: 1, 2, 4, 8, 16
            if (bitDepth != 1 && bitDepth != 2 && bitDepth != 4 &&
                    bitDepth != 8 && bitDepth != 16) {
                return false;
            }
            // Valid color types: 0 (grayscale), 2 (RGB), 3 (indexed),
            // 4 (grayscale+alpha), 6 (RGBA)
            if (colorType != 0 && colorType != 2 && colorType != 3 &&
                    colorType != 4 && colorType != 6) {
                return false;
            }
            // Check for IEND chunk at end of file
            long fileLength = file.length();
            fis.close();
            try (FileInputStream fis2 = new FileInputStream(file)) {
                fis2.skip(fileLength - 12); // IEND chunk is 12 bytes
                byte[] iendChunk = new byte[12];
                fis2.read(iendChunk);

                // Check length (should be 0)
                boolean lengthZero = iendChunk[0] == 0 && iendChunk[1] == 0 &&
                        iendChunk[2] == 0 && iendChunk[3] == 0;

                // Check type is "IEND"
                boolean isIEND = iendChunk[4] == 'I' && iendChunk[5] == 'E' &&
                        iendChunk[6] == 'N' && iendChunk[7] == 'D';

                if (!lengthZero || !isIEND) {
                    return false;
                }
            }

            return true;

        } catch (IOException e) {
            return false;
        }
    }

    public static void printImageMetadata(File file, String format, boolean isValid) {
        System.out.println("\n========================================");
        System.out.println("IMAGE METADATA");
        System.out.println("========================================");
        System.out.println("File: " + file.getName());
        System.out.println("Format: " + format);
        System.out.println("File Size: " + formatFileSize(file.length()));
        System.out.println("Integrity: " + (isValid ? "Valid" : "Corrupted"));
        System.out.println("----------------------------------------");

        if (format.equals("BMP")) {
            printBMPMetadata(file);
        } else if (format.equals("PNG")) {
            printPNGMetadata(file);
        }

        System.out.println("========================================");
    }
    private static void printBMPMetadata(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[54];
            fis.read(header);

            ByteBuffer bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);

            // Pixel data offset (bytes 10-13)
            int pixelOffset = bb.getInt(10);
            System.out.println("Pixel Data Offset: " + pixelOffset + " bytes");

            // DIB header size (bytes 14-17)
            int dibHeaderSize = bb.getInt(14);
            System.out.println("DIB Header Size: " + dibHeaderSize + " bytes");

            if (dibHeaderSize >= 40) { // BITMAPINFOHEADER or larger
                // Image dimensions (bytes 18-21 width, 22-25 height)
                int width = bb.getInt(18);
                int height = bb.getInt(22);
                System.out.println("Dimensions: " + width + " x " + Math.abs(height) + " pixels");

                if (height < 0) {
                    System.out.println("Orientation: Top-down");
                } else {
                    System.out.println("Orientation: Bottom-up");
                }

                // Color planes (bytes 26-27)
                short planes = bb.getShort(26);
                System.out.println("Color Planes: " + planes);

                // Bits per pixel (bytes 28-29)
                short bitsPerPixel = bb.getShort(28);
                System.out.println("Bits Per Pixel: " + bitsPerPixel);

                // Compression method (bytes 30-33)
                int compression = bb.getInt(30);
                String compressionType;
                switch (compression) {
                    case 0: compressionType = "None (BI_RGB)"; break;
                    case 1: compressionType = "RLE 8-bit"; break;
                    case 2: compressionType = "RLE 4-bit"; break;
                    case 3: compressionType = "Bitfields"; break;
                    default: compressionType = "Unknown (" + compression + ")";
                }
                System.out.println("Compression: " + compressionType);

                // Image size (bytes 34-37)
                int imageSize = bb.getInt(34);
                if (imageSize != 0) {
                    System.out.println("Image Data Size: " + formatFileSize(imageSize));
                }

                // Resolution (bytes 38-41 horizontal, 42-45 vertical)
                int hResolution = bb.getInt(38);
                int vResolution = bb.getInt(42);
                if (hResolution > 0 && vResolution > 0) {
                    System.out.println("Resolution: " + hResolution + " x " + vResolution + " pixels/meter");
                }

                // Color palette info (bytes 46-49 colors used, 50-53 important colors)
                int colorsUsed = bb.getInt(46);
                int importantColors = bb.getInt(50);
                if (colorsUsed > 0) {
                    System.out.println("Colors in Palette: " + colorsUsed);
                }
                if (importantColors > 0) {
                    System.out.println("Important Colors: " + importantColors);
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading BMP metadata: " + e.getMessage());
        }
    }

    private static void printPNGMetadata(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            // Skip PNG signature
            fis.skip(8);

            // Read IHDR chunk
            byte[] chunkHeader = new byte[8];
            fis.read(chunkHeader);

            byte[] ihdrData = new byte[13];
            fis.read(ihdrData);

            ByteBuffer bb = ByteBuffer.wrap(ihdrData).order(ByteOrder.BIG_ENDIAN);

            // Width (bytes 0-3)
            int width = bb.getInt(0);
            // Height (bytes 4-7)
            int height = bb.getInt(4);
            System.out.println("Dimensions: " + width + " x " + height + " pixels");

            // Bit depth (byte 8)
            int bitDepth = ihdrData[8] & 0xFF;
            System.out.println("Bit Depth: " + bitDepth + " bits");

            // Color type (byte 9)
            int colorType = ihdrData[9] & 0xFF;
            String colorTypeName;
            int channels;
            switch (colorType) {
                case 0:
                    colorTypeName = "Grayscale";
                    channels = 1;
                    break;
                case 2:
                    colorTypeName = "RGB (Truecolor)";
                    channels = 3;
                    break;
                case 3:
                    colorTypeName = "Indexed (Palette)";
                    channels = 1;
                    break;
                case 4:
                    colorTypeName = "Grayscale with Alpha";
                    channels = 2;
                    break;
                case 6:
                    colorTypeName = "RGBA (Truecolor with Alpha)";
                    channels = 4;
                    break;
                default:
                    colorTypeName = "Unknown";
                    channels = 0;
            }
            System.out.println("Color Type: " + colorTypeName);
            System.out.println("Channels: " + channels);

            // Compression method (byte 10)
            int compression = ihdrData[10] & 0xFF;
            System.out.println("Compression: " + (compression == 0 ? "Deflate" : "Unknown"));

            // Filter method (byte 11)
            int filter = ihdrData[11] & 0xFF;
            System.out.println("Filter Method: " + (filter == 0 ? "Adaptive" : "Unknown"));

            // Interlace method (byte 12)
            int interlace = ihdrData[12] & 0xFF;
            String interlaceMethod;
            switch (interlace) {
                case 0: interlaceMethod = "None"; break;
                case 1: interlaceMethod = "Adam7"; break;
                default: interlaceMethod = "Unknown";
            }
            System.out.println("Interlace: " + interlaceMethod);

            // Calculate approximate uncompressed size
            long uncompressedSize = (long) width * height * channels * (bitDepth / 8);
            System.out.println("Approx. Uncompressed Size: " + formatFileSize(uncompressedSize));

            // Read additional chunks for more metadata
            fis.skip(4); // Skip IHDR CRC

            System.out.println("\nAdditional Chunks:");
            while (fis.available() > 0) {
                byte[] lengthBytes = new byte[4];
                if (fis.read(lengthBytes) < 4) break;

                int chunkLength = ((lengthBytes[0] & 0xFF) << 24) |
                        ((lengthBytes[1] & 0xFF) << 16) |
                        ((lengthBytes[2] & 0xFF) << 8) |
                        (lengthBytes[3] & 0xFF);

                byte[] typeBytes = new byte[4];
                if (fis.read(typeBytes) < 4) break;

                String chunkType = new String(typeBytes);

                // Print important chunk types
                if (chunkType.equals("PLTE")) {
                    System.out.println("  - PLTE (Palette): " + (chunkLength / 3) + " colors");
                } else if (chunkType.equals("tRNS")) {
                    System.out.println("  - tRNS (Transparency)");
                } else if (chunkType.equals("gAMA")) {
                    System.out.println("  - gAMA (Gamma)");
                } else if (chunkType.equals("pHYs")) {
                    System.out.println("  - pHYs (Physical dimensions)");
                } else if (chunkType.equals("tEXt") || chunkType.equals("zTXt") ||
                        chunkType.equals("iTXt")) {
                    System.out.println("  - " + chunkType + " (Text metadata)");
                } else if (chunkType.equals("IDAT")) {
                    System.out.println("  - IDAT (Image data): " + formatFileSize(chunkLength));
                } else if (chunkType.equals("IEND")) {
                    System.out.println("  - IEND (End of file)");
                    break;
                }
                // Skip chunk data and CRC
                fis.skip(chunkLength + 4);
            }
        } catch (IOException e) {
            System.out.println("Error reading PNG metadata: " + e.getMessage());
        }
    }
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}