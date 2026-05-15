# Image-Extension-Detecter
This is a solid piece of code for low-level file handling and binary data parsing in Java. It demonstrates a deep understanding of how image files are structured at the byte level rather than just using a high-level library.Below is a professional README.md for your project, formatted to look great on GitHub or any markdown viewer.Image File Integrity & Metadata AnalyzerA lightweight Java utility designed to perform low-level binary analysis on image files. This tool identifies the file format (BMP or PNG), validates the file's internal integrity, and extracts detailed metadata by parsing the binary headers.
🚀 FeaturesSignature Detection: Identifies files based on magic bytes (Signatures) rather than file extensions.Integrity Validation:BMP: Verifies file size matches header data and validates DIB header sizes.PNG: Validates the IHDR chunk, performs CRC32 checksum verification, and ensures the presence of the IEND footer.Detailed Metadata Extraction:BMP: Dimensions, orientation (top-down vs bottom-up), bit depth, compression type (RLE, Bitfields, etc.), and resolution.PNG: Color types (Grayscale, RGB, RGBA, etc.), bit depth, filter methods, interlace methods (Adam7), and a scan of additional chunks like PLTE, gAMA, and tEXt.Size Formatting: Automatically converts bytes into human-readable formats (KB, MB, GB).
🛠️ How It WorksThe application operates directly on the byte stream of the file using FileInputStream and ByteBuffer.1. DetectionThe tool reads the first few bytes of the file to match known signatures:BMP: 0x42 0x4D (ASCII for "BM")PNG: 0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A2. ValidationFor PNGs, the program calculates a CRC32 checksum of the IHDR chunk and compares it to the value stored in the file. If they don't match, the file is flagged as corrupted.3. ParsingUsing Little Endian (for BMP) and Big Endian (for PNG) byte orders, the tool maps binary data to image properties like width, height, and color channels.💻 UsagePrerequisitesJava Development Kit (JDK) 8 or higher.Running the AppCompile the code:Bashjavac Main.java
Run the application:Bashjava Main
Enter the full path to an image file when prompted (e.g., C:/Users/Images/photo.png).
📂 Project StructureClassDescriptionImageSignaturesContains the constant byte arrays for BMP and PNG magic numbers.MainHandles user input, file stream logic, and binary parsing.detectFormatDetermines if the file is BMP, PNG, or Unknown.validateXXXBoolean checks for structural integrity and checksums.printXXXMetadataFormats and displays technical specifications of the image.
📝 Example OutputPlaintextEnter your image file path please : sample.png

========================================
DETECTION RESULT
========================================
Detected Format: PNG

========================================
IMAGE METADATA
========================================
File: sample.png
Format: PNG
File Size: 1.24 MB
Integrity: Valid
----------------------------------------
Dimensions: 1920 x 1080 pixels
Bit Depth: 8 bits
Color Type: RGBA (Truecolor with Alpha)
Channels: 4
Compression: Deflate
...
========================================
⚠️ LimitationsCurrently supports BMP and PNG only.Does not perform full pixel data decoding (it is an analyzer, not a viewer).Does not support encrypted or password-protected files.
